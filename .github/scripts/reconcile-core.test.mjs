// @ts-check
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  classifyStatus,
  computeTarget,
  computeDiff,
  COLUMNS,
} from './reconcile-core.mjs';

// The board's real column names (matched against the Status field option names).
const CFG = {
  teamReviewColumn: 'Team Review',
  authorOnlyColumns: ['🏗 In progress', '👀 In review'],
};

// pr helper: `reviewer` is the current "Team Reviewer" field value (a login or '').
const pr = (o) => ({ reviewer: '', ...o });

test('classifyStatus: Team Review is the reviewer column', () => {
  assert.equal(classifyStatus('Team Review', CFG), COLUMNS.TEAM_REVIEW);
});

test('classifyStatus: configured columns are author-only', () => {
  assert.equal(classifyStatus('🏗 In progress', CFG), COLUMNS.AUTHOR_ONLY);
  assert.equal(classifyStatus('👀 In review', CFG), COLUMNS.AUTHOR_ONLY);
});

test('classifyStatus: unconfigured columns are unmanaged (left alone)', () => {
  assert.equal(classifyStatus('🆕 New', CFG), COLUMNS.UNMANAGED);
  assert.equal(classifyStatus('Research', CFG), COLUMNS.UNMANAGED);
  assert.equal(classifyStatus('Refinement', CFG), COLUMNS.UNMANAGED);
  assert.equal(classifyStatus('✅ Done', CFG), COLUMNS.UNMANAGED);
});

// --- Team Review: field is the source of truth ----------------------------

test('Team Review, field set + reviewer assigned -> reviewer sole assignee, field untouched', () => {
  const r = computeTarget(pr({ author: 'A', assignees: ['A', 'R'], reviewer: 'R', statusName: 'Team Review' }), CFG);
  assert.equal(r.kind, COLUMNS.TEAM_REVIEW);
  assert.equal(r.target, 'R');
  assert.equal(r.setField, null);
});

test('Team Review, field set but reviewer NOT assigned -> auto-reassign reviewer (re-entry)', () => {
  // Author moved the card back to Team Review after resolving comments; the
  // reviewer was unassigned in "In progress". The field still remembers R.
  const r = computeTarget(pr({ author: 'A', assignees: ['A'], reviewer: 'R', statusName: 'Team Review' }), CFG);
  assert.equal(r.target, 'R');
  assert.equal(r.setField, null);
});

test('Team Review, field set to R1 but a different person self-assigned -> field wins + flag', () => {
  const r = computeTarget(pr({ author: 'A', assignees: ['A', 'R2'], reviewer: 'R1', statusName: 'Team Review' }), CFG);
  assert.equal(r.target, 'R1');
  assert.match(r.anomaly, /R2/);
  assert.equal(r.setField, null);
});

test('Team Review, field empty + single self-assigner -> assign them AND auto-fill field', () => {
  const r = computeTarget(pr({ author: 'A', assignees: ['A', 'M'], reviewer: '', statusName: 'Team Review' }), CFG);
  assert.equal(r.kind, COLUMNS.TEAM_REVIEW);
  assert.equal(r.target, 'M');
  assert.equal(r.setField, 'M');
});

test('Team Review, field empty + no reviewer yet -> keep author, silent, no field write', () => {
  const r = computeTarget(pr({ author: 'A', assignees: ['A'], reviewer: '', statusName: 'Team Review' }), CFG);
  assert.equal(r.target, 'A');
  assert.equal(r.setField, null);
  assert.equal(r.anomaly, null);
});

test('Team Review, field empty + author unassigned -> reassign author, no field write', () => {
  const r = computeTarget(pr({ author: 'A', assignees: [], reviewer: '', statusName: 'Team Review' }), CFG);
  assert.equal(r.target, 'A');
  assert.equal(r.setField, null);
});

test('Team Review, field empty + multiple self-assigners -> ambiguous, keep author, no field write', () => {
  const r = computeTarget(pr({ author: 'A', assignees: ['B', 'C', 'A'], reviewer: '', statusName: 'Team Review' }), CFG);
  assert.equal(r.target, 'A');
  assert.match(r.anomaly, /multiple reviewers/);
  assert.equal(r.setField, null);
});

// --- Author-only / unmanaged: field is ignored for assignment, never clobbered ---

test('Author-only columns -> author sole assignee, reviewer field left alone', () => {
  const r = computeTarget(pr({ author: 'A', assignees: ['A', 'krystynaShatkovska'], reviewer: 'R', statusName: '👀 In review' }), CFG);
  assert.equal(r.kind, COLUMNS.AUTHOR_ONLY);
  assert.equal(r.target, 'A');
  assert.equal(r.setField, null);
});

test('Unmanaged columns -> no target (skip), reviewer field left alone', () => {
  const r = computeTarget(pr({ author: 'MeShehi', assignees: ['majeteSil', 'MeShehi'], reviewer: 'majeteSil', statusName: '✅ Done' }), CFG);
  assert.equal(r.kind, COLUMNS.UNMANAGED);
  assert.equal(r.target, null);
  assert.equal(r.setField, null);
});

// --- Takeover exemption -----------------------------------------------------
// A card with the Takeover flag (a configured label on the PR) is exempt from
// reconciliation: the bot never touches its assignees (e.g. a PR taken over
// after its author left the team).

test('takeover flag -> card is skipped (author-only column)', () => {
  const r = computeTarget(pr({ author: 'A', assignees: ['B'], reviewer: '', statusName: '👀 In review', takeover: true }), CFG);
  assert.equal(r.kind, COLUMNS.TAKEOVER);
  assert.equal(r.target, null);
  assert.equal(r.setField, null);
  assert.equal(r.anomaly, null);
});

test('takeover flag -> card is skipped (Team Review, reviewer field set)', () => {
  const r = computeTarget(pr({ author: 'A', assignees: ['R'], reviewer: 'R', statusName: 'Team Review', takeover: true }), CFG);
  assert.equal(r.kind, COLUMNS.TAKEOVER);
  assert.equal(r.target, null);
  assert.equal(r.setField, null);
});

test('takeover flag -> card is skipped (unmanaged column)', () => {
  const r = computeTarget(pr({ author: 'A', assignees: ['B'], reviewer: '', statusName: '🆕 New', takeover: true }), CFG);
  assert.equal(r.kind, COLUMNS.TAKEOVER);
  assert.equal(r.target, null);
});

test('author-only, author unassigned + one self-assigner -> still targets author, anomaly suggests the Takeover flag', () => {
  const r = computeTarget(pr({ author: 'A', assignees: ['B'], reviewer: '', statusName: '🏗 In progress' }), CFG);
  assert.equal(r.target, 'A');
  assert.equal(r.setField, null);
  assert.match(r.anomaly, /Takeover/);
});

// --- computeDiff ----------------------------------------------------------

test('computeDiff: no-op when already at target', () => {
  const d = computeDiff(['Hiepiscus'], 'Hiepiscus');
  assert.deepEqual(d, { toAdd: [], toRemove: [], changed: false });
});

test('computeDiff: swaps author out and reviewer in (Team Review handoff)', () => {
  const d = computeDiff(['Hiepiscus'], 'majeteSil');
  assert.deepEqual(d.toRemove, ['Hiepiscus']);
  assert.deepEqual(d.toAdd, ['majeteSil']);
  assert.equal(d.changed, true);
});

test('computeDiff: unassigns the extra assignee (In review -> author only)', () => {
  const d = computeDiff(['samuelkos17', 'krystynaShatkovska'], 'samuelkos17');
  assert.deepEqual(d.toAdd, []);
  assert.deepEqual(d.toRemove, ['krystynaShatkovska']);
  assert.equal(d.changed, true);
});

test('computeDiff: null target removes everyone (defensive)', () => {
  const d = computeDiff(['a', 'b'], null);
  assert.deepEqual(d.toRemove, ['a', 'b']);
  assert.deepEqual(d.toAdd, []);
});
