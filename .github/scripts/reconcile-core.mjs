// @ts-check
//
// Pure decision logic for the "single-owner board" reconciler.
//
// Invariant being enforced:
//   - In the "Team Review" column the PR has exactly ONE assignee: the team
//     reviewer, taken from the board's "Team Reviewer" field. That field is the
//     source of truth and is auto-filled by the bot (never typed by hand): when
//     it is empty and exactly one non-author assignee exists (the team's
//     self-assign habit), the bot assigns them and records them in the field.
//     Because the field persists, moving a card back to Team Review re-assigns
//     the same reviewer even if they were unassigned in between.
//   - In every configured "author-only" column the PR has exactly ONE assignee:
//     the author (the Team Reviewer field is kept as a record, not cleared).
//   - In the configured "final review" column the PR has exactly TWO assignees:
//     the fixed final reviewer (cfg.finalReviewer) — the PO/PL who does the
//     final reviews — and the author (one, if both are the same person).
//     Unlike Team Review, the reviewer is a constant, not read from a board
//     field (the field is left untouched here, as a record).
//   - Columns that are neither (e.g. New / Research / Refinement / Done) are
//     left untouched so the bot never fights the team's other conventions.
//
// This file has no I/O so the rules can be unit tested in isolation.

export const COLUMNS = Object.freeze({
  TEAM_REVIEW: 'team-review',
  AUTHOR_ONLY: 'author-only',
  FINAL_REVIEW: 'final-review',
  UNMANAGED: 'unmanaged',
  TAKEOVER: 'takeover',
});

/**
 * Classify a Status option name against the configuration.
 * @param {string} statusName The Status value of a card (e.g. "Team Review").
 * @param {{ teamReviewColumn: string, authorOnlyColumns: string[],
 *          finalReviewColumn: string, finalReviewer: string }} cfg
 * @returns {typeof COLUMNS[keyof typeof COLUMNS]}
 */
export function classifyStatus(statusName, cfg) {
  if (statusName === cfg.teamReviewColumn) {
    return COLUMNS.TEAM_REVIEW;
  }
  if (statusName === cfg.finalReviewColumn) {
    return COLUMNS.FINAL_REVIEW;
  }
  if (cfg.authorOnlyColumns.includes(statusName)) {
    return COLUMNS.AUTHOR_ONLY;
  }
  return COLUMNS.UNMANAGED;
}

/**
 * Decide the desired assignee for a card, given its current state.
 *
 * In the "Team Review" column the **Team Reviewer field** is the source of
 * truth for who is reviewing:
 *   - Field set to R  -> the card's assignee is R (even if R is not currently
 *     assigned — this is what makes moving a card back to Team Review after
 *     "In progress" re-assign the original reviewer without a re-self-assign).
 *   - Field empty     -> the reviewer is inferred as the one current assignee
 *     who is not the author (the team's self-assign habit). If exactly one such
 *     person exists, they are assigned AND the field is auto-filled so the
 *     review is recorded.
 *
 * In the "final review" column the PR is assigned to the fixed final reviewer
 * (cfg.finalReviewer, the PO/PL who does the final reviews) and the author.
 * Unlike Team Review, the reviewer is a constant, not read from a board field —
 * the field is left untouched here (it is a record of the Team Reviewer).
 *
 * @param {{ author: string, assignees: string[], statusName: string,
 *           reviewer: string, takeover?: boolean }} pr  `reviewer` is the current
 *           Team Reviewer field value (a login, or '' when unset). `takeover` is
 *           set when the card carries an explicit takeover marker (a configured
 *           PR label), exempting it from reconciliation.
 * @param {{ teamReviewColumn: string, authorOnlyColumns: string[],
 *           finalReviewColumn: string, finalReviewer: string }} cfg
 * @returns {{ kind: string, targets: string[] | null, setField: string | null,
 *             anomaly: string | null }}
 *   `targets` are the desired assignees (exactly these, no others), or `null`
 *   when the card is in an unmanaged column (skip it). `setField` is the value to write to the Team
 *   Reviewer field (the auto-fill) or `null` to leave the field untouched.
 *   `anomaly` describes a data problem worth logging (never changes the safe
 *   fallback of keeping the author).
 */
export function computeTarget(pr, cfg) {
  // Explicit takeover (PR label): the bot never touches such a
  // card — e.g. a PR taken over after its author left the team.
  if (pr.takeover) {
    return { kind: COLUMNS.TAKEOVER, targets: null, setField: null, anomaly: null };
  }

  const kind = classifyStatus(pr.statusName, cfg);

  if (kind === COLUMNS.UNMANAGED) {
    // Leave unmanaged columns alone; the reviewer field is a record we don't clear.
    return { kind, targets: null, setField: null, anomaly: null };
  }

  if (kind === COLUMNS.AUTHOR_ONLY) {
    // Author-only columns: the field is ignored for assignment but never clobbered.
    const authorAssigned = pr.assignees.includes(pr.author);
    if (!authorAssigned) {
      // The author is absent from the card (e.g. they left the team). Keep the
      // author (safe, no silent ownership transfer) and flag it so the takeover
      // can be made explicit (Takeover label) or the author unassigned.
      const others = pr.assignees.filter((login) => login !== pr.author);
      const anomaly =
        others.length === 0
          ? 'author not assigned on author-only card; re-adding author'
          : `author not assigned but ${others.join(', ')} self-assigned on author-only card; kept author (add the Takeover label if ${others.join(', ')} took over)`;
      return { kind, targets: [pr.author], setField: null, anomaly };
    }
    return { kind, targets: [pr.author], setField: null, anomaly: null };
  }

  if (kind === COLUMNS.FINAL_REVIEW) {
    // Final review column: the fixed final reviewer (cfg.finalReviewer, the
    // PO/PL who does the final reviews) is assigned next to the author. The
    // reviewer is not read from the board's Team Reviewer field; that field is
    // a record of the Team Review and is left untouched here (setField: null).
    const targets = [...new Set([cfg.finalReviewer, pr.author])];
    return { kind, targets, setField: null, anomaly: null };
  }

  // TEAM_REVIEW: the Team Reviewer field is the source of truth.
  const reviewer = pr.reviewer || '';

  if (reviewer) {
    // Field names someone: they are the reviewer. Assign them and (idempotently)
    // leave the field as-is.
    const nonAuthors = pr.assignees.filter((login) => login !== pr.author);
    const other = nonAuthors.find((login) => login !== reviewer);
    const anomaly =
      other !== undefined
        ? `field says ${reviewer} but ${other} is also self-assigned; field wins`
        : null;
    return { kind, targets: [reviewer], setField: null, anomaly };
  }

  // Field empty: infer the reviewer from the self-assign habit.
  const nonAuthors = pr.assignees.filter((login) => login !== pr.author);
  if (nonAuthors.length === 1) {
    // Exactly one reviewer self-assigned: assign them and record it in the field.
    return { kind, targets: [nonAuthors[0]], setField: nonAuthors[0], anomaly: null };
  }
  if (nonAuthors.length === 0) {
    // No reviewer has self-assigned yet -> keep the author (silently).
    return { kind, targets: [pr.author], setField: null, anomaly: null };
  }
  // More than one non-author: ambiguous. Keep the author (safe) and flag it.
  return {
    kind,
    targets: [pr.author],
    setField: null,
    anomaly: `multiple reviewers assigned (${nonAuthors.join(', ')}); kept author`,
  };
}

/**
 * Compute which assignees to add/remove so the card ends up with exactly the
 * `targets` assignees.
 * @param {string[]} currentAssignees
 * @param {string[] | null} targets
 * @returns {{ toAdd: string[], toRemove: string[], changed: boolean }}
 */
export function computeDiff(currentAssignees, targets) {
  const wanted = targets ?? [];
  const toRemove = currentAssignees.filter((login) => !wanted.includes(login));
  const toAdd = wanted.filter((login) => !currentAssignees.includes(login));
  return { toAdd, toRemove, changed: toAdd.length > 0 || toRemove.length > 0 };
}

/** Number of the IDEasy board (github.com/orgs/devonfw/projects/5). */
const DEFAULT_BOARD_NUMBER = 5;

/**
 * Parse the BOARD_NUMBER setting. Unset or empty means the IDEasy board.
 * @param {string | undefined} raw
 * @returns {number}
 */
export function parseBoardNumber(raw) {
  const text = (raw ?? '').trim();
  if (text === '') {
    return DEFAULT_BOARD_NUMBER;
  }
  if (!/^[1-9]\d*$/.test(text)) {
    throw new Error(`BOARD_NUMBER must be a positive integer, got "${raw}"`);
  }
  return Number(text);
}

/**
 * Validate the result of the direct `organization { projectV2(number) }` lookup.
 * Fails if the token cannot see the board, or if the number points at a board
 * with another title than expected (guards against a wrong BOARD_NUMBER).
 * @param {any} data GraphQL `data` of the lookup (may be null)
 * @param {number} number
 * @param {string} expectedTitle
 * @returns {{ id: string, title: string }}
 */
export function resolveBoard(data, number, expectedTitle) {
  const project = data?.organization?.projectV2;
  if (!project) {
    throw new Error(
      `Could not read board #${number}. The token needs org "Projects: Read and write" ` +
        `access to it (the built-in GITHUB_TOKEN cannot read org boards).`,
    );
  }
  if (project.title !== expectedTitle) {
    throw new Error(
      `Board #${number} is titled "${project.title}", expected "${expectedTitle}". ` +
        `Check BOARD_NUMBER / BOARD_TITLE.`,
    );
  }
  return { id: project.id, title: project.title };
}
