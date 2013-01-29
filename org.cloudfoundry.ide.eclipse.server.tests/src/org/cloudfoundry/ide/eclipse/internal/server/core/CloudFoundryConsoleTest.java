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

import java.util.Arrays;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleContent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleContent.Result;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.ConsoleManager;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.cloudfoundry.ide.eclipse.server.tests.util.FullFileConsoleContent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

public class CloudFoundryConsoleTest extends AbstractCloudFoundryTest {

	protected Harness createHarness() {
		return CloudFoundryTestFixture.current().harness();
	}

	public void testRangedConsoleContent() throws Exception {
		harness.createProjectAndAddModule("dynamic-webapp");

		IModule[] modules = server.getModules();
		assertEquals("Expected dynamic-webapp module, got " + Arrays.toString(modules), 1, modules.length);
		int moduleState = server.getModulePublishState(modules);
		assertEquals(IServer.PUBLISH_STATE_UNKNOWN, moduleState);

		serverBehavior.deployOrStartModule(modules, true, null);
		moduleState = server.getModuleState(modules);
		assertEquals(IServer.STATE_STARTED, moduleState);

		ConsoleContent content = getRangeFileConsoleContent(modules[0]);
		assertNotNull(content);

		ConsoleContent.Result result = getResult(content);
		assertNotNull(result);

		String errorContent = result.getErrorContent();
		assertTrue("Expected error log content for app " + modules[0].getName(),
				errorContent != null && errorContent.length() > 0);
		assertEquals(result.getErrorContent().length(), result.getErrorEndingPosition());

	}

	public void testFullConsoleContent() throws Exception {
		harness.createProjectAndAddModule("dynamic-webapp");

		IModule[] modules = server.getModules();
		assertEquals("Expected dynamic-webapp module, got " + Arrays.toString(modules), 1, modules.length);
		int moduleState = server.getModulePublishState(modules);
		assertEquals(IServer.PUBLISH_STATE_UNKNOWN, moduleState);

		serverBehavior.deployOrStartModule(modules, true, null);
		moduleState = server.getModuleState(modules);
		assertEquals(IServer.STATE_STARTED, moduleState);

		ConsoleContent content = getFullFileConsoleContent(modules[0]);
		assertNotNull(content);

		ConsoleContent.Result result = getResult(content);
		assertNotNull(result);

		String errorContent = result.getErrorContent();
		assertTrue("Expected error log content for app " + modules[0].getName(),
				errorContent != null && errorContent.length() > 0);
		assertEquals(result.getErrorContent().length(), result.getErrorEndingPosition());

	}

	public void testFullMatchesRangedConsoleContent() throws Exception {
		harness.createProjectAndAddModule("dynamic-webapp");

		IModule[] modules = server.getModules();
		assertEquals("Expected dynamic-webapp module, got " + Arrays.toString(modules), 1, modules.length);
		int moduleState = server.getModulePublishState(modules);
		assertEquals(IServer.PUBLISH_STATE_UNKNOWN, moduleState);

		serverBehavior.deployOrStartModule(modules, true, null);
		moduleState = server.getModuleState(modules);
		assertEquals(IServer.STATE_STARTED, moduleState);

		ConsoleContent content = getFullFileConsoleContent(modules[0]);
		assertNotNull(content);

		ConsoleContent.Result fullResult = getResult(content);
		assertNotNull(fullResult);

		content.reset();

		content = getRangeFileConsoleContent(modules[0]);
		assertNotNull(content);

		ConsoleContent.Result rangedResult = getResult(content);
		assertNotNull(rangedResult);

		assertEquals(fullResult.getErrorContent(), rangedResult.getErrorContent());
		assertEquals(fullResult.getErrorEndingPosition(), rangedResult.getErrorEndingPosition());
		assertEquals(fullResult.getOutContent(), rangedResult.getOutContent());
		assertEquals(fullResult.getOutEndingPosition(), rangedResult.getOutEndingPosition());

	}

	/*
	 * HELPER METHODS
	 */
	protected ConsoleContent getFullFileConsoleContent(IModule module) throws CoreException {

		int instanceIndex = 0;
		CloudApplication app = getCloudApplication(module);
		MessageConsole console = ConsoleManager.getOrCreateConsole(server, app, instanceIndex);

		return new FullFileConsoleContent(cloudServer, console, app, instanceIndex);

	}

	protected ConsoleContent getRangeFileConsoleContent(IModule module) throws CoreException {

		int instanceIndex = 0;
		CloudApplication app = getCloudApplication(module);
		MessageConsole console = ConsoleManager.getOrCreateConsole(server, app, instanceIndex);

		return new ConsoleContent(cloudServer, console, app, instanceIndex);

	}

	protected ConsoleContent.Result getResult(final ConsoleContent content) throws Exception {
		return new AbstractWaitWithProgressJob<ConsoleContent.Result>(5, 1000) {

			@Override
			protected Result runInWait(IProgressMonitor monitor) throws CoreException {
				Result result = content.getFileContent(monitor);
				// if either error or stdout content is obtained, stop waiting
				if (result.getErrorContent() != null && result.getErrorContent().length() > 0
						|| result.getOutContent() != null && result.getOutContent().length() > 0) {
					return result;
				}
				else {
					return null;
				}
			}

			// Set it to fix build errors that fail because it takes too long to
			// get a result
			protected boolean shouldRetryOnError(Throwable t) {
				return true;
			}

		}.run(new NullProgressMonitor());
	}
}
