package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.lang.reflect.InvocationTargetException;

import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.ModulesRefreshListener;
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
		waitForExistingRefreshJobComplete();
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

	protected void waitForExistingRefreshJobComplete() throws CoreException {
		int total = 10;
		int attempts = total;
		long wait = 3000;
		boolean scheduled = serverBehavior.getRefreshHandler().isScheduled();

		// Test the Server behaviour API that checks if application is running
		for (; scheduled && attempts > 0; attempts--) {
			scheduled = serverBehavior.getRefreshHandler().isScheduled();

			if (scheduled) {
				try {
					Thread.sleep(wait);
				}
				catch (InterruptedException e) {

				}
			}

		}

		if (scheduled) {
			throw CloudErrorUtil.toCoreException("Timed out waiting for existing refresh job to complete");
		}
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
