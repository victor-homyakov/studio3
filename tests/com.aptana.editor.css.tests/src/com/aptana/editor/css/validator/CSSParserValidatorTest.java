/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.css.validator;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import com.aptana.core.build.IBuildParticipant;
import com.aptana.core.build.IProblem;
import com.aptana.editor.common.validation.AbstractValidatorTestCase;
import com.aptana.editor.css.CSSPlugin;
import com.aptana.editor.css.ICSSConstants;
import com.aptana.parsing.ParseState;

public class CSSParserValidatorTest extends AbstractValidatorTestCase
{

	@Override
	protected IBuildParticipant createValidator()
	{
		return new CSSParserValidator()
		{
			@Override
			protected String getPreferenceNode()
			{
				return CSSPlugin.PLUGIN_ID;
			}
		};
	}

	@Override
	protected String getFileExtension()
	{
		return "css";
	}

	protected List<IProblem> getParseErrors(String source) throws CoreException
	{
		return getParseErrors(source, new ParseState(source), ICSSConstants.CSS_PROBLEM);
	}

	public void testCSSParseErrors() throws CoreException
	{
		String text = "div#paginator {\nfloat: left\nwidth: 65px\n}";

		List<IProblem> items = getParseErrors(text);
		assertEquals(1, items.size());

		IProblem item = items.get(0);

		assertEquals("Error was not found on expected line", 3, item.getLineNumber());
		assertEquals("Error message did not match expected error message", "Syntax Error: unexpected token \":\"",
				item.getMessage());
	}
}
