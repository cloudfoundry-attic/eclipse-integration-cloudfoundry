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

public abstract class WaitWithProgressJob<T> {

	private final int ticks;

	private final long sleepTime;

	public WaitWithProgressJob(int ticks, long sleepTime) {
		this.ticks = ticks;
		this.sleepTime = sleepTime;
	}

	/**
	 * Return null if the run operation failed to obtain a run result. Null
	 * value or an exception will cause the wait operation to wait for a
	 * specified amount of time before trying again. Returning a non-null
	 * result will stop any further waiting.
	 * @return
	 */
	abstract protected T runInWait(IProgressMonitor monitor) throws CoreException;

	public T run(IProgressMonitor monitor) throws CoreException {

		Throwable error = null;

		T result = null;
		int i = 0;
		while (i < ticks && !monitor.isCanceled()) {
			try {
				result = runInWait(monitor);
			}
			catch (Throwable th) {
				error = th;
			}
			if (result == null) {

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

		if (result == null && error != null) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(error));
		}
		return result;
	}
}