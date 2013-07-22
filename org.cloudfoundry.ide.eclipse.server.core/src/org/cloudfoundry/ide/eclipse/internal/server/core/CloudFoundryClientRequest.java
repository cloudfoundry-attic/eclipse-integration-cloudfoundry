/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.springframework.web.client.RestClientException;

/**
 * Performs a CF client call, and times out if the call fails due to errors.
 * Also handles any errors thrown when calling the client.
 */
public abstract class CloudFoundryClientRequest<T> {

	public static final long DEFAULT_INTERVAL = 60 * 1000;

	public static final long SHORT_INTERVAL = 5 * 1000;

	public static final long MEDIUM_INTERVAL = 25 * 1000;

	public static final long DEPLOYMENT_TIMEOUT = 10 * 60 * 1000;

	// Set very high as we do not want to give up on staging. Eventually, either
	// staging will complete, or the server will notify with another error that
	// staging failed
	public static final long STAGING_TIMEOUT = 6 * 1000 * 1000;

	public static final long MEDIUM_TIMEOUT = 10 * 1000;

	public static final long ONE_SECOND_INTERVAL = 1000;

	public static final long UPLOAD_TIMEOUT = 60 * 1000;

	private long timeLeft;

	public CloudFoundryClientRequest(long requestTimeOut) {
		this.timeLeft = requestTimeOut;
	}

	abstract protected T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException;

	public T run(CloudFoundryOperations client, CloudFoundryOperationsHandler handler, SubMonitor progress)
			throws CoreException {

		Exception error = null;
		while (timeLeft > 0) {

			try {
				return doRun(client, progress);
			}
			catch (CoreException e) {
				error = e;
			}
			catch (CloudFoundryException cfe) {
				error = cfe;
			}
			catch (RestClientException rce) {
				error = rce;
			}

			// Client may not be logged in. Log in and try again after 1
			// second
			long timeTaken = ONE_SECOND_INTERVAL;
			if (error instanceof CloudFoundryException
					&& handler.shouldAttemptClientLogin((CloudFoundryException) error)) {
				client.login();
			}
			else if (CloudErrorUtil.isAppStoppedStateError(error)) {
				timeTaken = ONE_SECOND_INTERVAL * 2;
			}
			else if (CloudErrorUtil.isAppStaging(error)) {
				timeTaken = ONE_SECOND_INTERVAL * 4;
			}
			else {
				// Some other error encountered should
				// stop any further
				// retries
				break;
			}

			timeLeft -= timeTaken;

			try {
				Thread.sleep(timeTaken);
			}
			catch (InterruptedException e) {
				// Ignore, continue with the next iteration
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

}
