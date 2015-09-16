/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
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
package org.eclipse.cft.server.tests.core;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.cft.server.tests.util.ModulesRefreshListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * Provides API for testing Cloud operations asynchronously as well as provides
 * a mechanism to test module refresh.
 *
 */
public abstract class AbstractAsynchCloudTest extends AbstractCloudFoundryTest {

	@Override
	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return null;
	}

	/**
	 * Runs the given operation asynchronously in a separate Job, and waits in
	 * the current thread for the refresh triggered by that operation to
	 * complete. This tests the CF tooling asynch, event-driven refresh module
	 * behaviour. When checking for the refresh to complete, it will also verify
	 * that the expected refresh event type is the event type that was actually
	 * received.
	 * @param op
	 * @param testPrefix pass null if using a module listener to detect all.
	 * Otherwise will only listen and test against single-module refresh for
	 * module with the corresponding app name using the prefix module refresh
	 * @param expectedRefreshEventType
	 * @throws CoreException
	 */
	protected void asynchExecuteOperationWaitForRefresh(final ICloudFoundryOperation op, String testPrefix,
			int expectedRefreshEventType) throws Exception {
		String expectedAppName = testPrefix != null ? harness.getDefaultWebAppName(testPrefix) : null;
		IRunnableWithProgress runnable = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					op.run(monitor);
				}
				catch (CoreException e) {
					e.printStackTrace();
				}
			}
		};
		asynchExecuteOperationWaitForRefresh(runnable, expectedAppName, expectedRefreshEventType);
	}

	/**
	 * Runs the given operation asynchronously in a separate Job, and waits in
	 * the current thread for the refresh triggered by that operation to
	 * complete. This tests the CF tooling asynch, event-driven refresh module
	 * behaviour. When checking for the refresh to complete, it will also verify
	 * that the expected refresh event type is the event type that was actually
	 * received.
	 * @param runnable
	 * @param expectedAppName if not null, will use a module refresh listener
	 * listening for single module refresh. Otherwise if null listens to all
	 * modules refresh
	 * @param expectedRefreshEventType
	 * @throws CoreException
	 */
	protected void asynchExecuteOperationWaitForRefresh(final IRunnableWithProgress runnable, String expectedAppName,
			int expectedRefreshEventType) throws Exception {

		ModulesRefreshListener listener = getModulesRefreshListener(expectedAppName, cloudServer,
				expectedRefreshEventType);

		asynchExecuteOperation(runnable);

		assertModuleRefreshedAndDispose(listener, expectedRefreshEventType);
	}

	protected void asynchExecuteOperation(final IRunnableWithProgress runnable) {
		Job job = new Job("Running Cloud Operation") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					runnable.run(monitor);
				}
				catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}

		};
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}

	protected static ModulesRefreshListener getModulesRefreshListener(String appName, CloudFoundryServer cloudServer,
			int expectedEventType) {
		ModulesRefreshListener eventHandler = ModulesRefreshListener.getListener(appName, cloudServer,
				expectedEventType);
		assertFalse(eventHandler.hasBeenRefreshed());
		return eventHandler;
	}

	/**
	 * Waits for module refresh to complete and asserts that the given event was
	 * received. This blocks the thread.
	 * @param refreshHandler
	 * @param expectedEventType
	 * @throws CoreException
	 */
	protected static void assertModuleRefreshedAndDispose(ModulesRefreshListener refreshHandler, int expectedEventType)
			throws CoreException {
		assertTrue(refreshHandler.modulesRefreshed(new NullProgressMonitor()));
		assertTrue(refreshHandler.hasBeenRefreshed());
		assertEquals(expectedEventType, refreshHandler.getMatchedEvent().getType());
		refreshHandler.dispose();
	}

}
