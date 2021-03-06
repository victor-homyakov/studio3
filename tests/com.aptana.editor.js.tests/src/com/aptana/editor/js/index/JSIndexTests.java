/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.index;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import com.aptana.jetty.util.epl.ajax.JSON;

import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.IOUtil;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.contentassist.UserAgentManager;
import com.aptana.editor.js.JSPlugin;
import com.aptana.editor.js.contentassist.JSIndexQueryHelper;
import com.aptana.editor.js.contentassist.index.IJSIndexConstants;
import com.aptana.editor.js.contentassist.index.JSFileIndexingParticipant;
import com.aptana.editor.js.contentassist.index.JSIndexReader;
import com.aptana.editor.js.contentassist.index.JSIndexWriter;
import com.aptana.editor.js.contentassist.model.FunctionElement;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.contentassist.model.TypeElement;
import com.aptana.editor.js.contentassist.model.UserAgentElement;
import com.aptana.index.core.Index;
import com.aptana.index.core.IndexManager;
import com.aptana.index.core.IndexPlugin;
import com.aptana.index.core.IndexReader;
import com.aptana.index.core.QueryResult;
import com.aptana.index.core.SearchPattern;
import com.aptana.index.core.build.BuildContext;
import com.aptana.parsing.ast.IParseRootNode;

public class JSIndexTests extends TestCase
{
	private class TestBuildContext extends BuildContext
	{
		private String resource;

		public TestBuildContext(String resource)
		{
			this.resource = resource;
		}

		@Override
		public synchronized String getContents()
		{
			try
			{
				return getSource(resource);
			}
			catch (IOException e)
			{
				return StringUtil.EMPTY;
			}
		}

		@Override
		public String getContentType() throws CoreException
		{
			return "com.aptana.contenttype.js";
		}

		/*
		 * (non-Javadoc)
		 * @see com.aptana.index.core.build.BuildContext#getURI()
		 */
		@Override
		public URI getURI()
		{
			return URI.create(resource);
		}
	}

	/**
	 * getIndex
	 * 
	 * @return
	 */
	protected Index getIndex()
	{
		return JSIndexQueryHelper.getIndex();
	}

	protected IndexManager getIndexManager()
	{
		return IndexPlugin.getDefault().getIndexManager();
	}

	/**
	 * getSource
	 * 
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	private String getSource(InputStream stream) throws IOException
	{
		return IOUtil.read(stream);
	}

	/**
	 * getSource
	 * 
	 * @param resourceName
	 * @return
	 * @throws IOException
	 */
	private String getSource(String resourceName) throws IOException
	{
		InputStream stream = FileLocator.openStream(Platform.getBundle(JSPlugin.PLUGIN_ID), new Path(resourceName),
				false);
		return getSource(stream);
	}

	/**
	 * getType
	 * 
	 * @param typeName
	 * @return
	 */
	private List<TypeElement> getType(String typeName)
	{
		JSIndexReader reader = new JSIndexReader();

		return reader.getType(this.getIndex(), typeName, true);
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		getIndexManager().removeIndex(URI.create(IJSIndexConstants.METADATA_INDEX_LOCATION));

		super.tearDown();
	}

	/**
	 * writeType
	 * 
	 * @param type
	 */
	private void writeType(TypeElement type)
	{
		JSIndexWriter writer = new JSIndexWriter();

		writer.writeType(this.getIndex(), type);
	}

	/**
	 * Test for APSTUD-4289. Make sure we don't allow duplicate user agents into the JS index
	 */
	public void testDuplicateUserAgents()
	{
		// create property
		PropertyElement property = new PropertyElement();
		property.setName("property");

		// add all user agents, twice
		UserAgentManager manager = UserAgentManager.getInstance();

		for (UserAgentManager.UserAgent userAgent : manager.getAllUserAgents())
		{
			UserAgentElement uaElement = new UserAgentElement();
			uaElement.setPlatform(userAgent.name);

			property.addUserAgent(uaElement);
			property.addUserAgent(uaElement);
		}

		// create type for property so we can write it to the index
		TypeElement type = new TypeElement();
		type.setName("Testing");
		type.addProperty(property);

		// write type and its properties
		JSIndexWriter writer = new JSIndexWriter();
		writer.writeType(getIndex(), type);

		// read property back again
		JSIndexReader reader = new JSIndexReader();
		List<PropertyElement> properties = reader.getProperties(getIndex(), property.getOwningType());

		// make sure we have only one of each user agent
		assertNotNull(properties);
		assertEquals(1, properties.size());
		assertEquals(manager.getAllUserAgents().length, properties.get(0).getUserAgents().size());
	}

	/**
	 * testMethod
	 */
	public void testMethod()
	{
		String typeName = "MyClass";
		String methodName = "myMethod";

		// create type
		TypeElement type = new TypeElement();
		type.setName(typeName);

		// create method within type
		FunctionElement method = new FunctionElement();
		method.setName(methodName);
		type.addProperty(method);

		// write type to index
		this.writeType(type);

		// then retrieve it
		List<TypeElement> retrievedTypes = this.getType(typeName);
		TypeElement retrievedType = retrievedTypes.get(0);

		assertNotNull(retrievedType);
		assertEquals(typeName, retrievedType.getName());

		// make sure we have one property
		List<PropertyElement> properties = retrievedType.getProperties();
		assertNotNull(properties);
		assertTrue(properties.size() == 1);

		// make sure it is a function
		PropertyElement property = properties.get(0);
		assertTrue(property instanceof FunctionElement);

		// make sure it is the function we added earlier
		FunctionElement retrievedMethod = (FunctionElement) property;
		assertEquals(methodName, retrievedMethod.getName());
	}

	/**
	 * testProperty
	 */
	public void testProperty()
	{
		String typeName = "MyClass";
		String propertyName = "myProperty";

		// create type
		TypeElement type = new TypeElement();
		type.setName(typeName);

		// create property within type
		PropertyElement property = new PropertyElement();
		property.setName(propertyName);
		type.addProperty(property);

		// write type to index
		this.writeType(type);

		// then retrieve it
		List<TypeElement> retrievedTypes = this.getType(typeName);
		TypeElement retrievedType = retrievedTypes.get(0);

		assertNotNull(retrievedType);
		assertEquals(typeName, retrievedType.getName());

		// make sure we have one property
		List<PropertyElement> properties = retrievedType.getProperties();
		assertNotNull(properties);
		assertTrue(properties.size() == 1);

		// make sure the name is correct
		PropertyElement retrievedProperty = properties.get(0);
		assertEquals(propertyName, retrievedProperty.getName());
	}

	public void testRequires() throws URISyntaxException
	{
		List<String> requires = CollectionsUtil.newList("abc.js", "def.js");
		URI location = new URI("testFile.js");

		// write out paths
		JSIndexWriter writer = new JSIndexWriter();
		writer.writeRequires(getIndex(), requires, location);

		// read them back in
		JSIndexReader reader = new JSIndexReader();
		List<String> newList = reader.getRequires(getIndex(), location);

		assertEquals(requires, newList);
	}

	public void testRequires2() throws URISyntaxException
	{
		List<String> requires1 = CollectionsUtil.newList("abc.js", "def.js");
		URI location1 = new URI("testFile1.js");
		List<String> requires2 = CollectionsUtil.newList("def.js", "ghi.js");
		URI location2 = new URI("testFile2.js");

		// write out paths
		JSIndexWriter writer = new JSIndexWriter();
		writer.writeRequires(getIndex(), requires1, location1);
		writer.writeRequires(getIndex(), requires2, location2);

		// read them back in
		JSIndexReader reader = new JSIndexReader();
		List<String> newList1 = reader.getRequires(getIndex(), location1);
		List<String> newList2 = reader.getRequires(getIndex(), location2);

		assertEquals(requires1, newList1);
		assertEquals(requires2, newList2);
	}

	public void testSpecialAllUserAgentFlag()
	{
		// create property and use all user agents
		PropertyElement property = new PropertyElement();
		property.setName("property");
		property.setHasAllUserAgents();

		// create type for property so we can write it to the index
		TypeElement type = new TypeElement();
		type.setName("Testing");
		type.addProperty(property);

		// write type and its property
		JSIndexWriter writer = new JSIndexWriter();
		writer.writeType(getIndex(), type);

		// perform low-level query
		// @formatter:off
		List<QueryResult> properties = getIndex().query(
			new String[] { IJSIndexConstants.PROPERTY },
			type.getName(),
			SearchPattern.PREFIX_MATCH
		);
		// @formatter:on

		// make sure we got something
		assertNotNull(properties);
		assertEquals(1, properties.size());

		// split result into columns
		String word = properties.get(0).getWord();
		String[] columns = IndexReader.DELIMITER_PATTERN.split(word);
		assertEquals(3, columns.length);

		// grab last column and parse as JSON
		String json = columns[2];
		Object m = JSON.parse(json);

		// make sure we have a map
		assertTrue("Expected a Map from the JSON string", m instanceof Map);
		Map<?, ?> map = (Map<?, ?>) m;

		// test userAgents for "special value" which is really just a null value.
		assertTrue("Expected a userAgents property", map.containsKey("userAgents"));
		assertNull("Expected userAgents property to be null", map.get("userAgents"));
	}

	/**
	 * testType
	 */
	public void testType()
	{
		String typeName = "MyClass";

		TypeElement type = new TypeElement();
		type.setName(typeName);
		this.writeType(type);

		List<TypeElement> retrievedTypes = this.getType(typeName);
		TypeElement retrievedType = retrievedTypes.get(0);

		assertNotNull(retrievedType);
		assertEquals(typeName, retrievedType.getName());
	}

	/**
	 * Test for APSTUD-4535
	 */
	public void testTypeCaching()
	{
		TestBuildContext myContext = new TestBuildContext("indexing/dottedTypes.js");

		try
		{
			IParseRootNode ast = myContext.getAST();
			JSFileIndexingParticipant indexParticipant = new JSFileIndexingParticipant();
			Index index = getIndex();

			indexParticipant.processParseResults(myContext, index, ast, new NullProgressMonitor());
			JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();

			List<TypeElement> types = queryHelper.getTypes(index);
			assertNotNull(types);
			assertEquals("Expected 3 types", 3, types.size());

			// remove index and do it all over again
			getIndexManager().removeIndex(URI.create(IJSIndexConstants.METADATA_INDEX_LOCATION));

			// make sure we get the same results
			index = getIndex();
			indexParticipant.processParseResults(myContext, index, ast, new NullProgressMonitor());
			types = queryHelper.getTypes(index);
			assertNotNull(types);
			assertEquals("Expected 3 types", 3, types.size());
		}
		catch (CoreException e)
		{
			fail(e.getMessage());
		}
	}

	/**
	 * APSTUD-4117
	 */
	public void testFunctionDocumentationWithoutReturnTag()
	{
		TestBuildContext myContext = new TestBuildContext("indexing/functionDocsWithoutReturn.js");

		try
		{
			IParseRootNode ast = myContext.getAST();
			JSFileIndexingParticipant indexParticipant = new JSFileIndexingParticipant();
			Index index = getIndex();

			indexParticipant.processParseResults(myContext, index, ast, new NullProgressMonitor());
			JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();

			List<PropertyElement> types = queryHelper.getGlobals(index, "abc");
			assertNotNull(types);
			assertTrue("Expected at least a single property for 'abc'", !types.isEmpty());

			PropertyElement property = types.get(0);
			assertTrue("Expected a FunctionElement", property instanceof FunctionElement);

			FunctionElement function = (FunctionElement) property;
			List<String> returnTypes = function.getReturnTypeNames();
			assertNotNull(returnTypes);
			assertEquals("Expected a single return type for 'abc'", 1, returnTypes.size());
			assertEquals("Expected 'Number' return type", "Number", returnTypes.get(0));
		}
		catch (CoreException e)
		{
			fail(e.getMessage());
		}
	}

	/**
	 * APSTUD-4116
	 */
	public void testFunctionDocumenationWithoutParamTag()
	{
		TestBuildContext myContext = new TestBuildContext("indexing/functionDocsWithoutParam.js");

		try
		{
			IParseRootNode ast = myContext.getAST();
			JSFileIndexingParticipant indexParticipant = new JSFileIndexingParticipant();
			Index index = getIndex();

			indexParticipant.processParseResults(myContext, index, ast, new NullProgressMonitor());
			JSIndexQueryHelper queryHelper = new JSIndexQueryHelper();

			List<PropertyElement> types = queryHelper.getGlobals(index, "abc");
			assertNotNull(types);
			assertTrue("Expected at least a single property for 'abc'", !types.isEmpty());

			PropertyElement property = types.get(0);
			assertTrue("Expected a FunctionElement", property instanceof FunctionElement);

			FunctionElement function = (FunctionElement) property;
			List<String> parameters = function.getParameterNames();
			assertNotNull(parameters);
			assertEquals("Expected 3 parameters for 'abc'", 3, parameters.size());

			assertEquals("Expected parameter 1's name to be 'a'", "a", parameters.get(0));
			assertEquals("Expected parameter 2's name to be 'b'", "b", parameters.get(1));
			assertEquals("Expected parameter 3's name to be 'c'", "c", parameters.get(2));
		}
		catch (CoreException e)
		{
			fail(e.getMessage());
		}
	}

}
