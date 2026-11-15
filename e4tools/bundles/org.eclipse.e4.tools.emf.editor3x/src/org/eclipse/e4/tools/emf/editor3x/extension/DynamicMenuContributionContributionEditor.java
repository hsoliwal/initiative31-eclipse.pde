/*******************************************************************************
 * Copyright (c) 2013 MEDEVIT, FHV and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Marco Descher <marco@descher.at> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.tools.emf.editor3x.extension;

import org.eclipse.core.resources.IProject;
import org.eclipse.e4.internal.tools.wizards.classes.AbstractNewClassWizard;
import org.eclipse.e4.internal.tools.wizards.classes.NewDynamicMenuContributionClassWizard;
import org.eclipse.e4.ui.model.application.MContribution;
import org.eclipse.e4.ui.model.application.ui.menu.impl.MenuPackageImpl;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.swt.widgets.Shell;

@SuppressWarnings("restriction")
public class DynamicMenuContributionContributionEditor extends AbstractContributionEditor {

	/** Retains the original public force-new entry point and subclass dispatch. */
	@Override
	public void createOpen(MContribution contribution, EditingDomain domain,
			IProject project, Shell shell, boolean forceNew) {
		super.createOpen(contribution, domain, project, shell, forceNew);
	}

	@Override
	protected AbstractNewClassWizard createWizard(String contributionURI) {
		return new NewDynamicMenuContributionClassWizard(contributionURI);
	}

	@Override
	public boolean isSupported(EClass element) {
		return Util.isTypeOrSuper(MenuPackageImpl.Literals.DYNAMIC_MENU_CONTRIBUTION, element);
	}
}
