/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.TestCase;

import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Server;

/**
 * @author Steffen Pingel
 */
public class CloudUtilTest extends TestCase {

	private Harness harness;

	private IServer server;

	private IProject project;

	@Override
	protected void setUp() throws Exception {
		harness = CloudFoundryTestFixture.getTestFixture().createHarness();
		server = harness.createServer();
	}

	@Override
	protected void tearDown() throws Exception {
		harness.dispose();
	}

	public void testCreateWarFile() throws Exception {
		harness.createProject("appclient-module");
		project = harness.createProject("dynamic-webapp-with-appclient-module");
		harness.addModule(project);

		IModule[] modules = ServerUtil.getModules(project);
		File file = CloudUtil.createWarFile(modules, (Server) server, new NullProgressMonitor());

		List<String> files = new ArrayList<String>();
		ZipFile zipFile = new ZipFile(file);
		Enumeration<? extends ZipEntry> en = zipFile.entries();
		while (en.hasMoreElements()) {
			ZipEntry entry = en.nextElement();
			files.add(entry.getName());
		}
		// the directory entry is not always present, remove to avoid test
		// failure
		files.remove("WEB-INF/lib/");
		Collections.sort(files);
		List<String> expected = Arrays.asList("META-INF/", "META-INF/MANIFEST.MF", "WEB-INF/", "WEB-INF/classes/",
				"WEB-INF/classes/TestServlet.class", "WEB-INF/lib/appclient-module.jar", "WEB-INF/web.xml",
				"index.html");
		assertEquals(expected, files);
	}

}
