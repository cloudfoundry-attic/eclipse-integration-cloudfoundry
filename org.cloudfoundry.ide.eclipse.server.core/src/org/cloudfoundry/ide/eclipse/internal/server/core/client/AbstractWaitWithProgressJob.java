/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
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

	protected boolean shouldRetryOnError(Throwable t) {
		return false;
	}

	protected boolean isValid(T result) {
		return result != null;
	}

	/**
	 * Returns a result, or throws an exception ONLY if the result is invalid
	 * AND an exception was thrown after all attempts have been exhausted. Will
	 * only re-throw the last exception that was thrown. Note that the result
	 * may still be null.
	 * 
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public T run(IProgressMonitor monitor) throws CoreException {

		Throwable lastError = null;

		T result = null;
		int i = 0;
		while (i < attempts && !monitor.isCanceled()) {
			boolean reattempt = false;
			// Two conditions which results in a reattempt:
			// 1. Result is not valid
			// 2. Exception is thrown and an exception handler decides that a
			// reattempt should happen based on the given error

			try {
				result = runInWait(monitor);
				reattempt = !isValid(result);
			}
			catch (Throwable th) {
				lastError = th;
				reattempt = shouldRetryOnError(lastError);
			}

			if (reattempt) {
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
			i++;
		}

		// Only throw exception if an error was generated and an invalid result
		// was obtained.
		if (!isValid(result) && lastError != null) {
			CoreException coreError = lastError instanceof CoreException ? (CoreException) lastError : CloudErrorUtil
					.toCoreException(lastError);
			throw coreError;
		}
		return result;
	}
}