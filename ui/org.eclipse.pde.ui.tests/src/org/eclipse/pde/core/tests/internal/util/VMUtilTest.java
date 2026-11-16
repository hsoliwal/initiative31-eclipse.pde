/*******************************************************************************
 * Copyright (c) 2026 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.junit.Test;

public class VMUtilTest {

	@Test
	public void testParseJavaVersion() {
		assertEquals(3, VMUtil.parseJavaRelease("1.3"));
		assertEquals(4, VMUtil.parseJavaRelease("1.4.1"));
		assertEquals(8, VMUtil.parseJavaRelease("1.8.0_502"));

		assertEquals(9, VMUtil.parseJavaRelease("9.0.1"));
		assertEquals(25, VMUtil.parseJavaRelease("25.1"));
		assertEquals(26, VMUtil.parseJavaRelease("26"));
	}

	@Test
	public void testGetJavaTargetVersionRetainsLegacyDoubleValues() {
		assertEquals(1.8, VMUtil.getJavaTargetVersion(environment("1.8")), 0.0);
		assertEquals(17.0, VMUtil.getJavaTargetVersion(environment("17")), 0.0);
		assertEquals(25.1, VMUtil.getJavaTargetVersion(environment("25.1")), 0.0);
		assertEquals(8, VMUtil.parseJavaRelease("1.8"));
		assertEquals(25, VMUtil.parseJavaRelease("25.1"));
	}

	@Test
	public void testGetJavaTargetVersionRetainsMissingProfileFallback() {
		assertEquals(0.0, VMUtil.getJavaTargetVersion(mock(IExecutionEnvironment.class)), 0.0);
		assertEquals(0.0, VMUtil.getJavaTargetVersion(environment(null)), 0.0);
		assertEquals(0.0, VMUtil.getJavaTargetVersion(environment(Integer.valueOf(21))), 0.0);
	}

	@Test
	public void testGetJavaTargetVersionRetainsMalformedVersionException() {
		assertThrows(NumberFormatException.class, () -> VMUtil.getJavaTargetVersion(environment("unknown")));
		assertThrows(NumberFormatException.class, () -> VMUtil.getJavaTargetVersion(environment("25.0.1")));
		assertEquals(-1, VMUtil.parseJavaRelease("unknown"));
		assertEquals(25, VMUtil.parseJavaRelease("25.0.1"));
	}

	private static IExecutionEnvironment environment(Object target) {
		final Properties properties = new Properties();
		if (target != null) {
			properties.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, target);
		}
		final IExecutionEnvironment environment = mock(IExecutionEnvironment.class);
		when(environment.getProfileProperties()).thenReturn(properties);
		return environment;
	}

}
