package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.client.lib.CloudApplication.AppState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

public class StopApplicationInWaitOperation extends AbstractApplicationInWaitOperation {

	public StopApplicationInWaitOperation(CloudFoundryServer cloudServer) {
		super(cloudServer, "Stopping application");
	}

	@Override
	protected void doOperation(CloudFoundryServerBehaviour behaviour, IModule module, IProgressMonitor progress)
			throws CoreException {
		behaviour.stopModule(new IModule[] { module }, progress);
	}

	@Override
	protected boolean isInState(AppState state) {
		return state.equals(AppState.STOPPED);
	}

}
