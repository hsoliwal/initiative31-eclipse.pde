# PDE views on one model, deltas end to end

Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0

User, 2026-10-20: "all of eclipse implemented the same way", "all delta based view". The fork was approved the same
day. This branch applies the eclipse.platform pattern (hsoliwal/eclipse.platform, branch
m3/platform-views-viewport-20261020, docs/m3/platform-views/) to the views here: Plug ins, Target Platform State, Dependencies, Registry, manifest and target editor sections.

The pattern has four parts:

- One lane model per view. Model element trees and viewer element maps are thin views over it.
- Change handlers apply only deltas (M3ViewerDelta from hsoliwal/eclipse.platform.ui, branch m3/jface-viewport-20261019).
- Hidden views keep their lanes current and apply one window patch when shown (M3PartVisibility).
- Resize and layout are viewport deltas.

Fork state: hsoliwal/initiative31-eclipse.pde master is 7 commits ahead of and 111 behind eclipse-pde/eclipse.pde master (gh api compare, 2026-10-20); upstream is the remote `upstream`. Syncing is the upstream-sync agent's job.

## Step 1: inventory and delta gate

| File | What |
|---|---|
| VIEW_INVENTORY.tsv | One row per view: change source, update path, duplicate stores, full-rebuild sites (file:line, every reference checked), target delta path, delta_gate. |
| DELTA_GATE.tsv | 157 rebuild call sites in the view scopes (scopes.tsv), classified by eclipse.platform docs/m3/platform-views/tools/delta_gate.py: 100 FAIL (reached from a model listener) and 10 refresh(element) reached from one. The classifier is name-based and over-approximates. Inventory rows marked "hand review" correct the listeners it cannot follow (field lambdas, method references). |
| DELTA_CENSUS_BASELINE.tsv | C:/m3-superset/tools/delta_census.sh before any change: 658 main sites. |

The next steps follow eclipse.platform: measure first (OPTIMIZATION_PASS.md), convert view by view with a
stock-vs-ours differential (full-refresh counter = 0, a hide/10^4 deltas/show gate, a resize sweep), then add
OPTIMIZATION_LEDGER.tsv rows.
