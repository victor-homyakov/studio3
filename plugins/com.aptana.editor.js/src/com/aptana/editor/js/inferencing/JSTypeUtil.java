/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.inferencing;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.FileUtil;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.js.JSPlugin;
import com.aptana.editor.js.JSTypeConstants;
import com.aptana.editor.js.contentassist.model.ClassElement;
import com.aptana.editor.js.contentassist.model.FunctionElement;
import com.aptana.editor.js.contentassist.model.ParameterElement;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.contentassist.model.ReturnTypeElement;
import com.aptana.editor.js.contentassist.model.TypeElement;
import com.aptana.editor.js.parsing.ast.IJSNodeTypes;
import com.aptana.editor.js.parsing.ast.JSAssignmentNode;
import com.aptana.editor.js.parsing.ast.JSDeclarationNode;
import com.aptana.editor.js.parsing.ast.JSFunctionNode;
import com.aptana.editor.js.parsing.ast.JSIdentifierNode;
import com.aptana.editor.js.parsing.ast.JSNameValuePairNode;
import com.aptana.editor.js.parsing.ast.JSNode;
import com.aptana.editor.js.sdoc.model.DocumentationBlock;
import com.aptana.editor.js.sdoc.model.ExampleTag;
import com.aptana.editor.js.sdoc.model.ParamTag;
import com.aptana.editor.js.sdoc.model.ReturnTag;
import com.aptana.editor.js.sdoc.model.Tag;
import com.aptana.editor.js.sdoc.model.TagType;
import com.aptana.editor.js.sdoc.model.Type;
import com.aptana.editor.js.sdoc.model.TypeTag;
import com.aptana.parsing.ast.IParseNode;

public class JSTypeUtil
{
	private static final Set<String> FILTERED_TYPES;

	/**
	 * static initializer
	 */
	static
	{
		// @formatter:off
		FILTERED_TYPES = CollectionsUtil.newSet(
			JSTypeConstants.ARRAY_TYPE,
			JSTypeConstants.BOOLEAN_TYPE,
			JSTypeConstants.FUNCTION_TYPE,
			JSTypeConstants.NUMBER_TYPE,
			JSTypeConstants.OBJECT_TYPE,
			JSTypeConstants.REG_EXP_TYPE,
			JSTypeConstants.STRING_TYPE,
			JSTypeConstants.UNDEFINED_TYPE,
			JSTypeConstants.VOID_TYPE,
			JSTypeConstants.WINDOW_TYPE,
			JSTypeConstants.WINDOW_PROPERTY
		);
		// @formatter:on
	}

	/**
	 * applyDocumentation
	 * 
	 * @param function
	 * @param block
	 */
	public static void applyDocumentation(FunctionElement function, JSNode node, DocumentationBlock block)
	{
		if (block != null)
		{
			// apply description
			function.setDescription(block.getText());

			// apply parameters
			if (block.hasTag(TagType.PARAM))
			{
				for (Tag tag : block.getTags(TagType.PARAM))
				{
					ParamTag paramTag = (ParamTag) tag;
					ParameterElement parameter = new ParameterElement();

					parameter.setName(paramTag.getName());
					parameter.setDescription(paramTag.getText());
					parameter.setUsage(paramTag.getUsage().getName());

					for (Type type : paramTag.getTypes())
					{
						parameter.addType(type.toSource());
					}

					function.addParameter(parameter);
				}
			}
			else
			{
				if (node instanceof JSFunctionNode)
				{
					JSFunctionNode functionNode = (JSFunctionNode) node;

					for (IParseNode parameterNode : functionNode.getParameters())
					{
						ParameterElement parameterElement = new ParameterElement();

						parameterElement.setName(parameterNode.getText());
						parameterElement.addType(JSTypeConstants.OBJECT_TYPE);

						function.addParameter(parameterElement);
					}
				}
				else
				{
					// @formatter:off
					String message = MessageFormat.format(
						"Expected JSFunction node when applying documentation; however, the node type was ''{0}'' instead. Source ={1}{2}",
						(node != null) ? node.getClass().getName() : "null",
						FileUtil.NEW_LINE,
						node
					);
					// @formatter:on

					IdeLog.logError(JSPlugin.getDefault(), message);
				}
			}

			// apply return types
			for (Tag tag : block.getTags(TagType.RETURN))
			{
				ReturnTag returnTag = (ReturnTag) tag;

				for (Type type : returnTag.getTypes())
				{
					ReturnTypeElement returnType = new ReturnTypeElement();

					returnType.setType(type.toSource());
					returnType.setDescription(returnTag.getText());

					function.addReturnType(returnType);
				}
			}

			// apply examples
			for (Tag tag : block.getTags(TagType.EXAMPLE))
			{
				ExampleTag exampleTag = (ExampleTag) tag;

				function.addExample(exampleTag.getText());
			}
		}
	}

	/**
	 * applyDocumentation
	 * 
	 * @param property
	 * @param block
	 */
	public static void applyDocumentation(PropertyElement property, JSNode node, DocumentationBlock block)
	{
		if (property instanceof FunctionElement)
		{
			applyDocumentation((FunctionElement) property, node, block);
		}
		else
		{
			if (block != null)
			{
				// apply description
				property.setDescription(block.getText());

				// apply types
				for (Tag tag : block.getTags(TagType.TYPE))
				{
					TypeTag typeTag = (TypeTag) tag;

					for (Type type : typeTag.getTypes())
					{
						ReturnTypeElement returnType = new ReturnTypeElement();

						returnType.setType(type.toSource());
						returnType.setDescription(typeTag.getText());

						property.addType(returnType);
					}
				}

				// apply examples
				for (Tag tag : block.getTags(TagType.EXAMPLE))
				{
					ExampleTag exampleTag = (ExampleTag) tag;

					property.addExample(exampleTag.getText());
				}
			}
		}
	}

	/**
	 * applySignature
	 * 
	 * @param property
	 * @param typeName
	 */
	public static void applySignature(PropertyElement property, String typeName)
	{
		if (property instanceof FunctionElement)
		{
			applySignature((FunctionElement) property, typeName);
		}
		else
		{
			property.addType(typeName);
		}
	}

	/**
	 * applySignature
	 * 
	 * @param function
	 * @param typeName
	 */
	public static void applySignature(FunctionElement function, String typeName)
	{
		if (function != null && typeName != null)
		{
			int delimiter = typeName.indexOf(JSTypeConstants.FUNCTION_SIGNATURE_DELIMITER);

			if (delimiter != -1)
			{
				for (String returnType : typeName.substring(delimiter + 1).split(JSTypeConstants.RETURN_TYPE_DELIMITER))
				{
					function.addReturnType(returnType);
				}

				// chop off the signature to continue processing the type
				typeName = typeName.substring(0, delimiter); // $codepro.audit.disable questionableAssignment
			}

			function.addType(typeName);
		}
	}

	/**
	 * createGenericArrayType
	 * 
	 * @param elementType
	 * @return
	 */
	public static String createGenericArrayType(String elementType)
	{
		return JSTypeConstants.GENERIC_ARRAY_OPEN + elementType + JSTypeConstants.GENERIC_CLOSE;
	}

	/**
	 * getArrayElementType
	 * 
	 * @param type
	 * @return
	 */
	public static String getArrayElementType(String type)
	{
		String result = null;

		if (type != null && type.length() > 0)
		{
			if (type.endsWith(JSTypeConstants.ARRAY_LITERAL))
			{
				result = type.substring(0, type.length() - 2);
			}
			else if (type.startsWith(JSTypeConstants.GENERIC_ARRAY_OPEN)
					&& type.endsWith(JSTypeConstants.GENERIC_CLOSE))
			{
				result = type.substring(JSTypeConstants.GENERIC_ARRAY_OPEN.length(), type.length() - 1);
			}
			else if (type.equals(JSTypeConstants.ARRAY_TYPE))
			{
				result = JSTypeConstants.OBJECT_TYPE;
			}
		}

		return result;
	}

	/**
	 * getClassType
	 * 
	 * @param typeName
	 * @return
	 */
	public static String getClassType(String typeName)
	{
		String result = null;

		if (isClassType(typeName))
		{
			result = typeName.substring(JSTypeConstants.GENERIC_CLASS_OPEN.length(), typeName.length()
					- JSTypeConstants.GENERIC_CLOSE.length());
		}

		return result;
	}

	/**
	 * getFunctionSignatureReturnTypeNames
	 * 
	 * @param typeName
	 * @return
	 */
	public static List<String> getFunctionSignatureReturnTypeNames(String typeName)
	{
		List<String> result;

		int index = typeName.indexOf(JSTypeConstants.FUNCTION_SIGNATURE_DELIMITER);

		if (index != -1)
		{
			String returnTypesString = typeName.substring(index + 1);

			result = new ArrayList<String>();

			for (String returnType : returnTypesString.split(JSTypeConstants.RETURN_TYPE_DELIMITER))
			{
				result.add(returnType);
			}
		}
		else
		{
			result = Collections.emptyList();
		}

		return result;
	}

	/**
	 * getFunctionSignatureType
	 * 
	 * @param typeName
	 * @return
	 */
	public static String getFunctionSignatureType(String typeName)
	{
		String result = typeName;

		if (typeName != null)
		{
			int delimiter = typeName.indexOf(JSTypeConstants.FUNCTION_SIGNATURE_DELIMITER);

			if (delimiter != -1)
			{
				// chop off the signature to continue processing the type
				result = typeName.substring(0, delimiter);
			}
		}

		return result;
	}

	/**
	 * getFunctionType
	 * 
	 * @param typeName
	 * @return
	 */
	public static String getFunctionType(String typeName)
	{
		String result = typeName;

		if (typeName != null && typeName.startsWith(JSTypeConstants.GENERIC_FUNCTION_OPEN))
		{
			int startingIndex = JSTypeConstants.GENERIC_FUNCTION_OPEN.length();
			int endingIndex = typeName.indexOf(JSTypeConstants.GENERIC_CLOSE, startingIndex);

			if (endingIndex != -1)
			{
				result = typeName.substring(startingIndex, endingIndex);
			}
		}

		return result;
	}

	/**
	 * getName
	 * 
	 * @param node
	 * @return
	 */
	public static String getName(JSNode node)
	{
		String result = null;

		if (node != null)
		{
			List<String> parts = new ArrayList<String>();
			JSNode current = node;

			while (current != null)
			{
				switch (current.getNodeType())
				{
					case IJSNodeTypes.IDENTIFIER:
						parts.add(current.getText());
						break;

					case IJSNodeTypes.FUNCTION:
						JSFunctionNode function = (JSFunctionNode) current;
						IParseNode functionName = function.getName();

						if (!functionName.isEmpty())
						{
							parts.add(functionName.getText());
						}
						break;

					case IJSNodeTypes.NAME_VALUE_PAIR:
						JSNameValuePairNode entry = (JSNameValuePairNode) current;
						IParseNode entryName = entry.getName();
						String name = entryName.getText();

						if (entryName.getNodeType() == IJSNodeTypes.STRING)
						{
							name = name.substring(1, name.length() - 1);
						}

						parts.add(name);
						break;

					case IJSNodeTypes.DECLARATION:
						JSDeclarationNode declaration = (JSDeclarationNode) current;
						IParseNode declarationName = declaration.getIdentifier();

						parts.add(declarationName.getText());
						break;

					case IJSNodeTypes.ASSIGN:
						JSAssignmentNode assignment = (JSAssignmentNode) current;
						IParseNode lhs = assignment.getLeftHandSide();

						if (lhs instanceof JSIdentifierNode)
						{
							parts.add(lhs.getText());
						}
						break;

					default:
						break;
				}

				IParseNode parent = current.getParent();

				current = (parent instanceof JSNode) ? (JSNode) parent : null;
			}

			if (parts.size() > 0)
			{
				Collections.reverse(parts);

				result = StringUtil.join(".", parts); //$NON-NLS-1$
			}
		}

		// Don't allow certain names to avoid confusion and to prevent overwriting
		// of core types
		if (FILTERED_TYPES.contains(result))
		{
			result = null;
		}

		return result;
	}

	/**
	 * getUniqueTypeName
	 * 
	 * @return
	 */
	public static String getUniqueTypeName()
	{
		return JSTypeConstants.DYNAMIC_CLASS_PREFIX + UUID.randomUUID();
	}

	/**
	 * isClassType
	 * 
	 * @param typeName
	 * @return
	 */
	public static boolean isClassType(String typeName)
	{
		return typeName != null && typeName.startsWith(JSTypeConstants.GENERIC_CLASS_OPEN)
				&& typeName.endsWith(JSTypeConstants.GENERIC_CLOSE);
	}

	/**
	 * isFunctionPrefix
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isFunctionPrefix(String type)
	{
		boolean result = false;

		if (type != null)
		{
			Matcher m = JSTypeConstants.FUNCTION_PREFIX.matcher(type);

			result = m.find();
		}

		return result;
	}

	/**
	 * toFunctionType
	 * 
	 * @param type
	 * @return
	 */
	public static String toFunctionType(String type)
	{
		String result = type;

		if (!isFunctionPrefix(type))
		{
			result = JSTypeConstants.GENERIC_FUNCTION_OPEN + type + JSTypeConstants.GENERIC_CLOSE;
		}

		return result;
	}

	/**
	 * Given a list of type elements, return a list of class elements. We divide class members and instance members into
	 * Class\<Type\> and Type, respectively. A ClassElement recombines these separate items into a single item with
	 * members tagged appropriately.
	 * 
	 * @param types
	 * @return
	 */
	public static List<ClassElement> typesToClasses(List<TypeElement> types)
	{
		List<ClassElement> classes = new ArrayList<ClassElement>();

		if (types != null)
		{
			Map<String, ClassElement> classesByName = new HashMap<String, ClassElement>();

			for (TypeElement type : types)
			{
				String typeName = type.getName();
				boolean isClassType = isClassType(typeName);
				String baseName = isClassType ? getClassType(type.getName()) : typeName;

				if (!classesByName.containsKey(baseName))
				{
					ClassElement clss = new ClassElement();

					clss.setName(baseName);

					classesByName.put(baseName, clss);
				}

				ClassElement clss = classesByName.get(baseName);

				if (isClassType)
				{
					clss.addClassType(type);
				}
				else
				{
					clss.addInstanceType(type);
				}
			}

			classes = new ArrayList<ClassElement>(classesByName.values());
		}

		return classes;
	}

	/**
	 * JSContentAssistUtil
	 */
	private JSTypeUtil()
	{
	}
}
