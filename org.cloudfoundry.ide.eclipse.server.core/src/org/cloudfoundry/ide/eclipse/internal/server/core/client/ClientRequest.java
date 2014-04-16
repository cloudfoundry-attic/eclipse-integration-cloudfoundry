/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryLoginHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * A Request performs a CF request via the cloudfoundry-client-lib API. It
 * performs various error handling and connection checks that are generally
 * common to most client requests, and therefore any client request should be
 * wrapped around a Request.
 * <p/>
 * By default, the set of client calls in the Request is made twice, the first
 * time it is executed immediate. If it fails due to connection error error, it
 * will attempt a second time.
 * <p/>
 * Subtypes can modify this behaviour and add conditions that will result in
 * further retries aside from connection errors.
 * 
 * 
 * @param <T>
 * 
 */
public abstract class ClientRequest<T> {

	private static final String NO_CLIENT_ERROR = "No Cloud Foundry client available to process the following request: {0} ";

	private static final String UNKNOWN_REQUEST_ERROR = "Unknown Cloud Foundry plugin error while trying to perform client call in {0}";

	/**
	 * 
	 */
	private final String label;

	public ClientRequest(String label) {
		Assert.isNotNull(label);
		this.label = label;
	}

	/**
	 * 
	 * @return result of client operation
	 * @throws CoreException if failure occurred while attempting to execute the
	 * client operation.
	 */
	public T run(IProgressMonitor monitor) throws CoreException {

		SubMonitor subProgress = SubMonitor.convert(monitor, label, 100);

		CloudFoundryOperations client = getClient(subProgress);
		if (client == null) {
			throw CloudErrorUtil.toCoreException(NLS.bind(NO_CLIENT_ERROR, label));
		}
		T result;
		try {
			result = runAsClientRequestCheckConnection(client, subProgress);
		}
		catch (CoreException ce) {
			// Translate to a error message that the user can understand
			String connectionError = CloudErrorUtil.getConnectionError(ce);
			if (connectionError != null) {
				ce = new CoreException(CloudFoundryPlugin.getErrorStatus(connectionError));
			}
			throw ce;
		}
		finally {
			subProgress.done();
		}

		return result;
	}

	/**
	 * Attempts to execute the client request by first checking proxy settings,
	 * and if unauthorised/forbidden exceptions thrown the first time, will
	 * attempt to log in. If that succeeds, it will attempt one more time.
	 * Otherwise it will fail and not attempt the request any further.
	 * @param client
	 * @param cloudServer
	 * @param subProgress
	 * @return
	 * @throws CoreException if attempt to execute failed, even after a second
	 * attempt after a client login.
	 */
	protected T runAsClientRequestCheckConnection(CloudFoundryOperations client, SubMonitor subProgress)
			throws CoreException {
		// Check that a user is logged in and proxy is updated
		String cloudURL = getCloudServerUrl();
		CloudFoundryLoginHandler handler = new CloudFoundryLoginHandler(client, cloudURL);
		try {
			// Always check if proxy settings have changed.
			handler.updateProxyInClient(client);
		}
		catch (CoreException e) {
			// Failed to handle proxy change. Do not stop the operation, but log
			// the error to let the user know of the proxy change failure
			CloudFoundryPlugin.logError(e.getMessage());
		}

		try {
			return runAndWait(client, subProgress);
		}
		catch (CoreException ce) {
			CloudFoundryException cfe = ce.getCause() instanceof CloudFoundryException ? (CloudFoundryException) ce
					.getCause() : null;
			if (cfe != null && handler.shouldAttemptClientLogin(cfe)) {
				handler.login(subProgress, 3, CloudOperationsConstants.LOGIN_INTERVAL);
				return runAndWait(client, subProgress);
			}
			throw ce;
		}
	}

	protected T runAndWait(CloudFoundryOperations client, SubMonitor subProgress) throws CoreException {
		Throwable error = null;

		boolean reattempt = true;
		long timeLeft = getTotalTimeWait();
		while (reattempt) {

			long interval = -1;

			try {
				return doRun(client, subProgress);
			}
			catch (Throwable e) {
				error = e;
			}

			interval = getWaitInterval(error, subProgress);
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
			error = new CoreException(CloudFoundryPlugin.getErrorStatus(NLS.bind(UNKNOWN_REQUEST_ERROR,
					ClientRequest.class.getName())));
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

	/**
	 * Perform the actual client operation. The client is guaranteed to be
	 * non-null at this stage.
	 * @param client non-null client
	 * @param progress
	 * @return result of operation.
	 * @throws CoreException
	 */
	protected abstract T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException;

	/**
	 * This must never be null. This is the client used to perform operations.
	 * @return Non-null Java client.
	 * @throws CoreException if failed to obtain a client
	 */
	protected abstract CloudFoundryOperations getClient(IProgressMonitor monitor) throws CoreException;

	/**
	 * 
	 * @return the Cloud Foundry server URL. Can be null, but if null, some
	 * request checks like proxy handling may not be available.
	 * @throw {@link CoreException} if error occurred while resolving server URL
	 */
	protected abstract String getCloudServerUrl() throws CoreException;

	/**
	 * Total amount of time to wait. If less than the wait interval length, only
	 * one attempt will be made {@link #getWaitInterval(Throwable, SubMonitor)}
	 * @return
	 */
	protected long getTotalTimeWait() {
		return CloudOperationsConstants.DEFAULT_CF_CLIENT_REQUEST_TIMEOUT;
	}

}