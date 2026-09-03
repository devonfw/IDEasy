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
//   - Columns that are neither (e.g. New / Research / Refinement / Done) are
//     left untouched so the bot never fights the team's other conventions.
//
// This file has no I/O so the rules can be unit tested in isolation.

export const COLUMNS = Object.freeze({
  TEAM_REVIEW: 'team-review',
  AUTHOR_ONLY: 'author-only',
  UNMANAGED: 'unmanaged',
  TAKEOVER: 'takeover',
});

/**
 * Classify a Status option name against the configuration.
 * @param {string} statusName The Status value of a card (e.g. "Team Review").
 * @param {{ teamReviewColumn: string, authorOnlyColumns: string[] }} cfg
 * @returns {typeof COLUMNS[keyof typeof COLUMNS]}
 */
export function classifyStatus(statusName, cfg) {
  if (statusName === cfg.teamReviewColumn) {
    return COLUMNS.TEAM_REVIEW;
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
 * @param {{ author: string, assignees: string[], statusName: string,
 *           reviewer: string, takeover?: boolean }} pr  `reviewer` is the current
 *           Team Reviewer field value (a login, or '' when unset). `takeover` is
 *           set when the card carries an explicit takeover marker (a configured
 *           PR label), exempting it from reconciliation.
 * @param {{ teamReviewColumn: string, authorOnlyColumns: string[] }} cfg
 * @returns {{ kind: string, target: string | null, setField: string | null,
 *             anomaly: string | null }}
 *   `target` is the single desired assignee, or `null` when the card is in an
 *   unmanaged column (skip it). `setField` is the value to write to the Team
 *   Reviewer field (the auto-fill) or `null` to leave the field untouched.
 *   `anomaly` describes a data problem worth logging (never changes the safe
 *   fallback of keeping the author).
 */
export function computeTarget(pr, cfg) {
  // Explicit takeover (PR label): the bot never touches such a
  // card — e.g. a PR taken over after its author left the team.
  if (pr.takeover) {
    return { kind: COLUMNS.TAKEOVER, target: null, setField: null, anomaly: null };
  }

  const kind = classifyStatus(pr.statusName, cfg);

  if (kind === COLUMNS.UNMANAGED) {
    // Leave unmanaged columns alone; the reviewer field is a record we don't clear.
    return { kind, target: null, setField: null, anomaly: null };
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
      return { kind, target: pr.author, setField: null, anomaly };
    }
    return { kind, target: pr.author, setField: null, anomaly: null };
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
    return { kind, target: reviewer, setField: null, anomaly };
  }

  // Field empty: infer the reviewer from the self-assign habit.
  const nonAuthors = pr.assignees.filter((login) => login !== pr.author);
  if (nonAuthors.length === 1) {
    // Exactly one reviewer self-assigned: assign them and record it in the field.
    return { kind, target: nonAuthors[0], setField: nonAuthors[0], anomaly: null };
  }
  if (nonAuthors.length === 0) {
    // No reviewer has self-assigned yet -> keep the author (silently).
    return { kind, target: pr.author, setField: null, anomaly: null };
  }
  // More than one non-author: ambiguous. Keep the author (safe) and flag it.
  return {
    kind,
    target: pr.author,
    setField: null,
    anomaly: `multiple reviewers assigned (${nonAuthors.join(', ')}); kept author`,
  };
}

/**
 * Compute which assignees to add/remove so the card ends up with exactly the
 * single `target` assignee.
 * @param {string[]} currentAssignees
 * @param {string | null} target
 * @returns {{ toAdd: string[], toRemove: string[], changed: boolean }}
 */
export function computeDiff(currentAssignees, target) {
  const toRemove = currentAssignees.filter((login) => login !== target);
  const toAdd = target !== null && !currentAssignees.includes(target) ? [target] : [];
  return { toAdd, toRemove, changed: toAdd.length > 0 || toRemove.length > 0 };
}
