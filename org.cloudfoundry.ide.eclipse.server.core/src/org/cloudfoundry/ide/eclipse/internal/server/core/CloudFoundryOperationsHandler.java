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

import java.net.MalformedURLException;
import java.net.URL;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

public class CloudFoundryOperationsHandler {

	private final CloudFoundryOperations operations;

	private final String cloudURL;

	private static final String DEFAULT_PROGRESS_LABEL = "Performing Cloud Foundry operation";

	private static final int DEFAULT_PROGRESS_TICKS = 100;

	/**
	 * 
	 * @param operations must not be null
	 * @param cloudServer can be null if no server has been created yet
	 */
	public CloudFoundryOperationsHandler(CloudFoundryOperations operations, String cloudURL) {
		this.operations = operations;
		this.cloudURL = cloudURL;
	}

	public boolean login(IProgressMonitor monitor) throws CoreException {
		return login(monitor, 1, 0);
	}

	public boolean login(IProgressMonitor monitor, int tries, long sleep) throws CoreException {

		updateProxyInClient(operations);
		return internalLogin(monitor, tries, sleep);

	}

	protected boolean internalLogin(IProgressMonitor monitor, int tries, long sleep) throws CoreException {
		Boolean result = new WaitWithProgressJob(tries, sleep) {

			@Override
			protected boolean internalRunInWait(IProgressMonitor monitor) throws CoreException {
				try {
					operations.login();
					return true;
				}
				catch (CloudFoundryException cfe) {
					throw CloudUtil.toCoreException(cfe);
				}
				catch (RestClientException rce) {
					throw CloudUtil.toCoreException(rce);
				}
			}

			@Override
			protected boolean shouldRetryOnError(Throwable t) {
				return (t instanceof CloudFoundryException) && shouldAttemptClientLogin((CloudFoundryException) t);
			}

		}.run(monitor);
		return result.booleanValue();
	}

	protected SubMonitor getProgressMonitor(IProgressMonitor progressMonitor) {
		return progressMonitor instanceof SubMonitor ? (SubMonitor) progressMonitor : SubMonitor.convert(
				progressMonitor, DEFAULT_PROGRESS_LABEL, DEFAULT_PROGRESS_TICKS);
	}

	public boolean run(IProgressMonitor progressMonitor) throws CoreException {
		SubMonitor subMonitor = getProgressMonitor(progressMonitor);
		// Always check if proxy settings have changed.
		updateProxyInClient(operations);

		boolean succeeded = false;
		try {
			doRun(operations, subMonitor);
		}
		catch (CloudFoundryException e) {
			// try again in case of a login failure
			if (shouldAttemptClientLogin(e)) {
				int tries = 2;
				long wait = 500;
				internalLogin(progressMonitor, tries, wait);
				doRun(operations, subMonitor);
			}
			else {
				throw CloudUtil.toCoreException(e);
			}
		}
		catch (RestClientException rce) {
			throw CloudUtil.toCoreException(rce);
		}
		return succeeded;
	}

	protected void doRun(CloudFoundryOperations operations, SubMonitor progressMonitor) throws CoreException {
	
	}

	public boolean shouldAttemptClientLogin(CloudFoundryException cfe) {
		if (HttpStatus.FORBIDDEN.equals(cfe.getStatusCode())) {
			return true;
		}
		else if (HttpStatus.UNAUTHORIZED.equals(cfe.getStatusCode()) && operations.supportsSpaces()) {
			return true;
		}
		else {
			return false;
		}
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
