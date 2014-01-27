/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Performs a CF client call, and times out if the call fails due to errors, as
 * well as proxy checks prior to sending the request. Also handles any errors
 * thrown when calling the client.
 */
public abstract class ClientRequestOperation<T> {

	/*
	 * Intervals are how long a thread should sleep before moving to the next
	 * iteration, or how long a refresh operation should wait before refreshing
	 * the deployed apps.
	 */
	public static final long DEFAULT_INTERVAL = 60 * 1000;

	public static final long SHORT_INTERVAL = 5 * 1000;

	public static final long MEDIUM_INTERVAL = 10 * 1000;

	public static final long ONE_SECOND_INTERVAL = 1000;

	public static final long LOGIN_INTERVAL = 2000;

	public static final long DEPLOYMENT_TIMEOUT = 10 * 60 * 1000;

	public static final long UPLOAD_TIMEOUT = 60 * 1000;

	public static final long DEFAULT_CF_CLIENT_REQUEST_TIMEOUT = 15 * 1000;

	private final CloudFoundryOperations client;

	private long timeLeft;

	public ClientRequestOperation(CloudFoundryOperations client, long requestTimeOut) {
		this.timeLeft = requestTimeOut;
		this.client = client;
	}

	/**
	 * Calling this constructor means a client operation will only be performed
	 * once when the run command is invoked. No waiting and re-attempts will be
	 * made in case of failure.
	 * @param client
	 */
	public ClientRequestOperation(CloudFoundryOperations client) {
		this.timeLeft = 0;
		this.client = client;
	}

	abstract protected T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException;

	public T run(SubMonitor progress) throws CoreException {

		Throwable error = null;

		boolean reattempt = true;
		while (reattempt) {

			long interval = -1;

			try {
				return doRun(client, progress);
			}
			catch (Throwable e) {
				error = e;
			}

			interval = getWaitInterval(error, progress);
			timeLeft -= interval;

			if (interval > 0 && timeLeft >= 0) {

				try {
					Thread.sleep(interval);
				}
				catch (InterruptedException e) {
					// Ignore, continue with the next iteration
				}

				reattempt = true;
			}
			else {
				break;
			}
		}

		// If reached here, some error has occurred, although if for some
		// reason no error is set, it still means that the operation
		// failed somehow
		if (error == null) {
			error = new CoreException(
					CloudFoundryPlugin
							.getErrorStatus("Unknown Cloud Foundry plugin error while trying to perform client call in "
									+ CloudFoundryServerBehaviour.Request.class.getName()));
		}

		if (error instanceof CoreException) {
			throw (CoreException) error;
		}
		else {
			throw CloudErrorUtil.toCoreException(error);
		}
	}

	/**
	 * Given an error, determine how long the operation should wait before
	 * trying again before timeout is reached. Return -1 if the operation should
	 * stop trying and handle the last error that was caught, or throw
	 * CoreException if further errors occurred while determining the wait
	 * interval.
	 * 
	 * <p/>
	 * 
	 * By default it returns -1, meaning that the request is attempted only
	 * once, and any exceptions thrown will not result in reattempts. Subclasses
	 * can override to determine different reattempt conditions.
	 * @param exception to determine how long to wait until another attempt is
	 * made to run the operation. Note that if timeout is sooner than the
	 * interval, no further attempts will be made.
	 * @param monitor
	 * @return interval to wait , or -1 if operation should terminate right away
	 * without attempting again.
	 * @throw CoreException if failed to determine interval. A CoreException
	 * will result in no further attempts.
	 */
	protected long getWaitInterval(Throwable exception, SubMonitor monitor) throws CoreException {
		return -1;
	}

}
