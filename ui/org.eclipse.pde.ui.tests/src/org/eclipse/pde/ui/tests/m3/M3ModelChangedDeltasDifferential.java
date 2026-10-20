/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.m3;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.ui.editor.m3.M3ModelChangedDeltas;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Differential of the pde-model adapter: two identical section viewers over
 * one model, the stock one answering every {@link IModelChangedEvent} with
 * {@code viewer.refresh()} (what the PDE form sections did), the other with
 * {@link M3ModelChangedDeltas#refresh}. After every step the rows (identity and
 * order), labels and selection must be equal; the adapter side must not have
 * refreshed fully except for {@code WORLD_CHANGED} and filters without a
 * comparator. Seeded scripts insert at random positions, remove, relabel,
 * reorder (an order property on the owner), replace the world, switch sorting
 * and filtering, select, and resize the shell. Table and tree (root level, with
 * expanded children) variants.
 * <p>
 * Usage: {@code M3ModelChangedDeltasDifferential <out-file> <seeds> <steps>};
 * exit code 1 on any mismatch.
 * </p>
 */
public final class M3ModelChangedDeltasDifferential {

	/** A model object of a section: identity, a mutable label, children for the tree variant. */
	static final class Row {
		final int id;
		String name;
		final List<Row> children = new ArrayList<>();

		Row(int id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** The owner of the rows, the viewers' input. */
	static final class Owner {
		final List<Row> rows = new ArrayList<>();
	}

	static final IModelChangeProvider PROVIDER = new IModelChangeProvider() {
		@Override
		public void addModelChangedListener(IModelChangedListener listener) {
		}

		@Override
		public void fireModelChanged(IModelChangedEvent event) {
		}

		@Override
		public void fireModelObjectChanged(Object object, String property, Object oldValue, Object newValue) {
		}

		@Override
		public void removeModelChangedListener(IModelChangedListener listener) {
		}
	};

	static final class Provider extends ArrayContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object input) {
			return ((Owner) input).rows.toArray();
		}

		@Override
		public Object[] getChildren(Object parent) {
			return ((Row) parent).children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof Row r && !r.children.isEmpty();
		}
	}

	/** Counts full refreshes of the adapter side. */
	static final class CountingTable extends TableViewer {
		int fullRefreshes;

		CountingTable(Shell shell) {
			super(shell, SWT.MULTI | SWT.BORDER);
		}

		@Override
		public void refresh() {
			fullRefreshes++;
			super.refresh();
		}
	}

	static final class CountingTree extends TreeViewer {
		int fullRefreshes;

		CountingTree(Shell shell) {
			super(shell, SWT.MULTI | SWT.BORDER);
		}

		@Override
		public void refresh() {
			fullRefreshes++;
			super.refresh();
		}
	}

	private M3ModelChangedDeltasDifferential() {
	}

	public static void main(String[] args) throws IOException {
		Display display = new Display();
		int mismatches = 0;
		long expectedFallbacks = 0;
		long unexpectedFull = 0;
		try (Writer w = Files.newBufferedWriter(Path.of(args[0]), StandardCharsets.UTF_8)) {
			int seeds = Integer.parseInt(args[1]);
			int steps = Integer.parseInt(args[2]);
			for (int seed = 1; seed <= seeds; seed++) {
				for (boolean tree : new boolean[] { false, true }) {
					long[] result = run(display, w, seed, tree, steps);
					mismatches += (int) result[0];
					expectedFallbacks += result[1];
					unexpectedFull += result[2];
				}
			}
			StringBuilder reasons = new StringBuilder();
			for (M3ModelChangedDeltas.Fallback f : M3ModelChangedDeltas.Fallback.values()) {
				reasons.append(' ').append(f).append('=').append(M3ModelChangedDeltas.fallbacks(f));
			}
			w.write("mismatches " + mismatches + " fallbacks " + expectedFallbacks + " (" + reasons.toString().trim() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ ") full_refreshes_without_fallback " + unexpectedFull + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			display.dispose();
		}
		System.exit(mismatches == 0 && unexpectedFull == 0 ? 0 : 1);
	}

	static long[] run(Display display, Writer w, long seed, boolean tree, int steps) throws IOException {
		Random r = new Random(seed);
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		StructuredViewer stock = tree ? new TreeViewer(shell, SWT.MULTI | SWT.BORDER) : new TableViewer(shell, SWT.MULTI | SWT.BORDER);
		StructuredViewer ours = tree ? new CountingTree(shell) : new CountingTable(shell);
		Owner owner = new Owner();
		int[] next = { 0 };
		for (int i = 0; i < 5 + r.nextInt(20); i++) {
			owner.rows.add(row(r, next, tree));
		}
		for (StructuredViewer v : new StructuredViewer[] { stock, ours }) {
			v.setContentProvider(new Provider());
			v.setLabelProvider(new LabelProvider());
			v.setInput(owner);
			if (v instanceof TreeViewer t) {
				t.expandAll();
			}
		}
		shell.setSize(300, 400);
		shell.open();
		long mismatches = 0;
		long expected = 0;
		long full = 0; // full refreshes of the adapter side during model events (sort and filter switches refresh by design)
		for (int step = 0; step < steps; step++) {
			int op = r.nextInt(100);
			IModelChangedEvent event;
			boolean fallbackExpected = false;
			if (op < 30) {
				List<Object> added = new ArrayList<>();
				for (int k = 1 + r.nextInt(r.nextInt(10) == 0 ? 40 : 3); k > 0; k--) {
					Row row = row(r, next, tree);
					owner.rows.add(r.nextInt(owner.rows.size() + 1), row);
					added.add(row);
				}
				event = new ModelChangedEvent(PROVIDER, IModelChangedEvent.INSERT, added.toArray(), null);
			} else if (op < 50 && !owner.rows.isEmpty()) {
				List<Object> removed = new ArrayList<>();
				for (int k = 1 + r.nextInt(3); k > 0 && !owner.rows.isEmpty(); k--) {
					removed.add(owner.rows.remove(r.nextInt(owner.rows.size())));
				}
				event = new ModelChangedEvent(PROVIDER, IModelChangedEvent.REMOVE, removed.toArray(), null);
			} else if (op < 68 && !owner.rows.isEmpty()) {
				List<Object> changed = new ArrayList<>();
				for (int k = 1 + r.nextInt(3); k > 0; k--) {
					Row row = owner.rows.get(r.nextInt(owner.rows.size()));
					row.name = "r" + row.id + "." + r.nextInt(1000); //$NON-NLS-1$ //$NON-NLS-2$
					changed.add(row);
				}
				event = new ModelChangedEvent(PROVIDER, IModelChangedEvent.CHANGE, changed.toArray(), "name"); //$NON-NLS-1$
			} else if (op < 80 && owner.rows.size() > 1) {
				for (int k = 1 + r.nextInt(3); k > 0; k--) {
					Row moved = owner.rows.remove(r.nextInt(owner.rows.size()));
					owner.rows.add(r.nextInt(owner.rows.size() + 1), moved);
				}
				event = new ModelChangedEvent(PROVIDER, IModelChangedEvent.CHANGE, new Object[] { owner }, "order"); //$NON-NLS-1$
			} else if (op < 83) {
				owner.rows.clear();
				for (int i = r.nextInt(15); i > 0; i--) {
					owner.rows.add(row(r, next, tree));
				}
				event = new ModelChangedEvent(PROVIDER, IModelChangedEvent.WORLD_CHANGED, null, null);
				fallbackExpected = true;
			} else if (op < 88) {
				boolean sorted = r.nextBoolean();
				for (StructuredViewer v : new StructuredViewer[] { stock, ours }) {
					v.setComparator(sorted ? new ViewerComparator() : null);
				}
				w.write("sort " + sorted + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				mismatches += compare(w, seed, step, stock, ours);
				continue;
			} else if (op < 91) {
				int mod = 2 + r.nextInt(3);
				boolean filtered = r.nextBoolean();
				for (StructuredViewer v : new StructuredViewer[] { stock, ours }) {
					v.setFilters(filtered ? new ViewerFilter[] { new ViewerFilter() {
						@Override
						public boolean select(Viewer viewer, Object parent, Object element) {
							return ((Row) element).id % mod != 0;
						}
					} } : new ViewerFilter[0]);
				}
				w.write("filter " + filtered + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				mismatches += compare(w, seed, step, stock, ours);
				continue;
			} else if (op < 96 && !owner.rows.isEmpty()) {
				Object[] pick = { owner.rows.get(r.nextInt(owner.rows.size())), owner.rows.get(r.nextInt(owner.rows.size())) };
				for (StructuredViewer v : new StructuredViewer[] { stock, ours }) {
					v.setSelection(new StructuredSelection(pick));
				}
				w.write("select\n"); //$NON-NLS-1$
				mismatches += compare(w, seed, step, stock, ours);
				continue;
			} else {
				shell.setSize(150 + r.nextInt(400), 100 + r.nextInt(500));
				w.write("resize\n"); //$NON-NLS-1$
				mismatches += compare(w, seed, step, stock, ours);
				continue;
			}
			long fallbacksBefore = M3ModelChangedDeltas.fallbacks();
			int refreshesBefore = fullRefreshes(ours);
			stock.refresh();
			M3ModelChangedDeltas.refresh(ours, event, o -> o);
			long fell = M3ModelChangedDeltas.fallbacks() - fallbacksBefore;
			full += fullRefreshes(ours) - refreshesBefore;
			expected += fell;
			if (fallbackExpected && fell == 0) {
				w.write("WORLD_CHANGED answered without the stock refresh\n"); //$NON-NLS-1$
				mismatches++;
			}
			while (display.readAndDispatch()) {
				// settle
			}
			w.write("step " + step + " op " + op + " type " + event.getChangeType() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			mismatches += compare(w, seed, step, stock, ours);
		}
		shell.dispose();
		w.write("== seed " + seed + (tree ? " tree" : " table") + " mismatches " + mismatches + " full_refreshes " + full //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				+ " expected " + expected + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		return new long[] { mismatches, expected, Math.max(0, full - expected) };
	}

	static Row row(Random r, int[] next, boolean tree) {
		Row row = new Row(next[0]++, "r" + next[0] + "." + r.nextInt(1000)); //$NON-NLS-1$ //$NON-NLS-2$
		if (tree && r.nextInt(3) == 0) {
			for (int c = 1 + r.nextInt(3); c > 0; c--) {
				row.children.add(new Row(next[0]++, "c" + next[0])); //$NON-NLS-1$
			}
		}
		return row;
	}

	static int fullRefreshes(StructuredViewer v) {
		return v instanceof CountingTable t ? t.fullRefreshes : ((CountingTree) v).fullRefreshes;
	}

	static int compare(Writer w, long seed, int step, StructuredViewer stock, StructuredViewer ours) throws IOException {
		String a = dump(stock);
		String b = dump(ours);
		if (a.equals(b)) {
			return 0;
		}
		w.write("MISMATCH seed " + seed + " step " + step + "\n stock: " + a + "\n ours:  " + b + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return 1;
	}

	/* rows (data identity, label, expansion) in widget order, then the selection */
	static String dump(StructuredViewer v) {
		StringBuilder out = new StringBuilder();
		Item[] items = v instanceof TableViewer t ? t.getTable().getItems() : ((TreeViewer) v).getTree().getItems();
		for (Item item : items) {
			dumpItem(item, out);
		}
		out.append(" | sel ").append(((StructuredSelection) v.getSelection()).toList());
		return out.toString();
	}

	static void dumpItem(Item item, StringBuilder out) {
		Object data = item.getData();
		out.append(data instanceof Row row ? row.id : data).append('=').append(item.getText()).append(' ');
		if (item instanceof TreeItem t && t.getExpanded()) {
			out.append('[');
			for (TreeItem child : t.getItems()) {
				dumpItem(child, out);
			}
			out.append(']');
		} else if (item instanceof TableItem) {
			// rows only
		}
	}
}
