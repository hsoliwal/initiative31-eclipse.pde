/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.pde.internal.ui.editor.m3.M3ModelChangedDeltas;
import org.junit.jupiter.api.Test;

/**
 * The pde-model adapter binds the M3 JFace viewer delta only when the JFace in
 * use has it; on a stock JFace its differential must report that no delta was
 * compared instead of passing.
 */
public class M3ModelChangedDeltasAvailabilityTest {

	private static final String DELTA_TYPE = "org.eclipse.jface.viewers.m3.M3ViewerDelta"; //$NON-NLS-1$
	private static final String VIEWERS_TYPE = "org.eclipse.jface.viewers.m3.M3Viewers"; //$NON-NLS-1$

	@Test
	public void bindingMatchesTheJFaceInUse() {
		assertEquals(loadable(DELTA_TYPE) && loadable(VIEWERS_TYPE), M3ModelChangedDeltas.viewerDeltaAvailable());
	}

	@Test
	public void differentialReportsUnavailableOnStockJFace() throws IOException {
		assumeFalse(M3ModelChangedDeltas.viewerDeltaAvailable(), "the M3 JFace viewer delta is bound"); //$NON-NLS-1$
		Path report = Files.createTempFile("m3-differential", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			int exit = M3ModelChangedDeltasDifferential.execute(new String[] { report.toString(), "1", "5" }); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals(M3ModelChangedDeltasDifferential.UNAVAILABLE, exit);
			assertTrue(Files.readString(report, StandardCharsets.UTF_8).startsWith("UNAVAILABLE\t")); //$NON-NLS-1$
		} finally {
			Files.deleteIfExists(report);
		}
	}

	private static boolean loadable(String name) {
		try {
			Class.forName(name, false, StructuredViewer.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
