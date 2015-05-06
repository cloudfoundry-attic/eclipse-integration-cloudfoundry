/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.core.internal.jrebel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerListener;
import org.cloudfoundry.ide.eclipse.server.core.internal.ExternalRestTemplate;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ModuleChangeEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.AppUrlChangeEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.osgi.framework.Bundle;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

public abstract class CloudRebelAppHandler implements CloudServerListener {

	private static final String ERROR_NO_APP = "No application module found. Application may no longer exist in the Cloud."; //$NON-NLS-1$

	abstract public void register();

	/**
	 * Checks if remoting is enabled for the given module. The module's
	 * associated deployed application start state is checked, as remoting can
	 * only be enabled if the application is running. In addition, checks are
	 * performed to see if a remoting agent is available in the Cloud runtime
	 * 
	 */
	protected boolean isRemotingAgentRunning(IModule module, CloudFoundryServer server, int eventType,
			IProgressMonitor monitor) {
		CloudFoundryApplicationModule appModule = null;
		try {

			// Fetch the cached module first to avoid Cloud requests
			appModule = server.getExistingCloudModule(module);

			int attempts = 40;

			SubMonitor subMonitor = SubMonitor.convert(monitor, 100 * (attempts + 1));

			// Otherwise request for an updated module
			if (appModule == null) {
				appModule = server.getBehaviour().updateCloudModule(module, subMonitor.newChild(100));
			}

			CloudApplication cloudApp = appModule != null ? appModule.getApplication() : null;

			// Cloud application doesn't exist, no need to check agent
			if (cloudApp == null) {
				CloudFoundryPlugin.logError(ERROR_NO_APP);
				return false;
			}

			while (attempts > 0 && !subMonitor.isCanceled()) {

				if (findRemotingAgent(appModule, server)) {
					return true;
				}
				try {
					Thread.sleep(3000);
				}
				catch (InterruptedException e) {
					// Ignore. Proceed to get updated module anyway
				}
				appModule = server.getBehaviour().updateCloudModule(module, subMonitor.newChild(100));

				attempts--;
			}
			if (!subMonitor.isCanceled()) {
				printErrorToConsole(appModule, server, Messages.CloudRebelAppHandler_TIME_OUT_RESOLVING_REMOTING_AGENT);
				CloudFoundryPlugin.logError(Messages.CloudRebelAppHandler_TIME_OUT_RESOLVING_REMOTING_AGENT);
			}
		}
		catch (CoreException e) {
			printErrorToConsole(appModule, server, NLS.bind(Messages.CloudRebelAppHandler_ERROR, e.getMessage()));
			CloudFoundryPlugin.logError(e);
		}
		return false;
	}

	protected boolean findRemotingAgent(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer)
			throws CoreException {
		if (appModule == null) {
			throw CloudErrorUtil.toCoreException(ERROR_NO_APP);
		}
		if (appModule.getDeploymentInfo() == null) {
			return false;
		}
		List<String> urls = appModule.getDeploymentInfo().getUris();

		try {
			String url = urls != null && !urls.isEmpty() ? "http://" + urls.get(0) + "/app" : null; //$NON-NLS-1$ //$NON-NLS-2$
			if (url != null) {

				HttpHeaders headers = new HttpHeaders();
				headers.set("x-rebel-id", "random"); //$NON-NLS-1$ //$NON-NLS-2$

				HttpEntity<Object> requestEntity = new HttpEntity<Object>(headers);
				ResponseEntity<String> responseEntity = new ExternalRestTemplate().exchange(url, HttpMethod.POST,
						requestEntity, String.class);

				return responseEntity != null && responseEntity.getHeaders() != null
						&& responseEntity.getHeaders().containsKey("x-rebel-response");//$NON-NLS-1$ 
			}
		}
		catch (RestClientException e) {
			// Skip 404 errors..they may require retrying to connect to the
			// agent until it becomes available.
			if (!CloudErrorUtil.isNotFoundException(e)) {
				throw CloudErrorUtil.toCoreException(e);
			}
		}
		return false;
	}

	@Override
	public void serverChanged(CloudServerEvent event) {

		if (event.getServer() != null
				&& (event.getType() == CloudServerEvent.EVENT_JREBEL_REMOTING_UPDATE || shouldReplaceRemotingUrl(event
						.getType()))) {

			List<IModule> modules = new ArrayList<IModule>();

			if (event instanceof ModuleChangeEvent) {
				final ModuleChangeEvent moduleEvent = (ModuleChangeEvent) event;

				final IModule module = moduleEvent.getModule();

				if (module != null) {
					modules.add(module);
				}
			}

			if (!modules.isEmpty()) {
				updateModulesWithRebelProjects(modules, event);
			}
		}
	}

	/**
	 * 
	 * @param modules must not be null.
	 * @param event
	 */
	protected void updateModulesWithRebelProjects(List<IModule> modules, CloudServerEvent event) {

		for (IModule module : modules) {
			final IModule mod = module;
			final CloudServerEvent moduleEvent = event;
			// Only check remoting agent if it is a JRebel project as the
			// remoting check may
			// require multiple requests to the Cloud and may be a slow running
			// operation
			if (isJRebelEnabled(module)) {

				Job job = new Job(NLS.bind(Messages.CloudRebelAppHandler_UPDATING_JREBEL_REMOTING, module.getName())) {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							handleRebelProject(moduleEvent, mod, monitor);
						}
						catch (CoreException e) {
							CloudFoundryPlugin.logError(e);
							return e.getStatus();
						}

						return Status.OK_STATUS;
					}
				};

				job.schedule();
			}
		}
	}

	protected void handleRebelProject(CloudServerEvent event, IModule module, IProgressMonitor monitor)
			throws CoreException {
		CloudFoundryServer cloudServer = event.getServer();
		CloudFoundryApplicationModule cloudAppModule = cloudServer.getExistingCloudModule(module);

		int eventType = event.getType();
		List<String> oldUrls = null;
		List<String> currentUrls = null;
		if (event instanceof AppUrlChangeEvent) {
			AppUrlChangeEvent appUrlEvent = (AppUrlChangeEvent) event;
			oldUrls = appUrlEvent.getOldUrls();
			currentUrls = appUrlEvent.getCurrentUrls();
		}
		else if (cloudAppModule != null && cloudAppModule.getDeploymentInfo() != null) {
			currentUrls = cloudAppModule.getDeploymentInfo().getUris();
		}

		updateJRebelRemoting(cloudServer, eventType, module, oldUrls, currentUrls, monitor);
	}

	/**
	 * Only gets invoked if the module has an associated JRebel-enabled project.
	 * Therefore the module's associated project exists and is accessible.
	 */
	protected void updateJRebelRemoting(CloudFoundryServer cloudServer, int cloudEventType, IModule module,
			List<String> oldUrls, List<String> currentUrls, IProgressMonitor monitor) throws CoreException {

		Bundle bundle = getJRebelBundle();
		if (bundle != null) {
			Throwable error = null;
			try {

				Class<?> providerClass = bundle
						.loadClass("org.zeroturnaround.eclipse.jrebel.remoting.RebelRemotingProvider"); //$NON-NLS-1$

				if (providerClass != null) {

					CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

					Method getRemotingProject = providerClass.getMethod("getRemotingProject", IProject.class); //$NON-NLS-1$

					if (getRemotingProject != null) {

						getRemotingProject.setAccessible(true);

						// static method
						IProject project = module.getProject();
						Object remoteProjectObj = getRemotingProject.invoke(null, project);
						if (remoteProjectObj != null
								&& remoteProjectObj.getClass().getName()
										.equals("org.zeroturnaround.eclipse.jrebel.remoting.RemotingProject")) { //$NON-NLS-1$

							// This includes ALL URLs, not just the one
							// pertaining to the application project. It may
							// contain
							// deployment URLs for other projects, including
							// those not deployed to Cloud Foundry
							URL[] existingRebelUrls = null;
							Method getUrls = remoteProjectObj.getClass().getMethod("getRemoteUrls"); //$NON-NLS-1$
							if (getUrls != null) {
								getUrls.setAccessible(true);
								Object urlList = getUrls.invoke(remoteProjectObj);
								if (urlList instanceof URL[]) {
									existingRebelUrls = (URL[]) urlList;
								}
							}
							if (existingRebelUrls == null) {
								existingRebelUrls = new URL[0];
							}

							List<String> currentAppUrls = new ArrayList<String>();
							List<String> oldAppUrls = new ArrayList<String>();

							if (currentUrls != null) {
								currentAppUrls.addAll(currentUrls);
							}
							if (oldUrls != null) {
								oldAppUrls.addAll(oldUrls);
							}

							List<URL> updatedRebelUrls = new ArrayList<URL>();

							if (shouldReplaceRemotingUrl(cloudEventType)
									|| cloudEventType == CloudServerEvent.EVENT_JREBEL_REMOTING_UPDATE) {

								if (!currentAppUrls.isEmpty()) {

									boolean changed = false;

									// Remove obsolete app URLs. Do checks
									// against
									// the
									// authority instead of the full
									// URL, as URLs may not be identical between
									// JRebel and the application, even if the
									// authorities are.
									for (URL rebelUrl : existingRebelUrls) {
										String authority = rebelUrl.getAuthority();
										if (oldAppUrls.contains(authority) && !currentAppUrls.contains(authority)) {
											// Skip adding back the obsolete
											// URL.
											changed = true;
										}
										else {
											updatedRebelUrls.add(rebelUrl);
										}
									}

									// Now perform the following:
									// 1. Check if another app URL is registered
									// with JRebel remoting
									// 2. If not, add the first encountered app
									// URL to JRebel remoting. Otherwise, make
									// no changes to JRebel remoting

									String urlToAdd = currentAppUrls.get(0);

									// Check if any of the current app URLs are
									// in JRebel Remoting. If so, no need to add
									// a URL
									for (URL updatedRebelUrl : updatedRebelUrls) {
										String updatedRebelAuthority = updatedRebelUrl.getAuthority();

										if (currentAppUrls.contains(updatedRebelAuthority)) {
											urlToAdd = null;
											break;
										}
									}
									if (urlToAdd != null) {
										// Add the first new URL encountered
										if (!urlToAdd.startsWith("http://") && !urlToAdd.startsWith("https://")) { //$NON-NLS-1$ //$NON-NLS-2$
											urlToAdd = "http://" + urlToAdd; //$NON-NLS-1$
										}
										try {
											URL toAdd = new URL(urlToAdd);
											updatedRebelUrls.add(toAdd);
											changed = true;
										}
										catch (MalformedURLException e) {
											throw CloudErrorUtil.toCoreException(e);
										}
									}

									if (changed) {
										Method setRebelRemotingUrls = remoteProjectObj.getClass().getDeclaredMethod(
												"setRemoteUrls", URL[].class); //$NON-NLS-1$

										if (setRebelRemotingUrls != null) {
											setRebelRemotingUrls.setAccessible(true);
											setRebelRemotingUrls.invoke(remoteProjectObj,
													new Object[] { updatedRebelUrls.toArray(new URL[0]) });
											printToConsole(appModule, cloudServer, NLS.bind(
													Messages.CloudRebelAppHandler_UPDATED_URL, updatedRebelUrls));
										}
									}
									else {
										printToConsole(appModule, cloudServer, Messages.CloudRebelAppHandler_UP_TO_DATE);
									}
								}
								else {
									throw CloudErrorUtil
											.toCoreException("No Cloud application deployment URL found for " + module.getName() + ". Unable to automatically set a deployment URL in JRebel remoting."); //$NON-NLS-1$ //$NON-NLS-2$
								}
							}
						}
					}
				}
			}
			catch (ClassNotFoundException e) {
				error = e;
			}
			catch (SecurityException e) {
				error = e;
			}
			catch (NoSuchMethodException e) {
				error = e;
			}
			catch (IllegalAccessException e) {
				error = e;
			}
			catch (InvocationTargetException e) {
				error = e;
			}
			catch (IllegalArgumentException e) {
				error = e;
			}

			if (error != null) {
				throw CloudErrorUtil.toCoreException(error);
			}
		}
	}

	protected boolean shouldReplaceRemotingUrl(int eventType) {
		return eventType == CloudServerEvent.EVENT_APP_URL_CHANGED;
	}

	protected void printToConsole(CloudFoundryApplicationModule appModule, CloudFoundryServer server, String message) {
		printToConsole(appModule, server, message, false);
	}

	protected void printErrorToConsole(CloudFoundryApplicationModule appModule, CloudFoundryServer server,
			String message) {
		printToConsole(appModule, server, message, true);
	}

	protected void printToConsole(CloudFoundryApplicationModule appModule, CloudFoundryServer server, String message,
			boolean error) {
		if (appModule != null && server != null) {
			message = Messages.CloudRebelAppHandler_MESSAGE_PREFIX + " - " + message + '\n'; //$NON-NLS-1$
			CloudFoundryPlugin.getCallback().printToConsole(server, appModule, message, false, error);
		}
	}

	public static boolean isJRebelEnabled(IModule module) {
		IProject project = module != null ? module.getProject() : null;

		try {
			return project != null && project.isAccessible()
					&& project.hasNature("org.zeroturnaround.eclipse.remoting.remotingNature") //$NON-NLS-1$
					&& project.hasNature("org.zeroturnaround.eclipse.jrebelNature"); //$NON-NLS-1$
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return false;
	}

	/**
	 * 
	 * @return true if JRebel bundle is found. False otherwise
	 */
	public static Bundle getJRebelBundle() {
		Bundle bundle = null;
		try {
			bundle = Platform.getBundle("org.zeroturnaround.eclipse.remoting"); //$NON-NLS-1$
		}
		catch (Throwable e) {
			CloudFoundryPlugin.logError(e);
		}

		return bundle;
	}

	/**
	 * 
	 * @return true if JRebel is installed in Eclipse. False otherwise.
	 */
	public static boolean isJRebelIDEInstalled() {
		return getJRebelBundle() != null;
	}
}
