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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.Viewer;

/**
 * Parses every {@code .m3view} spec under the given source roots with the view
 * template's parser and prints one line per spec: its view id, adapter and
 * lanes, or the parse error. Exit code 1 when any spec is invalid.
 * <p>
 * The parser, {@code org.eclipse.jface.viewers.m3.M3ViewSpec}, belongs to the
 * M3 JFace of hsoliwal/eclipse.platform.ui and is bound reflectively, so this
 * bundle also compiles against a stock JFace. Without it no spec can be
 * checked: the check prints why and exits with code 2.
 * </p>
 * <p>
 * Usage: {@code M3SectionSpecsCheck <source-root>...}
 * </p>
 */
public final class M3SectionSpecsCheck {

	private static final String VIEW_SPEC_TYPE = "org.eclipse.jface.viewers.m3.M3ViewSpec"; //$NON-NLS-1$

	private M3SectionSpecsCheck() {
	}

	public static void main(String[] args) throws IOException, ReflectiveOperationException {
		Class<?> specType;
		try {
			specType = Class.forName(VIEW_SPEC_TYPE, false, Viewer.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			System.out.println("UNAVAILABLE\t" + VIEW_SPEC_TYPE + " is not in this JFace; no spec was checked"); //$NON-NLS-1$ //$NON-NLS-2$
			System.exit(2);
			return;
		}
		Method parse = specType.getMethod("parse", String.class); //$NON-NLS-1$
		Method viewId = specType.getMethod("viewId"); //$NON-NLS-1$
		Method source = specType.getMethod("source"); //$NON-NLS-1$
		Method lanes = specType.getMethod("lanes"); //$NON-NLS-1$
		int bad = 0;
		int good = 0;
		for (String root : args) {
			List<Path> specs;
			try (Stream<Path> walk = Files.walk(Path.of(root))) {
				specs = walk.filter(p -> p.toString().endsWith(".m3view")).sorted().toList(); //$NON-NLS-1$
			}
			for (Path spec : specs) {
				try {
					Object s = parse.invoke(null, Files.readString(spec, StandardCharsets.UTF_8));
					good++;
					System.out.println("OK\t" + viewId.invoke(s) + "\t" + source.invoke(s) + "\t" + ((List<?>) lanes.invoke(s)).size() + " lanes"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				} catch (InvocationTargetException e) {
					if (!(e.getCause() instanceof IllegalArgumentException invalid)) {
						throw e;
					}
					bad++;
					System.out.println("BAD\t" + spec + "\t" + invalid.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		System.out.println("specs ok " + good + " bad " + bad); //$NON-NLS-1$ //$NON-NLS-2$
		System.exit(bad == 0 ? 0 : 1);
	}
}
