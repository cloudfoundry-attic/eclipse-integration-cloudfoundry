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
import org.cloudfoundry.client.lib.NotFinishedStagingException;
import org.cloudfoundry.client.lib.StagingErrorException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.springframework.web.client.RestClientException;

/**
 * Performs a CF client call, and times out if the call fails due to errors, as
 * well as proxy checks prior to sending the request. Also handles any errors
 * thrown when calling the client.
 */
public abstract class CloudFoundryClientRequest<T> {

	/*
	 * Intervals are how long a thread should sleep before moving to the next
	 * iteration, or how long a refresh operation should wait before refreshing
	 * the deployed apps.
	 */
	public static final long DEFAULT_INTERVAL = 60 * 1000;

	public static final long SHORT_INTERVAL = 5 * 1000;

	public static final long MEDIUM_INTERVAL = 10 * 1000;

	public static final long ONE_SECOND_INTERVAL = 1000;

	// Set very high as we do not want to give up on staging. Eventually, either
	// staging will complete, or the server will notify with another error that
	// staging failed
	public static final long STAGING_TIMEOUT = 6 * 1000 * 1000;

	public static final long DEPLOYMENT_TIMEOUT = 10 * 60 * 1000;

	public static final long UPLOAD_TIMEOUT = 60 * 1000;

	public static final long DEFAULT_CF_CLIENT_REQUEST_TIMEOUT = 15 * 1000;

	private final CloudFoundryOperations client;

	private final CloudFoundryServer server;

	private long timeLeft;

	public CloudFoundryClientRequest(CloudFoundryOperations client, CloudFoundryServer server, long requestTimeOut) {
		this.timeLeft = requestTimeOut;
		this.client = client;
		this.server = server;
	}

	abstract protected T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException;

	public T run(SubMonitor progress) throws CoreException {

		String cloudURL = server.getUrl();

		CloudFoundryLoginHandler handler = new CloudFoundryLoginHandler(client, cloudURL);

		// Always check if proxy settings have changed.
		handler.updateProxyInClient(client);

		Exception error = null;
		while (timeLeft > 0) {

			long timeTaken = ONE_SECOND_INTERVAL;
			CloudFoundryException cloudException = null;
			try {
				return doRun(client, progress);
			}
			catch (NotFinishedStagingException notFinishedError) {
				// Continue trying
				error = notFinishedError;
				timeTaken = ONE_SECOND_INTERVAL * 2;
			}
			catch (StagingErrorException stagingError) {
				// Most likely cannot continue if error occurred during staging,
				// so quit.
				error = stagingError;
				break;
			}
			catch (CoreException e) {

				if (e.getCause() instanceof CloudFoundryException) {
					cloudException = (CloudFoundryException) e.getCause();
				}
				else {
					error = e;
					break;
				}
			}
			catch (CloudFoundryException cfe) {
				cloudException = cfe;
			}
			catch (RestClientException rce) {
				// Although CloudFoundryException is subtype of
				// RestClientException,
				// Catch any RCE that aren't CFE, and quit any further attempts.
				error = rce;
				break;
			}

			if (cloudException != null) {
				// Client may not be logged in. Log in and try again
				error = cloudException;
				if (handler.shouldAttemptClientLogin(cloudException)) {
					handler.login(progress);
					timeTaken = ONE_SECOND_INTERVAL * 2;
				}
				else if (!CloudErrorUtil.isAppStoppedStateError(cloudException)) {
					// Some other error encountered should
					// stop any further
					// retries
					break;
				}
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
