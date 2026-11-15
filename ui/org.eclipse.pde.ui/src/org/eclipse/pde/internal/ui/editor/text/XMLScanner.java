/*******************************************************************************
 *  Copyright (c) 2000, 2026 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.SyntaxThemeConstants;

public class XMLScanner extends BasePDEScanner {
	private Token fProcInstr;

	private Token fExternalizedString;

	// The manager constructor calls initialize() from BasePDEScanner.
	// Keep token fields free of initializers so those tokens survive.
	private boolean useThemeColors;

	public XMLScanner() {
		useThemeColors = true;
		fProcInstr = new Token(null);
		fExternalizedString = new Token(null);
		IRule[] rules = new IRule[3];
		//Add rule for processing instructions
		rules[0] = new SingleLineRule("<?", "?>", fProcInstr); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new ExternalizedStringRule(fExternalizedString);
		// Add generic whitespace rule.
		rules[2] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
		updateColors();
	}

	public XMLScanner(IColorManager manager) {
		super(manager);
	}

	@Override
	protected void initialize() {
		fProcInstr = new Token(createTextAttribute(IPDEColorConstants.P_PROC_INSTR));
		fExternalizedString = new Token(createTextAttribute(IPDEColorConstants.P_EXTERNALIZED_STRING));

		IRule[] rules = new IRule[3];
		//Add rule for processing instructions
		rules[0] = new SingleLineRule("<?", "?>", fProcInstr); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new ExternalizedStringRule(fExternalizedString);
		// Add generic whitespace rule.
		rules[2] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
		setDefaultReturnToken(new Token(createTextAttribute(IPDEColorConstants.P_DEFAULT)));
	}

	@Override
	protected Token getTokenAffected(PropertyChangeEvent event) {
		if (event.getProperty().startsWith(IPDEColorConstants.P_PROC_INSTR)) {
			return fProcInstr;
		} else if (event.getProperty().startsWith(IPDEColorConstants.P_EXTERNALIZED_STRING)) {
			return fExternalizedString;
		}
		return (Token) fDefaultReturnToken;
	}

	@Override
	public boolean affectsTextPresentation(String property) {
		return !useThemeColors && (property.startsWith(IPDEColorConstants.P_DEFAULT) || property.startsWith(IPDEColorConstants.P_PROC_INSTR) || property.startsWith(IPDEColorConstants.P_EXTERNALIZED_STRING));
	}

	/** Refreshes theme colours; manager-backed scanners use preference events. */
	public void updateColors() {
		if (!useThemeColors) {
			return;
		}
		fProcInstr.setData(new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.DIRECTIVE_COLOR)));
		fExternalizedString.setData(new TextAttribute(XMLSyntaxColors.get(XMLSyntaxColors.EXTERNALIZED_STRING_COLOR)));
	}

}
