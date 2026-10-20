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
package org.eclipse.pde.internal.ui.editor.m3;

import java.util.Arrays;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.m3.M3ViewerDelta;
import org.eclipse.jface.viewers.m3.M3Viewers;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/**
 * The {@code pde-model} change-source adapter of the view template (brief rule
 * 27): a form section answers an {@link IModelChangedEvent} of its PDE editor
 * model (plug-in, bundle, feature, product, site, build, schema or DS model)
 * with one viewer delta instead of {@code viewer.refresh()}.
 * <p>
 * {@link #refresh(StructuredViewer, IModelChangedEvent, Function)} is the
 * drop-in replacement for the section's {@code viewer.refresh()}: it diffs the
 * rows the widget shows under the parent against the rows a refresh would show
 * (content provider, filters, comparator) and emits removals, insertions at the
 * refreshed positions and label updates for the rows of the changed model
 * objects. Rows that stay keep their items and labels; they are the longest
 * increasing subsequence of their new positions, so a row that changed places
 * is detected and, until the viewer delta has a move that keeps selection and
 * expansion, answered with the stock refresh.
 * </p>
 * <p>
 * The stock {@code refresh()} still runs when the delta cannot reproduce it: a
 * {@code WORLD_CHANGED} event (the document was replaced), a virtual widget,
 * rows below an unexpanded or unknown parent, filters without a comparator (the
 * insert API cannot place a row at a filtered position), or a move. Each
 * fallback is counted by {@link Fallback} reason.
 * </p>
 */
public final class M3ModelChangedDeltas {

	private static final Logger LOG = Logger.getLogger(M3ModelChangedDeltas.class.getName());

	/** Why the stock refresh ran instead of a delta. */
	public enum Fallback {
		/** the document was replaced: the section's stock path */
		WORLD_CHANGED,
		/** filters without a comparator: the insert API cannot place a row at a filtered position */
		FILTERS_WITHOUT_COMPARATOR,
		/** a virtual widget, a dummy item or a collapsed parent: the shown rows are not all known */
		ROWS_UNKNOWN,
		/** rows changed places: needs a move operation in the viewer delta that keeps selection and expansion */
		MOVE
	}

	/** Full refreshes this adapter fell back to, per reason (differential and delta-gate counters). */
	private static final long[] FALLBACKS = new long[Fallback.values().length];

	private static Fallback lastReason;

	private M3ModelChangedDeltas() {
	}

	/** @return the number of times the stock refresh ran instead of a delta */
	public static long fallbacks() {
		long n = 0;
		for (long f : FALLBACKS) {
			n += f;
		}
		return n;
	}

	/**
	 * @param reason the reason
	 * @return the number of times the stock refresh ran for {@code reason}
	 */
	public static long fallbacks(Fallback reason) {
		return FALLBACKS[reason.ordinal()];
	}

	/**
	 * Replaces {@code viewer.refresh()} in a section's {@code modelChanged}: the
	 * rows of the parent of the changed rows (the input for tables) become what
	 * a refresh would show, through one delta.
	 *
	 * @param viewer the section's table or tree viewer (UI thread)
	 * @param event  the model event
	 * @param rowOf  maps a changed model object to the row the viewer shows for
	 *               it, or to {@code null}
	 */
	public static void refresh(StructuredViewer viewer, IModelChangedEvent event, Function<Object, Object> rowOf) {
		lastReason = Fallback.WORLD_CHANGED;
		M3ViewerDelta delta = event.getChangeType() == IModelChangedEvent.WORLD_CHANGED ? null
				: toDelta(viewer, parentOf(viewer, event, rowOf), rows(event, rowOf));
		if (delta == null) {
			Fallback reason = lastReason;
			FALLBACKS[reason.ordinal()]++;
			LOG.log(Level.FINE, () -> "stock refresh (" + reason + ") for " + viewer.getClass().getSimpleName()); //$NON-NLS-1$ //$NON-NLS-2$
			viewer.refresh();
		} else {
			M3Viewers.apply(viewer, delta);
		}
	}

	/**
	 * The delta that turns the rows shown under {@code parent} into the rows a
	 * refresh of {@code parent} would show, updating the labels of
	 * {@code changedRows} that stay.
	 *
	 * @param viewer      the viewer
	 * @param parent      the input (tables) or an expanded tree element
	 * @param changedRows rows whose labels may have changed
	 * @return the delta, or {@code null} when only a refresh reproduces it
	 */
	public static M3ViewerDelta toDelta(StructuredViewer viewer, Object parent, Object[] changedRows) {
		Object[] old = shownRows(viewer, parent);
		if (old == null) {
			lastReason = Fallback.ROWS_UNKNOWN;
			return null;
		}
		if (viewer.getComparator() == null && viewer.getFilters().length > 0) {
			lastReason = Fallback.FILTERS_WITHOUT_COMPARATOR;
			return null;
		}
		Object[] now = refreshedRows(viewer, parent);
		IElementComparer comparer = viewer.getComparer();
		int[] oldAt = positions(comparer, now, old); // index in now of each old row, -1 when gone
		boolean[] stable = stable(oldAt);
		M3ViewerDelta delta = new M3ViewerDelta();
		boolean[] placed = new boolean[now.length];
		for (int i = 0; i < old.length; i++) {
			if (stable[i]) {
				placed[oldAt[i]] = true;
			} else if (oldAt[i] >= 0) {
				lastReason = Fallback.MOVE; // remove + insert would drop its selection and expansion
				return null;
			} else {
				delta.remove(old[i]);
			}
		}
		for (int j = 0; j < now.length; j++) {
			if (!placed[j]) {
				delta.add(parent, now[j], viewer.getComparator() != null ? -1 : j);
			}
		}
		for (Object row : changedRows) {
			int j = indexOf(comparer, now, row);
			if (j >= 0 && placed[j]) {
				delta.update(now[j], null);
			}
		}
		return delta;
	}

	/* rows of the changed objects, in event order */
	static Object[] rows(IModelChangedEvent event, Function<Object, Object> rowOf) {
		Object[] changed = event.getChangedObjects();
		if (changed == null) {
			return new Object[0];
		}
		Object[] rows = new Object[changed.length];
		int n = 0;
		for (Object o : changed) {
			Object row = o == null ? null : rowOf.apply(o);
			if (row != null) {
				rows[n++] = row;
			}
		}
		return Arrays.copyOf(rows, n);
	}

	/* the parent whose rows change: the input for tables, else the common tree parent of the changed rows */
	static Object parentOf(StructuredViewer viewer, IModelChangedEvent event, Function<Object, Object> rowOf) {
		if (!(viewer instanceof AbstractTreeViewer) || !(viewer.getContentProvider() instanceof ITreeContentProvider tree)) {
			return viewer.getInput();
		}
		Object parent = null;
		for (Object row : rows(event, rowOf)) {
			Object p = tree.getParent(row);
			p = p == null ? viewer.getInput() : p;
			if (parent != null && parent != p) {
				return viewer.getInput();
			}
			parent = p;
		}
		return parent == null ? viewer.getInput() : parent;
	}

	/* the rows the widget shows under parent today, or null when they are not all known */
	static Object[] shownRows(StructuredViewer viewer, Object parent) {
		Item[] items;
		if (viewer instanceof AbstractTableViewer table && table.getControl() instanceof Table widget) {
			if ((widget.getStyle() & SWT.VIRTUAL) != 0) {
				return null;
			}
			items = widget.getItems();
		} else if (viewer instanceof AbstractTreeViewer tree && tree.getControl() instanceof Tree widget) {
			if ((widget.getStyle() & SWT.VIRTUAL) != 0) {
				return null;
			}
			if (parent == viewer.getInput()) {
				items = widget.getItems();
			} else {
				Widget item = viewer.testFindItem(parent);
				if (!(item instanceof TreeItem treeItem) || !treeItem.getExpanded()) {
					return null;
				}
				items = treeItem.getItems();
			}
		} else {
			return null;
		}
		Object[] rows = new Object[items.length];
		for (int i = 0; i < items.length; i++) {
			rows[i] = items[i].getData();
			if (rows[i] == null) {
				return null; // a dummy or unrealized item: only a refresh knows its row
			}
		}
		return rows;
	}

	/* what a refresh of parent shows: content provider order, then filters, then the comparator */
	static Object[] refreshedRows(StructuredViewer viewer, Object parent) {
		IContentProvider provider = viewer.getContentProvider();
		Object[] rows;
		if (parent != viewer.getInput() && provider instanceof ITreeContentProvider tree) {
			rows = tree.getChildren(parent);
		} else if (provider instanceof IStructuredContentProvider structured) {
			rows = structured.getElements(parent);
		} else {
			rows = new Object[0];
		}
		rows = rows == null ? new Object[0] : rows.clone();
		for (ViewerFilter filter : viewer.getFilters()) {
			rows = filter.filter(viewer, parent, rows);
		}
		if (viewer.getComparator() != null) {
			viewer.getComparator().sort(viewer, rows);
		}
		return rows;
	}

	/* for each old row its index in now, or -1 (transient open-addressing table keyed by the comparer) */
	static int[] positions(IElementComparer comparer, Object[] now, Object[] old) {
		int capacity = Integer.highestOneBit(Math.max(4, now.length * 2 - 1)) << 1;
		int mask = capacity - 1;
		int[] table = new int[capacity]; // index + 1, 0 = empty
		for (int j = 0; j < now.length; j++) {
			int s = hash(comparer, now[j]) & mask;
			while (table[s] != 0) {
				s = (s + 1) & mask;
			}
			table[s] = j + 1;
		}
		int[] at = new int[old.length];
		boolean[] taken = new boolean[now.length];
		for (int i = 0; i < old.length; i++) {
			at[i] = -1;
			for (int s = hash(comparer, old[i]) & mask; table[s] != 0; s = (s + 1) & mask) {
				int j = table[s] - 1;
				if (!taken[j] && same(comparer, now[j], old[i])) {
					at[i] = j;
					taken[j] = true;
					break;
				}
			}
		}
		return at;
	}

	/* rows that keep their items: a longest increasing subsequence of the new positions (patience sorting) */
	static boolean[] stable(int[] at) {
		int n = at.length;
		int[] tails = new int[n];
		int[] tailIndex = new int[n];
		int[] previous = new int[n];
		int length = 0;
		for (int i = 0; i < n; i++) {
			if (at[i] < 0) {
				continue;
			}
			int lo = 0;
			int hi = length;
			while (lo < hi) {
				int mid = (lo + hi) >>> 1;
				if (tails[mid] < at[i]) {
					lo = mid + 1;
				} else {
					hi = mid;
				}
			}
			tails[lo] = at[i];
			tailIndex[lo] = i;
			previous[i] = lo > 0 ? tailIndex[lo - 1] : -1;
			length = Math.max(length, lo + 1);
		}
		boolean[] stable = new boolean[n];
		for (int i = length > 0 ? tailIndex[length - 1] : -1; i >= 0; i = previous[i]) {
			stable[i] = true;
		}
		return stable;
	}

	static int indexOf(IElementComparer comparer, Object[] rows, Object row) {
		for (int j = 0; j < rows.length; j++) {
			if (same(comparer, rows[j], row)) {
				return j;
			}
		}
		return -1;
	}

	private static boolean same(IElementComparer comparer, Object a, Object b) {
		return comparer != null ? comparer.equals(a, b) : a.equals(b);
	}

	private static int hash(IElementComparer comparer, Object o) {
		int h = (comparer != null ? comparer.hashCode(o) : o.hashCode()) * 0x9E3779B9;
		return h ^ (h >>> 16);
	}
}
