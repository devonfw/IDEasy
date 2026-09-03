#!/usr/bin/env node
// @ts-check
//
// pr-assignee-reconcile.mjs
//
// Enforces the "single-owner board" invariant on a GitHub Projects v2 board:
//   - In the "Team Review" column a PR has exactly one assignee: the team
//     reviewer, taken from the board's "Team Reviewer" text field. That field
//     is the source of truth and is AUTO-FILLED by this script (never typed by
//     hand, so no typos): when it is empty and exactly one non-author assignee
//     exists (the team's self-assign habit), the bot assigns them and records
//     their login in the field. Because the field persists across column
//     moves, moving a card back to Team Review re-assigns the same reviewer
//     even if they were unassigned in between.
//   - In each configured "author-only" column a PR has exactly one assignee:
//     the author (the Team Reviewer field is kept as a record, not cleared).
//   - Every other column is left untouched.
//   - A PR carrying the explicit TAKEOVER label (default "takeover") is left
//     completely alone: this is how the team marks a PR handed over after its
//     author left the team, so the bot does not re-assign the departed author.
//
// It changes PR assignees (Issues/PRs REST API) and writes the Team Reviewer
// field (GraphQL updateProjectV2ItemFieldValue). It never moves cards between
// columns; humans keep doing that on the board. If the Team Reviewer field
// does not exist yet, it degrades to inference-only behaviour and says so.
//
// Configuration (environment variables):
//   GH_TOKEN / GITHUB_TOKEN  OAuth or GITHUB_TOKEN with read:project +
//                            write:repo (needs "Administration" > "Assign
//                            issues" or the repo's issues:write + pull-requests:write).
//                            NOTE: a GITHUB_TOKEN issued by one repo manages
//                            assignees on that repo's PRs. PRs from OTHER repos
//                            that share the board are best-effort: a 403/404
//                            there is logged and skipped (not fatal), so the
//                            invariant only holds for in-repo PRs. For
//                            cross-repo writes, use an org-scoped PAT as GH_TOKEN.
//   GH_ACCOUNT               GitHub account (user OR organization) that owns
//                            the board (default: devonfw). Personal / fork
//                            boards live under a user account, so both are
//                            searched (user first, then org).
//   BOARD_TITLE              board title to manage                    (default: "IDEasy board")
//   TEAM_REVIEW_COLUMN       Status option that means "under team review"
//                            (default: "Team Review")
//   AUTHOR_ONLY_COLUMNS      comma-separated Status options forced to
//                            "author only" (default: "🏗 In progress,👀 In review")
//   TAKEOVER_LABEL           PR label that exempts a PR from reconciliation
//                            (default: "takeover"). (A board field cannot be
//                            used here: Projects v2 exposes no checkbox value
//                            type via the GraphQL API.)
//   DRY_RUN                  "true" -> report only, change nothing
//                            (default: "true"; set "false" to apply)
//
// Exit codes: 0 ok (incl. dry run), 1 config/runtime error, 2 API error.

import { COLUMNS, computeTarget, computeDiff } from './reconcile-core.mjs';

const API = 'https://api.github.com';

// Boards are looked up separately for the user and the org account of the
// same login, because a combined `user + organization` query in one request
// makes GitHub return a NOT_FOUND error for whichever account type does not
// exist — and the graphql() helper treats any error as fatal.
const USER_BOARDS_QUERY = `
query ($login: String!, $after: String) {
  user(login: $login) {
    projectsV2(first: 100, after: $after) {
      nodes { id title }
      pageInfo { hasNextPage endCursor }
    }
  }
}
`;

const ORG_BOARDS_QUERY = `
query ($login: String!, $after: String) {
  organization(login: $login) {
    projectsV2(first: 100, after: $after) {
      nodes { id title }
      pageInfo { hasNextPage endCursor }
    }
  }
}
`;

const ITEMS_QUERY = `
query ($id: ID!, $after: String) {
  node(id: $id) {
    ... on ProjectV2 {
      items(first: 100, after: $after) {
        nodes {
          ... on ProjectV2Item {
            id
            content {
              __typename
              ... on PullRequest {
                number
                state
                mergedAt
                isDraft
                author { login }
                repository { nameWithOwner }
                assignees(first: 30) { nodes { login } }
                labels(first: 30) { nodes { name } }
              }
            }
            fieldValues(first: 100) {
              nodes {
                __typename
                ... on ProjectV2ItemFieldSingleSelectValue {
                  name
                  field {
                    ... on ProjectV2SingleSelectField { name }
                  }
                }
                ... on ProjectV2ItemFieldTextValue {
                  text
                  field {
                    ... on ProjectV2Field { name }
                  }
                }
              }
            }
          }
        }
        pageInfo { hasNextPage endCursor }
      }
    }
  }
}
`;

// ---------------------------------------------------------------------------
// HTTP helpers
// ---------------------------------------------------------------------------

function headers() {
  const token = process.env.GH_TOKEN || process.env.GITHUB_TOKEN;
  if (!token) {
    fail('No GH_TOKEN or GITHUB_TOKEN set.');
  }
  return {
    Authorization: `Bearer ${token}`,
    Accept: 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2022-11-28',
    'Content-Type': 'application/json',
  };
}

function fail(message) {
  console.error(`[reconcile] FATAL: ${message}`);
  process.exit(1);
}

async function graphql(query, variables) {
  const res = await fetch(`${API}/graphql`, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify({ query, variables }),
  });
  if (!res.ok) {
    fail(`GraphQL HTTP ${res.status}: ${await res.text()}`);
  }
  const json = await res.json();
  // Reads use GraphQL and are treated as fatal: if the board/field read fails
  // we cannot trust the board state, so the whole run aborts. Assignee REST
  // writes are per-card and non-fatal (see rest()). A bad single card therefore
  // never aborts the run; only a bad board-level read does.
  if (json.errors?.length) {
    fail(`GraphQL error: ${JSON.stringify(json.errors)}`);
  }
  return json.data;
}

/**
 * Like {@link graphql}, but treats a GraphQL NOT_FOUND as "the thing was not
 * found here" (returns `null`) instead of aborting the run. Used by the board
 * lookup: the account may be a USER (fork/personal board) or an ORGANIZATION,
 * so the query for whichever account type does not exist is expected to come
 * back NOT_FOUND and must not be fatal. Any other error is still fatal.
 */
async function graphqlTolerantNotFound(query, variables) {
  const res = await fetch(`${API}/graphql`, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify({ query, variables }),
  });
  if (!res.ok) {
    fail(`GraphQL HTTP ${res.status}: ${await res.text()}`);
  }
  const json = await res.json();
  if (json.errors?.length) {
    if (json.errors.every((e) => e.type === 'NOT_FOUND')) {
      return null;
    }
    fail(`GraphQL error: ${JSON.stringify(json.errors)}`);
  }
  return json.data;
}

async function rest(method, path, body) {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: headers(),
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    // 422/404 on assignee ops is non-fatal for a single card; surface it and continue.
    const text = await res.text();
    console.error(`[reconcile] REST ${method} ${path} -> ${res.status}: ${text}`);
    return { ok: false, status: res.status };
  }
  return { ok: true, status: res.status };
}

// ---------------------------------------------------------------------------
// Board access
// ---------------------------------------------------------------------------

const FIELDS_QUERY = `
query ($id: ID!) {
  node(id: $id) {
    ... on ProjectV2 {
      fields(first: 100) {
        nodes {
          ... on ProjectV2Field { id name dataType }
        }
      }
    }
  }
}
`;

/**
 * Find the board field named `fieldName` (the "Team Reviewer" text field).
 * Returns its node id, or `null` if the field does not exist yet.
 */
async function findField(boardId, fieldName) {
  const data = await graphql(FIELDS_QUERY, { id: boardId });
  const node = data.node?.fields?.nodes.find((f) => f.name === fieldName);
  return node ? { id: node.id, name: node.name } : null;
}

async function findBoard(login, title) {
  // The board owner is a GitHub account of unknown type: try the user
  // account first (personal / fork boards live there), then the organization
  // (the production case, e.g. devonfw). Each lookup runs its own query, and
  // a NOT_FOUND — "this account type does not exist" (the fork is a user, so
  // no org by that login) — is tolerated and the next account type is tried.
  const lookups = [
    { query: USER_BOARDS_QUERY, field: 'user' },
    { query: ORG_BOARDS_QUERY, field: 'organization' },
  ];
  for (const { query, field } of lookups) {
    let after = null;
    for (let page = 0; page < 50; page++) {
      const data = await graphqlTolerantNotFound(query, { login, after });
      const proj = data?.[field]?.projectsV2;
      if (!proj) {
        break; // account does not exist or has no boards; try the next one
      }
      for (const node of proj.nodes) {
        if (node.title === title) {
          return { id: node.id, title: node.title };
        }
      }
      if (!proj.pageInfo.hasNextPage) {
        break;
      }
      after = proj.pageInfo.endCursor;
    }
  }
  fail(`Could not find a project titled "${title}" in account "${login}".`);
}

/**
 * Read every Pull Request item on the board (paginated) and reduce each to a
 * plain object: { itemId, number, author, repo, assignees, statusName,
 * reviewer, takeover } where `reviewer` is the current "Team Reviewer" text
 * field value (a GitHub login, or '' when the field is unset / does not exist)
 * and `takeover` is true when the PR has the configured takeover label.
 * (A board field cannot be used for this: Projects v2 exposes no value type
 * for checkbox fields via the GraphQL API.)
 */
async function readPrItems(boardId, reviewerFieldName, takeoverLabel) {
  const items = [];
  let after = null;
  for (let page = 0; page < 100; page++) {
    const data = await graphql(ITEMS_QUERY, { id: boardId, after });
    const conn = data.node.items;
    for (const node of conn.nodes) {
      const content = node.content;
      if (!content || content.__typename !== 'PullRequest') {
        continue; // skip issues / drafts
      }
      // With "... on PullRequest { number ... }" the fields merge directly onto
      // `content` (there is no content.PullRequest wrapper).
      const pr = content;
      // The Status value is the single-select whose field is named "Status";
      // the Team Reviewer value is the text field named `reviewerFieldName`.
      // In both cases the field name merges directly onto `v.field` (no wrapper).
      const statusValue = node.fieldValues.nodes.find(
        (v) =>
          v.__typename === 'ProjectV2ItemFieldSingleSelectValue' &&
          v.field?.name === 'Status',
      );
      const reviewerValue = node.fieldValues.nodes.find(
        (v) => v.__typename === 'ProjectV2ItemFieldTextValue' && v.field?.name === reviewerFieldName,
      );
      // Takeover exemption: the configured PR label marks the card as handed
      // over (e.g. author left the team) so the bot never reverts the taker's
      // assignment.
      const takeover = pr.labels.nodes.some((label) => label.name === takeoverLabel);
      // Only manage open, not-yet-merged PRs authored by a human. The board
      // holds hundreds of stale/merged PRs (incl. dependabot); we must not
      // rewrite their assignees.
      // Skip automated authors. GitHub reports the dependabot login as
      // "dependabot" via GraphQL (the board query) but as "dependabot[bot]"
      // via the REST API, so match both; any other "[bot]" login is a bot too.
      const isBot = (login) => login === 'dependabot' || login.endsWith('[bot]');
      const manageable = pr.state === 'OPEN' && pr.mergedAt === null && !isBot(pr.author.login);
      if (!manageable) {
        continue;
      }
      items.push({
        itemId: node.id,
        number: pr.number,
        author: pr.author.login,
        repo: pr.repository.nameWithOwner,
        assignees: pr.assignees.nodes.map((a) => a.login),
        statusName: statusValue?.name ?? '',
        reviewer: (reviewerValue?.text || '').trim(),
        takeover,
      });
    }
    if (!conn.pageInfo.hasNextPage) {
      break;
    }
    after = conn.pageInfo.endCursor;
  }
  return items;
}

// ---------------------------------------------------------------------------
// Reconciliation
// ---------------------------------------------------------------------------

// Add the target first and only then remove the others, so a failed add
// (e.g. a hand-typed typo in the Team Reviewer field) can never leave the
// card with zero assignees.
async function applyAssignees(repo, number, toAdd, toRemove) {
  if (toAdd.length) {
    const r = await rest('POST', `/repos/${repo}/issues/${number}/assignees`, { assignees: toAdd });
    if (!r.ok) {
      console.warn(`[reconcile]   ! could not assign ${toAdd.join(', ')} on ${repo}#${number}; skipping unassign`);
      return;
    }
  }
  // Remove via a JSON body { assignees: [...] } — the API retired the old
  // "?assignee=<login>" query form (it now 400s "Body should be a JSON
  // object"). Mirrors the add call above, which already uses the body form.
  for (const login of toRemove) {
    const r = await rest('DELETE', `/repos/${repo}/issues/${number}/assignees`, { assignees: [login] });
    if (!r.ok) {
      console.warn(`[reconcile]   ! could not unassign ${login} on ${repo}#${number}`);
    }
  }
}

/**
 * Auto-fill the "Team Reviewer" text field with a login (the record of who
 * did the team review). Only called when the field was empty.
 */
async function setReviewerField(boardId, itemId, fieldId, login) {
  // NOTE: updateProjectV2ItemFieldValue takes a single `input` object
  // (UpdateProjectV2ItemFieldValueInput), not top-level arguments — the
  // old projectId/itemId/fieldId/value form is retired by the API.
  const mutation = `
mutation ($input: UpdateProjectV2ItemFieldValueInput!) {
  updateProjectV2ItemFieldValue(input: $input) {
    projectV2Item { id }
  }
}
`;
  const res = await fetch(`${API}/graphql`, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify({
      query: mutation,
      variables: { input: { projectId: boardId, itemId, fieldId, value: { text: login } } },
    }),
  });
  if (!res.ok) {
    console.warn(`[reconcile]   ! field write HTTP ${res.status}: ${await res.text()}`);
    return;
  }
  const json = await res.json();
  if (json.errors?.length) {
    console.warn(`[reconcile]   ! field write error: ${JSON.stringify(json.errors)}`);
    return;
  }
  const item = json.data.updateProjectV2ItemFieldValue.projectV2Item;
  console.log(`[reconcile]   wrote Team Reviewer field = ${login} (item ${item?.id})`);
}

async function main() {
  const login = process.env.GH_ACCOUNT || process.env.GH_ORG || 'devonfw';
  const title = process.env.BOARD_TITLE || 'IDEasy board';
  const teamReviewColumn = process.env.TEAM_REVIEW_COLUMN || 'Team Review';
  const reviewerFieldName = process.env.REVIEWER_FIELD || 'Team Reviewer';
  // Takeover exemption: PRs with the configured label are left alone (see
  // readPrItems / computeTarget).
  const takeoverLabel = process.env.TAKEOVER_LABEL || 'takeover';
  const authorOnlyColumns = (process.env.AUTHOR_ONLY_COLUMNS || '🏗 In progress,👀 In review')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
  const dryRun = (process.env.DRY_RUN ?? 'true').toLowerCase() !== 'false';

  const cfg = { teamReviewColumn, authorOnlyColumns };

  console.log(`[reconcile] account=${login} board="${title}" dryRun=${dryRun}`);
  console.log(`[reconcile] teamReview="${teamReviewColumn}" authorOnly=[${authorOnlyColumns.join(', ')}] reviewerField="${reviewerFieldName}" takeoverLabel="${takeoverLabel}"`);

  const board = await findBoard(login, title);
  console.log(`[reconcile] board id=${board.id}`);

  // The "Team Reviewer" field is optional: if it does not exist yet, the
  // reconciler degrades to inference-only behaviour (no field reads/writes).
  const reviewerField = await findField(board.id, reviewerFieldName);
  if (!reviewerField) {
    console.warn(
      `[reconcile] WARNING: board has no "${reviewerFieldName}" field; running inference-only (no reviewer field will be recorded). Create a TEXT field named exactly "${reviewerFieldName}" to enable the record.`,
    );
  }

  const prItems = await readPrItems(board.id, reviewerFieldName, takeoverLabel);
  const prCards = prItems.filter((it) => it.statusName); // skip items without a readable Status
  console.log(`[reconcile] found ${prCards.length} open, non-merged PR items to manage`);

  let changes = 0;
  let skipped = 0;
  let takeovers = 0;
  let fieldWrites = 0;

  for (const pr of prCards) {
    const target = computeTarget(pr, cfg);
    if (target.kind === COLUMNS.UNMANAGED) {
      skipped++;
      continue;
    }
    if (target.kind === COLUMNS.TAKEOVER) {
      // Explicitly taken-over card: leave the assignees as they are.
      takeovers++;
      console.log(
        `[reconcile] #${pr.number} [${pr.statusName}] (takeover: assignees left as-is: ${pr.assignees.join(', ') || '(none)'})`,
      );
      continue;
    }
    const diff = computeDiff(pr.assignees, target.target);
    const willFillField = target.setField !== null && reviewerField !== null;
    if (!diff.changed && !willFillField) {
      continue;
    }

    changes++;
    const who = target.target ? ` -> ${target.target}` : ' -> (none)';
    const verb = dryRun ? 'WOULD SET' : 'SET';
    console.log(
      `[reconcile] #${pr.number} [${pr.statusName}] ${pr.assignees.join(', ') || '(none)'}${who} ` +
        `(author=${pr.author}, ${diff.toRemove.length} removed, ${diff.toAdd.length} added)` +
        (willFillField ? ` [field: "${reviewerFieldName}" = ${target.setField}]` : ''),
    );
    if (target.anomaly) {
      console.warn(`[reconcile]   ! ${pr.number}: ${target.anomaly}`);
    }
    if (!dryRun) {
      // Apply the assignee change first and only then record the reviewer in
      // the field, so the field never points at a reviewer who was not actually
      // assigned. If the field write then fails, the card is correct and the
      // field stays empty -> the next run re-infers and re-fills it (self-heals).
      if (diff.changed) {
        await applyAssignees(pr.repo, pr.number, diff.toAdd, diff.toRemove);
      }
      if (willFillField) {
        fieldWrites++;
        await setReviewerField(board.id, pr.itemId, reviewerField.id, target.setField);
      }
    }
  }

  console.log(
    `[reconcile] done. cards=${prCards.length} changed=${changes} skipped=${skipped} takeovers=${takeovers}` +
      ` fieldWrites=${dryRun ? `(dry-run) ${fieldWrites}` : fieldWrites} mode=${dryRun ? 'dry-run' : 'apply'}`,
  );
}

main().catch((err) => {
  console.error(`[reconcile] unhandled: ${err.stack || err}`);
  process.exit(2);
});
