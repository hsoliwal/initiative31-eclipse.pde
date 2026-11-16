/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.dialogs.BundleClassDialog;
import org.eclipse.e4.tools.emf.ui.internal.common.resourcelocator.dialogs.IconDialog;
import org.junit.jupiter.api.Test;

public class UriDialogCompatibilityTest {

	private static class BundleDialog extends BundleClassDialog {
		BundleDialog() { super(null, null); }
		void setParts(String bundle, String qualifier, String name) {
			setBundle(bundle);
			setPackage(qualifier);
			setClazz(name);
		}
		String[] getParts() { return new String[] { getBundle(), getPackage(), getClazz() }; }
		void setIgnore(boolean value) { ignoreModify = value; }
		boolean isIgnoring() { return ignoreModify; }
	}

	private static class ImageDialog extends IconDialog {
		ImageDialog() { super(null, null); }
		void setParts(String bundle, String qualifier, String name) {
			setBundle(bundle);
			setPath(qualifier);
			setIcon(name);
		}
		String[] getParts() { return new String[] { getBundle(), getPath(), getIcon() }; }
		void setIgnore(boolean value) { ignoreModify = value; }
		boolean isIgnoring() { return ignoreModify; }
	}

	@Test
	public void bundleUriFieldAndAccessorsUseTheSameState() {
		BundleDialog dialog = new BundleDialog();
		dialog.uri = "bundleclass://one/p.Name";
		assertEquals(dialog.uri, dialog.getUri());
		dialog.setUri("bundleclass://two/q.Other");
		assertEquals("bundleclass://two/q.Other", dialog.uri);
		assertArrayEquals(new String[] { "two", "q", "Other" }, dialog.getParts());
		dialog.uri = null;
		assertNull(dialog.getUri());
		dialog.setUri(null);
		assertEquals("", dialog.uri);
	}

	@Test
	public void iconUriFieldAndAccessorsUseTheSameState() {
		ImageDialog dialog = new ImageDialog();
		dialog.uri = "platform:/plugin/one/icons/a.png";
		assertEquals(dialog.uri, dialog.getUri());
		dialog.setUri("platform:/plugin/two/images/b.png");
		assertEquals("platform:/plugin/two/images/b.png", dialog.uri);
		assertArrayEquals(new String[] { "two", "images", "b.png" }, dialog.getParts());
		dialog.uri = null;
		assertNull(dialog.getUri());
		dialog.setUri(null);
		assertEquals("", dialog.uri);
	}

	@Test
	public void bundleProtectedAliasesAndModifyFlagRemainAvailable() {
		BundleDialog dialog = new BundleDialog();
		dialog.setParts("one", "p", "Name");
		assertArrayEquals(new String[] { "one", "p", "Name" }, dialog.getParts());
		dialog.setParts(null, null, null);
		assertArrayEquals(new String[] { null, null, null }, dialog.getParts());
		dialog.setIgnore(true);
		assertTrue(dialog.isIgnoring());
		dialog.setIgnore(false);
		assertFalse(dialog.isIgnoring());
	}

	@Test
	public void iconProtectedAliasesAndModifyFlagRemainAvailable() {
		ImageDialog dialog = new ImageDialog();
		dialog.setParts("one", "icons", "a.png");
		assertArrayEquals(new String[] { "one", "icons", "a.png" }, dialog.getParts());
		dialog.setParts(null, null, null);
		assertArrayEquals(new String[] { null, null, null }, dialog.getParts());
		dialog.setIgnore(true);
		assertTrue(dialog.isIgnoring());
		dialog.setIgnore(false);
		assertFalse(dialog.isIgnoring());
	}

	@Test
	public void existingBundleGetterOverridesRemainLegal() {
		BundleDialog dialog = new BundleDialog() {
			@Override protected String getBundle() { return "override:" + super.getBundle(); }
			@Override protected String getPackage() { return "legacy:" + super.getPackage(); }
		};
		dialog.setUri("bundleclass://one/p.Name");
		assertArrayEquals(new String[] { "override:one", "legacy:p", "Name" }, dialog.getParts());
	}

	@Test
	public void existingIconGetterOverridesRemainLegal() {
		ImageDialog dialog = new ImageDialog() {
			@Override protected String getBundle() { return "override:" + super.getBundle(); }
			@Override protected String getPath() { return "legacy:" + super.getPath(); }
		};
		dialog.setUri("platform:/plugin/one/icons/a.png");
		assertArrayEquals(new String[] { "override:one", "legacy:icons", "a.png" }, dialog.getParts());
	}
}
