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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.debug.DebugModeType;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

//NOTE: These do not run until debug support is re-enabled in CF servers (Debug support has been disable since CF 1.5.0)
public class CloudFoundryDebugTest extends AbstractCloudFoundryTest {

	/*
	 * 
	 * 
	 * Helper methods and types.
	 */

	protected void assertStarted(final DebugModeType expectedType, String appPrefix) throws Exception {
		new CreateAppAndDebug() {

			@Override
			protected void performTestBeforeStopping(IModule[] modules) throws Exception {
				// Verify that the application was deployed in the correct Debug
				// Mode
				assertIsOfDebugModeType(modules, expectedType);

				// Verify it has started
				int moduleState = server.getModuleState(modules);
				assertEquals(IServer.STATE_STARTED, moduleState);
			}

		}.launch(appPrefix);
	}

	protected void assertRestarted(final DebugModeType expectedType, String appPrefix) throws Exception {
		new RestartInDebugHandler() {

			@Override
			protected void performTestBeforeStopping(IModule[] modules) throws Exception {
				// Verify that the application was deployed in the correct Debug
				// Mode
				assertIsOfDebugModeType(modules, DebugModeType.SUSPEND);
			}

		}.launch(appPrefix);
	}

	protected void assertIsOfDebugModeType(IModule[] modules, DebugModeType expectedType) {
		for (IModule module : modules) {
			DebugModeType actualType = getActualDeploymentType(module);
			assertEquals(expectedType, actualType);
		}
	}

	protected DebugModeType getActualDeploymentType(IModule module) {
		return serverBehavior.getDebugModeType(module, null);
	}

	/**
	 * Creates and starts an application in regular mode
	 */
	protected final AbstractLaunchAppHandler REGULAR_START = new AbstractLaunchAppHandler() {

		@Override
		public IModule[] launch(String appPrefix) throws Exception {
			// Create the app first
			createWebApplicationProject();
			return super.launch(appPrefix);
		}

		@Override
		protected void launchInModeType(IModule[] modules) throws Exception {
			serverBehavior.startModule(modules, new NullProgressMonitor());
		}

	};

	/**
	 * 
	 * Restarts an already deployed application in regular mode. Application
	 * must have already been created and deployed.
	 */
	protected final AbstractLaunchAppHandler REGULAR_RESTART = new AbstractLaunchAppHandler() {

		@Override
		protected void launchInModeType(IModule[] modules) throws Exception {
			serverBehavior.restartModule(modules, null);
		}

	};

	/**
	 * 
	 * Create and deploy an application in the specified Debug mode
	 * 
	 * 
	 */
	abstract class CreateAppAndDebug extends AbstractDebugLaunchAppHandler {

		@Override
		public IModule[] launch(String appPrefix) throws Exception {
			// Create the app first before launching
			createWebApplicationProject();
			return super.launch(appPrefix);
		}

		@Override
		protected void launchInModeType(IModule[] modules) throws Exception {
			serverBehavior.debugModule(modules, new NullProgressMonitor());
		}

	}

	/**
	 * Restart an already created and deployed application in the specified
	 * debug mode
	 * 
	 * @author Nieraj Singh
	 * 
	 */
	abstract class RestartInDebugHandler extends AbstractDebugLaunchAppHandler {

		@Override
		protected void launchInModeType(IModule[] modules) throws Exception {
			serverBehavior.restartDebugModule(modules, null);
		}

	}

	/**
	 * Launches an already deployed application. Subclasses must specify in
	 * which mode the application should be launched.
	 * 
	 */
	abstract class AbstractLaunchAppHandler {
		public IModule[] launch(String appPrefix) throws Exception {

			IModule[] modules = server.getModules();
			assertEquals("Expected 1 module, got " + Arrays.toString(modules), 1, modules.length);

			launchInModeType(modules);

			int moduleState = server.getModuleState(modules);
			assertEquals(IServer.STATE_STARTED, moduleState);

			CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(modules[0]);
			List<String> uris = appModule.getApplication().getUris();
			assertEquals(Collections.singletonList(harness.getExpectedDefaultURL(appPrefix)), uris);

			// wait 1s until app is actually started
			// FIXNS: for now skip testing content for debug launches
			// URI uri = new URI("http://" + harness.getUrl("dynamic-webapp") +
			// "/index.html");
			// assertEquals("Hello World.", getContent(uri));

			return modules;
		}

		abstract protected void launchInModeType(IModule[] modules) throws Exception;

	}

	/**
	 * Launches an application in debug mode. As all debug launches also attempt
	 * to connect the application to a local debugger, this class handles manual
	 * stopping of the application to disconnect the debugger from the remote VM
	 * 
	 */
	abstract class AbstractDebugLaunchAppHandler extends AbstractLaunchAppHandler {
		@Override
		public IModule[] launch(String appPrefix) throws Exception {
			IModule[] modules = super.launch(appPrefix);

			performTestBeforeStopping(modules);

			// Stop the module to make sure port/IP of app connected to the
			// debugger are cleared
			serverBehavior.stopModule(modules, new NullProgressMonitor());

			int moduleState = server.getModuleState(modules);
			assertEquals(IServer.STATE_STOPPED, moduleState);
			return modules;
		}

		abstract protected void performTestBeforeStopping(IModule[] modules) throws Exception;

	}

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}

}
