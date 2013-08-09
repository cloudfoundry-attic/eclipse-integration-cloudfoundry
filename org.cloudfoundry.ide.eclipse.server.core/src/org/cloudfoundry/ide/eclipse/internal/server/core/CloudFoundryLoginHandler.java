/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.net.MalformedURLException;
import java.net.URL;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.springframework.http.HttpStatus;

public class CloudFoundryLoginHandler {

	private final CloudFoundryOperations operations;

	private final String cloudURL;

	private static final String DEFAULT_PROGRESS_LABEL = "Performing Cloud Foundry operation";

	private static final int DEFAULT_PROGRESS_TICKS = 100;

	/**
	 * 
	 * @param operations must not be null
	 * @param cloudServer can be null if no server has been created yet
	 */
	public CloudFoundryLoginHandler(CloudFoundryOperations operations, String cloudURL) {
		this.operations = operations;
		this.cloudURL = cloudURL;
	}

	/**
	 * Attempts to log in once. If login fails, Core exception is thrown
	 * @throws CoreException if login failed. The reason for the login failure
	 * is contained in the core exception's
	 */
	public void login(IProgressMonitor monitor) throws CoreException {
		login(monitor, 1, 0);
	}

	/**
	 * Attempts a log in for the specified amount of attempts, and waits by the
	 * specified sleep time between each attempt. If at the end of the attempts,
	 * login has failed, Core exception is thrown.
	 */
	public void login(IProgressMonitor monitor, int tries, long sleep) throws CoreException {
		internalLogin(monitor, tries, sleep);
	}

	protected boolean internalLogin(IProgressMonitor monitor, int tries, long sleep) throws CoreException {
		Boolean result = new WaitWithProgressJob(tries, sleep) {

			@Override
			protected boolean internalRunInWait(IProgressMonitor monitor) throws CoreException {
				// Do not wrap CloudFoundryException or RestClientException in a
				// CoreException.
				// as they are uncaught exceptions and can be inspected directly
				// by the shouldRetryOnError(..) method.
				operations.login();
				return true;
			}

			@Override
			protected boolean shouldRetryOnError(Throwable t) {
				return (t instanceof CloudFoundryException) && shouldAttemptClientLogin((CloudFoundryException) t);
			}

		}.run(monitor);
		return result != null ? result.booleanValue() : false;
	}

	protected SubMonitor getProgressMonitor(IProgressMonitor progressMonitor) {
		return progressMonitor instanceof SubMonitor ? (SubMonitor) progressMonitor : SubMonitor.convert(
				progressMonitor, DEFAULT_PROGRESS_LABEL, DEFAULT_PROGRESS_TICKS);
	}

	public boolean shouldAttemptClientLogin(CloudFoundryException cfe) {
		return HttpStatus.UNAUTHORIZED.equals(cfe.getStatusCode()) || HttpStatus.FORBIDDEN.equals(cfe.getStatusCode());
	}

	/**
	 * 
	 * @return true if there was a proxy update. False any other case.
	 * @throws CoreException
	 */
	public boolean updateProxyInClient(CloudFoundryOperations client) throws CoreException {
		if (client != null && cloudURL != null) {
			try {
				URL actualUrl = new URL(cloudURL);
				HttpProxyConfiguration proxyConfiguration = CloudFoundryClientFactory.getProxy(actualUrl);

				client.updateHttpProxyConfiguration(proxyConfiguration);
				return true;
			}
			catch (MalformedURLException e) {
				// Ignore. If URL is incorrect, other
				// mechanisms exit to prompt user for correct values.
			}
		}
		return false;
	}

}
