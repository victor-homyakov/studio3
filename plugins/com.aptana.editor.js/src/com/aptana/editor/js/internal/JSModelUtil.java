/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.internal;

import java.util.Collections;
import java.util.List;

import com.aptana.core.IFilter;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.util.EditorUtil;
import com.aptana.editor.js.contentassist.JSIndexQueryHelper;
import com.aptana.editor.js.contentassist.JSLocationIdentifier;
import com.aptana.editor.js.contentassist.ParseUtil;
import com.aptana.editor.js.contentassist.model.FunctionElement;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.parsing.ast.JSGetPropertyNode;
import com.aptana.index.core.Index;
import com.aptana.parsing.ast.IParseNode;

public class JSModelUtil
{

	private JSModelUtil()
	{
	}

	public static List<PropertyElement> getProperties(AbstractThemeableEditor editor, IParseNode node)
	{
		// We add one because for var assignments, JSLI decrements by one, pushing us before the var name and returning
		// no results.
		JSLocationIdentifier identifier = new JSLocationIdentifier(node.getStartingOffset() + 1, node);
		switch (identifier.getType())
		{
			case IN_CONSTRUCTOR:
			case IN_GLOBAL:
			case IN_VARIABLE_DECLARATION:
			case IN_VARIABLE_NAME:
			{
				JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();
				Index index = EditorUtil.getIndex(editor);
				return queryHelper.getGlobals(index, node.getText());
			}

			case IN_PROPERTY_NAME:
				JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();
				Index index = EditorUtil.getIndex(editor);
				JSGetPropertyNode propertyNode = ParseUtil.getGetPropertyNode(identifier.getTargetNode(),
						identifier.getStatementNode());

				List<String> types = ParseUtil.getParentObjectTypes(index, EditorUtil.getURI(editor),
						identifier.getTargetNode(), propertyNode, node.getStartingOffset());
				String typeName = null;
				String methodName = null;

				if (!CollectionsUtil.isEmpty(types))
				{
					typeName = types.get(0);
					methodName = propertyNode.getLastChild().getText();
				}

				if (typeName != null && methodName != null)
				{
					List<PropertyElement> properties = queryHelper.getTypeMembers(index, typeName, methodName);
					// filter to only functions
					return CollectionsUtil.filter(properties, new IFilter<PropertyElement>()
					{
						public boolean include(PropertyElement item)
						{
							return (item instanceof FunctionElement);
						}
					});
				}
				break;

			case IN_OBJECT_LITERAL_PROPERTY:
			case IN_PARAMETERS:
			case IN_LABEL:
			case UNKNOWN:
			case NONE:
			default:
				break;
		}

		return Collections.emptyList();
	}
}
