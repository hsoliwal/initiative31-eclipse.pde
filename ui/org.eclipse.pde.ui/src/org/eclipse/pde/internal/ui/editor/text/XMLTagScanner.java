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
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.SyntaxThemeConstants;

public class XMLTagScanner extends BasePDEScanner {

	private Token fTagToken;
	private Token fAttributeNameToken;
	private Token fStringToken;
	private Token fExternalizedStringToken;
	private Token fCommentToken;

	// The manager constructor calls initialize() from BasePDEScanner.
	// Keep token fields free of initializers so those tokens survive.
	private boolean useThemeColors;

	public XMLTagScanner() {
		useThemeColors = true;
		fTagToken = new Token(null);
		fAttributeNameToken = new Token(null);
		fStringToken = new Token(null);
		fExternalizedStringToken = new Token(null);
		fCommentToken = new Token(null);
		IRule[] rules = new IRule[7];
		rules[0] = new SingleLineRule("\"%", "\"", fExternalizedStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new SingleLineRule("'%", "'", fExternalizedStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		// Add rule for single and double quotes
		rules[2] = new MultiLineRule("\"", "\"", fStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[3] = new SingleLineRule("'", "'", fStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[4] = new MultiLineRule("<!--", "-->", fCommentToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[5] = new AttributeNameRule(fAttributeNameToken);
		// Add generic whitespace rule.
		rules[6] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
		updateColors();
		setDefaultReturnToken(fTagToken);
	}

	public XMLTagScanner(IColorManager manager) {
		super(manager);
	}

	@Override
	protected void initialize() {
		fStringToken = new Token(createTextAttribute(IPDEColorConstants.P_STRING));
		fExternalizedStringToken = new Token(createTextAttribute(IPDEColorConstants.P_EXTERNALIZED_STRING));
		fCommentToken = new Token(createTextAttribute(IPDEColorConstants.P_XML_COMMENT));
		IRule[] rules = new IRule[6];
		rules[0] = new SingleLineRule("\"%", "\"", fExternalizedStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new SingleLineRule("'%", "'", fExternalizedStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		// Add rule for single and double quotes
		rules[2] = new MultiLineRule("\"", "\"", fStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[3] = new SingleLineRule("'", "'", fStringToken); //$NON-NLS-1$ //$NON-NLS-2$
		rules[4] = new MultiLineRule("<!--", "-->", fCommentToken); //$NON-NLS-1$ //$NON-NLS-2$
		// Add generic whitespace rule.
		rules[5] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
		setDefaultReturnToken(new Token(createTextAttribute(IPDEColorConstants.P_TAG)));
	}

	@Override
	protected Token getTokenAffected(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property.startsWith(IPDEColorConstants.P_STRING)) {
			return fStringToken;
		} else if (property.startsWith(IPDEColorConstants.P_EXTERNALIZED_STRING)) {
			return fExternalizedStringToken;
		} else 	if (event.getProperty().startsWith(IPDEColorConstants.P_XML_COMMENT)) {
			return fCommentToken;
		}
		return (Token) fDefaultReturnToken;
	}

	@Override
	public boolean affectsTextPresentation(String property) {
		return !useThemeColors && (property.startsWith(IPDEColorConstants.P_TAG) || property.startsWith(IPDEColorConstants.P_STRING)
				|| property.startsWith(IPDEColorConstants.P_EXTERNALIZED_STRING)
				|| property.startsWith(IPDEColorConstants.P_XML_COMMENT));
	}

	/** Refreshes theme colours; manager-backed scanners use preference events. */
	public void updateColors() {
		if (!useThemeColors) {
			return;
		}
		fTagToken.setData(new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.TAG_COLOR)));
		fAttributeNameToken.setData(new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.ATTRIBUTE_NAME_COLOR)));
		fStringToken.setData(new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.STRING_COLOR)));
		fExternalizedStringToken.setData(new TextAttribute(XMLSyntaxColors.get(XMLSyntaxColors.EXTERNALIZED_STRING_COLOR)));
		fCommentToken.setData(new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.COMMENT_COLOR)));
	}

	/**
	 * Matches a name that is followed by {@code =}, so the tag name itself keeps
	 * the tag color.
	 */
	private static class AttributeNameRule implements IRule {

		private final IToken token;

		AttributeNameRule(IToken token) {
			this.token = token;
		}

		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			int read = 0;
			int c = scanner.read();
			read++;
			while (isNameChar(c)) {
				c = scanner.read();
				read++;
			}
			int nameLength = read - 1;
			while (c != ICharacterScanner.EOF && Character.isWhitespace(c)) {
				c = scanner.read();
				read++;
			}
			if (nameLength > 0 && c == '=') {
				// keep only the name in the token
				for (int i = nameLength; i < read; i++) {
					scanner.unread();
				}
				return token;
			}
			for (int i = 0; i < read; i++) {
				scanner.unread();
			}
			return Token.UNDEFINED;
		}

		private static boolean isNameChar(int c) {
			return c != ICharacterScanner.EOF && (Character.isLetterOrDigit(c) || c == ':' || c == '_' || c == '-' || c == '.');
		}
	}

}
