/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.inferencing;

public class ConstructorInferencingTests extends InferencingTestsBase
{
	/**
	 * testNewArray
	 */
	public void testNewArray()
	{
		String source = "var x = new Array(); x";

		this.lastStatementTypeTests(source, "Array");
	}

	/**
	 * testNewBoolean
	 */
	public void testNewBoolean()
	{
		String source = "var x = new Boolean(); x";

		this.lastStatementTypeTests(source, "Boolean");
	}

	/**
	 * testNewDate
	 */
	public void testNewDate()
	{
		String source = "var x = new Date(); x";

		this.lastStatementTypeTests(source, "Date");
	}

	/**
	 * testNewFunction
	 */
	public void testNewFunction()
	{
		String source = "var x = new Function(); x";

		this.lastStatementTypeTests(source, "Function");
	}

	/**
	 * testNewObject
	 */
	public void testNewObject()
	{
		String source = "var x = new Object(); x";

		this.lastStatementTypeTests(source, "Object");
	}

	/**
	 * testNewNumber
	 */
	public void testNewNumber()
	{
		String source = "var x = new Number(); x";

		this.lastStatementTypeTests(source, "Number");
	}

	/**
	 * testNewString
	 */
	public void testNewString()
	{
		String source = "var x = new String(); x";

		this.lastStatementTypeTests(source, "String");
	}

	/**
	 * testNewRegeExp
	 */
	public void testNewRegExp()
	{
		String source = "var x = new RegExp(); x";

		this.lastStatementTypeTests(source, "RegExp");
	}
}
