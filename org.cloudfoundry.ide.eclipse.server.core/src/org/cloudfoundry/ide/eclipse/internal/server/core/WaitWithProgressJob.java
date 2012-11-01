package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public abstract class WaitWithProgressJob extends AbstractWaitWithProgressJob<Boolean> {

	public WaitWithProgressJob(int ticks, long sleepTime) {
		super(ticks, sleepTime);
	}

	@Override
	protected Boolean runInWait(IProgressMonitor monitor) throws CoreException {
		boolean result = internalRunInWait(monitor);
		return new Boolean(result);
	}

	abstract protected boolean internalRunInWait(IProgressMonitor monitor) throws CoreException;

	@Override
	protected boolean isValid(Boolean result) {
		return result != null && result.booleanValue();
	}

}
