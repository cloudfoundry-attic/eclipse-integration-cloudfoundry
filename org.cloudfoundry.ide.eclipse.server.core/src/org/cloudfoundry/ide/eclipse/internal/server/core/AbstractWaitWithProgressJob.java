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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Performs an operation that is expect to return a result. The result is
 * validated, and if invalid, a waiting period occurs, and the operation
 * attempted again until a valid result is obtained, or the maximum number of
 * attempts is reached. If an invalid result is returned at the end of the
 * maximum attempt, and it's due to an error, a CoreException is thrown.
 * <p/>
 * A check is also performed on the progress monitor, if it is cancelled before
 * the maximum number of attempts is reached, the operation is cancelled,
 * regardless of whether a valid result was obtained or not.
 */
public abstract class AbstractWaitWithProgressJob<T> {

	private final int attempts;

	private final long sleepTime;

	public AbstractWaitWithProgressJob(int attempts, long sleepTime) {
		this.attempts = attempts;
		this.sleepTime = sleepTime;
	}

	/**
	 * To continue waiting, return an invalid result that is checked as invalid
	 * by the isValid(...) API
	 * @return
	 */
	abstract protected T runInWait(IProgressMonitor monitor) throws CoreException;

	protected boolean isValid(T result) {
		return result != null;
	}

	public T run(IProgressMonitor monitor) throws CoreException {

		Throwable error = null;

		T result = null;
		int i = 0;
		while (i++ < attempts && !monitor.isCanceled()) {
			try {
				result = runInWait(monitor);
			}
			catch (Throwable th) {
				error = th;
			}
			if (!isValid(result)) {

				try {
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e) {
					// Ignore and proceed
				}
			}
			else {
				break;
			}
		}

		if (!isValid(result) && error != null) {
			CoreException coreError = error instanceof CoreException ? (CoreException) error : new CoreException(
					CloudFoundryPlugin.getErrorStatus(error));
			throw coreError;
		}
		return result;
	}
}