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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.m3.M3ViewSpec;

/**
 * Parses every {@code .m3view} spec under the given source roots with the view
 * template's parser and prints one line per spec: its view id, adapter and
 * lanes, or the parse error. Exit code 1 when any spec is invalid.
 * <p>
 * Usage: {@code M3SectionSpecsCheck <source-root>...}
 * </p>
 */
public final class M3SectionSpecsCheck {

	private M3SectionSpecsCheck() {
	}

	public static void main(String[] args) throws IOException {
		int bad = 0;
		int good = 0;
		for (String root : args) {
			List<Path> specs;
			try (Stream<Path> walk = Files.walk(Path.of(root))) {
				specs = walk.filter(p -> p.toString().endsWith(".m3view")).sorted().toList(); //$NON-NLS-1$
			}
			for (Path spec : specs) {
				try {
					M3ViewSpec s = M3ViewSpec.parse(Files.readString(spec, StandardCharsets.UTF_8));
					good++;
					System.out.println("OK\t" + s.viewId() + "\t" + s.source() + "\t" + s.lanes().size() + " lanes"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				} catch (IllegalArgumentException e) {
					bad++;
					System.out.println("BAD\t" + spec + "\t" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		System.out.println("specs ok " + good + " bad " + bad); //$NON-NLS-1$ //$NON-NLS-2$
		System.exit(bad == 0 ? 0 : 1);
	}
}
