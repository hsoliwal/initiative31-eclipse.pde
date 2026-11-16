/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/
package org.eclipse.pde.ui.tests.editor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.plugin.ExportPackageSection;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.StructuredViewerPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.Constants;

/** Exercises real section methods with viewer doubles; creates no native controls. */
public class ExportPackageSectionTest {

	public static class OriginalClient extends ExportPackageSection {
		public OriginalClient(PDEFormPage page, Composite parent) {
			super(page, parent);
		}

		TableSection originalType() {
			return this;
		}

		boolean originalCount() {
			return super.createCount();
		}

		StructuredViewerPart originalFactory() {
			return super.createViewerPart(new String[] { "Add", "Remove" });
		}

		EditableTablePart originalPart() {
			return super.getTablePart();
		}

		IAction originalRename() {
			return super.getRenameAction();
		}

		void originalModification() {
			super.entryModified(this, "value");
		}

		void install(StructuredViewerPart part) {
			fViewerPart = part;
		}
	}

	@Test
	public void originalTableExtensionContractsRemainCallable() {
		OriginalClient client = mock(OriginalClient.class, CALLS_REAL_METHODS);
		assertSame(client, client.originalType());
		assertTrue(client.originalCount());
		StructuredViewerPart part = client.originalFactory();
		assertTrue(part instanceof EditableTablePart);
		client.install(part);
		assertSame(part, client.originalPart());
		assertSame(((EditableTablePart) part).getRenameAction(), client.originalRename());
		client.originalModification();
	}

	@Test
	public void tableInsertKeepsOriginalSelectionAndFocusOrder() throws Exception {
		TableViewer viewer = mock(TableViewer.class);
		Table control = mock(Table.class);
		when(viewer.getTable()).thenReturn(control);
		when(viewer.getControl()).thenReturn(control);
		ExportPackageObject element = mock(ExportPackageObject.class);
		subject(viewer).modelChanged(event(IModelChangedEvent.INSERT, element));
		InOrder order = inOrder(viewer, control);
		order.verify(viewer).add(element);
		order.verify(viewer).setSelection(new StructuredSelection(element), false);
		order.verify(control).setFocus();
	}

	@Test
	public void treeInsertAddsAChildToTheInput() throws Exception {
		TreeViewer viewer = mock(TreeViewer.class);
		Tree control = mock(Tree.class);
		when(viewer.getTree()).thenReturn(control);
		Object input = new Object();
		when(viewer.getInput()).thenReturn(input);
		when(viewer.getControl()).thenReturn(control);
		ExportPackageObject element = mock(ExportPackageObject.class);
		subject(viewer).modelChanged(event(IModelChangedEvent.INSERT, element));
		InOrder order = inOrder(viewer, control);
		order.verify(viewer).add(input, element);
		order.verify(viewer).setSelection(new StructuredSelection(element), false);
		order.verify(control).setFocus();
	}

	@Test
	public void tableRemovalKeepsOriginalIndexBoundaries() throws Exception {
		for (int[] state : new int[][] { { -1, 3, -1 }, { 0, 0, -1 }, { 1, 4, 1 }, { 3, 3, 2 }, { 5, 2, 1 } }) {
			TableViewer viewer = mock(TableViewer.class);
			Table table = mock(Table.class);
			when(viewer.getTable()).thenReturn(table);
			when(table.getSelectionIndex()).thenReturn(state[0]);
			when(table.getItemCount()).thenReturn(state[1]);
			ExportPackageObject element = mock(ExportPackageObject.class);
			subject(viewer).modelChanged(event(IModelChangedEvent.REMOVE, element));
			InOrder order = inOrder(viewer, table);
			order.verify(table).getSelectionIndex();
			order.verify(viewer).remove(element);
			order.verify(table).setSelection(state[2]);
		}
	}

	@Test
	public void treeRemovalKeepsExistingRootSelection() throws Exception {
		for (int[] state : new int[][] { { 0, 1, 0 }, { 2, 4, 2 }, { 4, 3, 2 } }) {
			TreeViewer viewer = mock(TreeViewer.class);
			Tree tree = mock(Tree.class);
			TreeItem selected = mock(TreeItem.class);
			TreeItem remaining = mock(TreeItem.class);
			when(viewer.getTree()).thenReturn(tree);
			when(tree.getSelection()).thenReturn(new TreeItem[] { selected });
			when(tree.indexOf(selected)).thenReturn(state[0]);
			when(tree.getItemCount()).thenReturn(state[1]);
			when(tree.getItem(state[2])).thenReturn(remaining);
			ExportPackageObject element = mock(ExportPackageObject.class);
			subject(viewer).modelChanged(event(IModelChangedEvent.REMOVE, element));
			InOrder order = inOrder(viewer, tree);
			order.verify(viewer).remove(element);
			order.verify(tree).setSelection(remaining);
		}
	}

	@Test
	public void emptyTreeRemovalDoesNotReadAChild() throws Exception {
		TreeViewer viewer = mock(TreeViewer.class);
		Tree tree = mock(Tree.class);
		when(viewer.getTree()).thenReturn(tree);
		when(tree.getSelection()).thenReturn(new TreeItem[0]);
		when(tree.getItemCount()).thenReturn(0);
		ExportPackageObject element = mock(ExportPackageObject.class);
		subject(viewer).modelChanged(event(IModelChangedEvent.REMOVE, element));
		verify(viewer).remove(element);
		verify(tree, never()).getItem(0);
		verify(tree, never()).getItem(-1);
	}

	@Test
	public void headerRefreshSelectsLastTableElement() throws Exception {
		TableViewer viewer = mock(TableViewer.class);
		Table table = mock(Table.class);
		Object element = new Object();
		when(viewer.getTable()).thenReturn(table);
		when(table.getItemCount()).thenReturn(2);
		when(viewer.getElementAt(1)).thenReturn(element);
		IModelChangedEvent event = event(IModelChangedEvent.CHANGE, mock(ExportPackageObject.class));
		when(event.getChangedProperty()).thenReturn(Constants.EXPORT_PACKAGE);
		subject(viewer).modelChanged(event);
		InOrder order = inOrder(viewer);
		order.verify(viewer).refresh();
		order.verify(viewer).setSelection(new StructuredSelection(element));
	}

	@Test
	public void headerRefreshSelectsLastTreeRoot() throws Exception {
		TreeViewer viewer = mock(TreeViewer.class);
		Tree tree = mock(Tree.class);
		TreeItem item = mock(TreeItem.class);
		Object element = new Object();
		when(viewer.getTree()).thenReturn(tree);
		when(tree.getItemCount()).thenReturn(2);
		when(tree.getItem(1)).thenReturn(item);
		when(item.getData()).thenReturn(element);
		IModelChangedEvent event = event(IModelChangedEvent.CHANGE, mock(ExportPackageObject.class));
		when(event.getChangedProperty()).thenReturn(Constants.EXPORT_PACKAGE);
		subject(viewer).modelChanged(event);
		InOrder order = inOrder(viewer);
		order.verify(viewer).refresh();
		order.verify(viewer).setSelection(new StructuredSelection(element));
	}

	@Test
	public void usesHierarchyRetainsNamesParentAndRemoval() throws Exception {
		ExportPackageObject element = mock(ExportPackageObject.class);
		when(element.getDirectives(Constants.USES_DIRECTIVE)).thenReturn(new String[] { " alpha, , beta ", "gamma" });
		ExportPackageSection section = subject(mock(TreeViewer.class));
		Class<?> contentType = Class.forName(ExportPackageSection.class.getName() + "$ExportPackageContentProvider");
		Constructor<?> constructor = contentType.getDeclaredConstructor(ExportPackageSection.class);
		constructor.setAccessible(true);
		ITreeContentProvider content = (ITreeContentProvider) constructor.newInstance(section);
		Object[] children = content.getChildren(element);
		assertEquals(3, children.length);
		assertArrayEquals(new String[] { "alpha", "beta", "gamma" },
				new String[] { children[0].toString(), children[1].toString(), children[2].toString() });
		assertTrue(content.hasChildren(element));
		assertSame(element, content.getParent(children[1]));
		Method remove = ExportPackageSection.class.getDeclaredMethod("removeUsesEntry", children[1].getClass());
		remove.setAccessible(true);
		remove.invoke(section, children[1]);
		verify(element).setUsesDirective("alpha,gamma");
	}

	@Test
	public void treeUpdateExpandsUsesChildren() throws Exception {
		TreeViewer viewer = mock(TreeViewer.class);
		ExportPackageObject element = mock(ExportPackageObject.class);
		when(element.getDirectives(Constants.USES_DIRECTIVE)).thenReturn(new String[] { "alpha" });
		subject(viewer).modelChanged(event(IModelChangedEvent.CHANGE, element));
		InOrder order = inOrder(viewer);
		order.verify(viewer).refresh(element);
		order.verify(viewer).expandToLevel(element, 1);
	}

	private static ExportPackageSection subject(StructuredViewer viewer) throws Exception {
		ExportPackageSection section = mock(ExportPackageSection.class, CALLS_REAL_METHODS);
		doReturn(Constants.EXPORT_PACKAGE).when(section).getExportedPackageHeader();
		Field field = ExportPackageSection.class.getDeclaredField("fPackageViewer");
		field.setAccessible(true);
		field.set(section, viewer);
		return section;
	}

	private static IModelChangedEvent event(int type, ExportPackageObject element) {
		IModelChangedEvent event = mock(IModelChangedEvent.class);
		when(event.getChangeType()).thenReturn(type);
		when(event.getChangedProperty()).thenReturn("unrelated");
		when(event.getChangedObjects()).thenReturn(new Object[] { element });
		return event;
	}
}
