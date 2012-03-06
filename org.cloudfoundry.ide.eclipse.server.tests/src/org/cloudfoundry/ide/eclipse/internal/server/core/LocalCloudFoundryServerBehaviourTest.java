/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.ide.eclipse.server.tests.server.TestServlet.Response;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class LocalCloudFoundryServerBehaviourTest extends AbstractCloudFoundryTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		testServlet = harness.startMockServer();
	}

	@Override
	protected void tearDown() throws Exception {
		harness.dispose();
	}

	public void testValidate() throws Exception {
		try {
			CloudFoundryServerBehaviour.validate(harness.getUrl(), "user", "password", new NullProgressMonitor());
		}
		catch (CoreException e) {
			assertEquals("Expects 503 error message",
					"Communication with server failed: 503 Service Temporarily Unavailable", e.getMessage());
		}
	}

	public void testValidateJson() throws Exception {
		testServlet.addResponse(new Response(HttpServletResponse.SC_OK, null, "{ \"response\" : 503 }"));
		try {
			CloudFoundryServerBehaviour.validate(harness.getUrl(), "user", "password", new NullProgressMonitor());
		}
		catch (CoreException e) {
			assertEquals("Expect simple error message", "Unable to communicate with server", e.getMessage());
		}
	}

	@Override
	protected Harness createHarness() {

		return CloudFoundryTestFixture.LOCAL.harness();
	}
}
