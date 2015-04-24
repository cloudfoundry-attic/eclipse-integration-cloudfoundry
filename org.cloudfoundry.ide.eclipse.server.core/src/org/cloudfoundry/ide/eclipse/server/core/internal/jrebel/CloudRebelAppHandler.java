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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerListener;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.ExternalRestTemplate;
import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ModuleChangeEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.AppUrlChangeEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.osgi.framework.Bundle;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CloudRebelAppHandler implements CloudServerListener {

	private static CloudRebelAppHandler handler;

	public static final String ID_MODULE_STANDALONE = "cloudfoundry.standalone.app"; //$NON-NLS-1$

	private CloudRebelAppHandler() {
		ServerEventHandler.getDefault().addServerListener(this);
	}

	public static void init() {
		if (handler == null) {
			handler = new CloudRebelAppHandler();
		}
	}

	/**
	 * Checks if remoting is enabled for the given module. The module's
	 * associated deployed application start state is checked, as remoting can
	 * only be enabled if the application is running. In addition, checks are
	 * performed to see if a remoting agent is available in the Cloud
	 * environment.
	 * @param module for a deployed application.
	 * @param server where application is deployed
	 * @return true if remoting is enabled. False otherwise.
	 */
	protected boolean isRemotingEnabled(IModule module, CloudServerEvent event, IProgressMonitor monitor) {
		CloudFoundryApplicationModule appModule = null;
		CloudFoundryServer server = event.getServer();
		try {

			// Fetch the cached module first to avoid Cloud requests
			appModule = server.getExistingCloudModule(module);

			int attempts = 100;

			SubMonitor subMonitor = SubMonitor.convert(monitor, 100 * (attempts + 1));

			// Otherwise request for an updated module
			if (appModule == null) {
				appModule = server.getBehaviour().updateCloudModule(module, subMonitor.newChild(100));
			}

			CloudApplication cloudApp = appModule != null ? appModule.getApplication() : null;

			// If app does not exist or app is not started do not check for the
			// agent. The only exception is if the event was triggered by an app
			// start operation itself, at which
			// point part of waiting for the agent to be available is to also
			// wait for the app to start
			if (cloudApp == null
					|| (event.getType() != CloudServerEvent.EVENT_APP_STARTED && cloudApp.getState() != AppState.STARTED)) {
				return false;
			}

			if (appModule != null) {
				printToConsole(appModule, server,
						NLS.bind(Messages.CloudRebelAppHandler_FINDING_REMOTING_AGENT, module.getName()));
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
			throw CloudErrorUtil.toCoreException("No application module found. Application may no longer exist."); //$NON-NLS-1$
		}
		if (appModule.getDeploymentInfo() == null) {
			return false;
		}
		List<String> urls = appModule.getDeploymentInfo().getUris();

		try {
			String url = urls != null && !urls.isEmpty() ? "http://" + urls.get(0) + "/app" : null; //$NON-NLS-1$ //$NON-NLS-2$
			if (url != null) {

				HttpHeaders headers = new HttpHeaders();
				String id = appModule.getLocalModule().getProject() != null ? appModule.getLocalModule().getProject()
						.getName() : "random"; //$NON-NLS-1$
				headers.set("x-rebel-id", id); //$NON-NLS-1$ 

				HttpEntity<Object> requestEntity = new HttpEntity<Object>(headers);
				ResponseEntity<String> responseEntity = new ExternalRestTemplate().exchange(url, HttpMethod.POST,
						requestEntity, String.class);
				if (responseEntity != null && responseEntity.getHeaders() != null
						&& responseEntity.getHeaders().containsKey("x-rebel-response")) { //$NON-NLS-1$ 

					printToConsole(appModule, cloudServer, Messages.CloudRebelAppHandler_FOUND_REMOTING_AGENT);
					return true;
				}
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

		if (event.getServer() != null && event.getServer().jrebelAutomaticAppUrlSynch()
				&& (shouldAddRemotingUrl(event) || shouldRemoveRemotingUrl(event))) {

			List<IModule> modules = new ArrayList<IModule>();

			if (event instanceof ModuleChangeEvent) {
				final ModuleChangeEvent moduleEvent = (ModuleChangeEvent) event;

				final IModule module = moduleEvent.getModule();

				if (module != null) {
					modules.add(module);
				}
			}
			else if (event.getType() == CloudServerEvent.EVENT_SERVER_CONNECTED
					|| event.getType() == CloudServerEvent.EVENT_SERVER_DISCONNECTED) {
				IModule[] mods = event.getServer().getServer().getModules();
				if (mods != null) {
					for (IModule module : mods) {
						modules.add(module);
					}
				}
			}

			if (!modules.isEmpty()) {
				updateModulesWithRebelProjects(modules, event);
			}
		}
	}

	protected void updateModulesWithRebelProjects(List<IModule> modules, CloudServerEvent event) {

		if (modules == null) {
			return;
		}

		for (IModule module : modules) {
			final IProject project = module.getProject();
			final IModule appModule = module;
			final CloudServerEvent moduleEvent = event;
			try {
				// Only check remoting agent if it is a JRebel project as the
				// remoting check may
				// require multiple requests to the Cloud
				if (project != null && project.isAccessible()
						&& project.hasNature("org.zeroturnaround.eclipse.remoting.remotingNature") //$NON-NLS-1$
						&& project.hasNature("org.zeroturnaround.eclipse.jrebelNature")) { //$NON-NLS-1$

					Job job = new Job(NLS.bind(Messages.CloudRebelAppHandler_FINDING_REMOTING_AGENT, module.getName())) {

						@Override
						protected IStatus run(IProgressMonitor monitor) {

							// Only check the presence of remoting agent if
							// adding URL, which requires the application to be
							// started. Removing URL does should not check
							// for the presence of the agent as the app would be
							// stopped and agent not available.
							if (!shouldAddRemotingUrl(moduleEvent)
									|| isRemotingEnabled(appModule, moduleEvent, monitor)) {
								try {
									handleRebelProject(moduleEvent, appModule);
								}
								catch (CoreException e) {
									CloudFoundryPlugin.logError(e);
									return e.getStatus();
								}
							}

							return Status.OK_STATUS;
						}

					};

					job.schedule();
				}
			}
			catch (CoreException ce) {
				CloudFoundryPlugin.logError(ce);
			}
		}
	}

	protected void handleRebelProject(CloudServerEvent event, IModule module) throws CoreException {
		CloudFoundryServer server = event.getServer();
		Bundle bundle = Platform.getBundle("org.zeroturnaround.eclipse.remoting"); //$NON-NLS-1$

		if (bundle != null) {
			Throwable error = null;
			try {

				Class<?> providerClass = bundle
						.loadClass("org.zeroturnaround.eclipse.jrebel.remoting.RebelRemotingProvider"); //$NON-NLS-1$

				if (providerClass != null) {

					CloudFoundryApplicationModule appModule = server.getExistingCloudModule(module);

					Method getRemotingProject = providerClass.getMethod("getRemotingProject", IProject.class); //$NON-NLS-1$

					if (getRemotingProject != null) {

						getRemotingProject.setAccessible(true);

						// static method
						IProject project = module.getProject();
						Object remoteProjectObj = getRemotingProject.invoke(null, project);
						if (remoteProjectObj != null
								&& remoteProjectObj.getClass().getName()
										.equals("org.zeroturnaround.eclipse.jrebel.remoting.RemotingProject")) { //$NON-NLS-1$

							// If the app is started, check that rebel.xml is
							// configured correctly for spring boot applications

							if (event.getType() == CloudServerEvent.EVENT_APP_STARTED && isSpringBootJar(appModule)) {
								configureSpringBootJarApp(project);
							}

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
							if (event instanceof AppUrlChangeEvent) {
								AppUrlChangeEvent appUrlEvent = (AppUrlChangeEvent) event;
								if (appUrlEvent.getChangedUrls() != null) {
									currentAppUrls = appUrlEvent.getChangedUrls();
									currentAppUrls.addAll(appUrlEvent.getChangedUrls());
								}
								if (appUrlEvent.getOldUrls() != null) {
									oldAppUrls.addAll(appUrlEvent.getOldUrls());
								}
							}
							else if (appModule != null && appModule.getDeploymentInfo() != null) {
								currentAppUrls.addAll(appModule.getDeploymentInfo().getUris());
							}

							List<URL> updatedRebelUrls = new ArrayList<URL>();

							boolean shouldSetUrls = false;
							if (shouldAddRemotingUrl(event)) {

								shouldSetUrls = true;
								printToConsole(appModule, server, Messages.CloudRebelAppHandler_ADDING_URL);
								// Remove old app URLs
								for (URL rebelUrl : existingRebelUrls) {
									String authority = rebelUrl.getAuthority();
									if (!oldAppUrls.contains(authority)) {
										updatedRebelUrls.add(rebelUrl);
									}
								}

								// Add new app URLs
								for (String appUrl : currentAppUrls) {
									if (!appUrl.startsWith("http://") && !appUrl.startsWith("https://")) { //$NON-NLS-1$ //$NON-NLS-2$
										appUrl = "http://" + appUrl; //$NON-NLS-1$
									}
									try {
										URL appURL = new URL(appUrl);
										if (!updatedRebelUrls.contains(appURL)) {
											updatedRebelUrls.add(appURL);
										}
									}
									catch (MalformedURLException e) {
										CloudFoundryPlugin.logError(e);
									}
								}
							}
							else if (shouldRemoveRemotingUrl(event)) {

								shouldSetUrls = true;
								printToConsole(appModule, server, Messages.CloudRebelAppHandler_REMOVING_URL);

								for (URL rebelUrl : existingRebelUrls) {
									String authority = rebelUrl.getAuthority();
									// If deleting or removing an application,
									// remove the URL from JRebel
									if (!currentAppUrls.contains(authority)) {
										updatedRebelUrls.add(rebelUrl);
									}
								}
							}

							if (shouldSetUrls) {
								Method setRebelRemotingUrls = remoteProjectObj.getClass().getDeclaredMethod(
										"setRemoteUrls", URL[].class); //$NON-NLS-1$

								if (setRebelRemotingUrls != null) {
									setRebelRemotingUrls.setAccessible(true);
									setRebelRemotingUrls.invoke(remoteProjectObj,
											new Object[] { updatedRebelUrls.toArray(new URL[0]) });
									printToConsole(appModule, server, Messages.CloudRebelAppHandler_UPDATED_URL);
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

	protected void configureSpringBootJarApp(IProject project) {

		// rebel.xml is overwritten for Spring Boot Jar apps to skip the /lib
		// folder which
		// is only generated in the Spring Boot Jar but has no workspace
		// equivalent

		try {
			// Only overwrite the rebel.xml if auto-generation is enabled. Don't
			// change rebel.xml if auto-generate is off as to avoid
			// changing user-specified changes to the file
			String value = project.getPersistentProperty(new QualifiedName("org.zeroturnaround.eclipse.jrebel",//$NON-NLS-1$
					"autoGenerateRebelXml")); //$NON-NLS-1$
			if ("false".equals(value)) {//$NON-NLS-1$
				return;
			}

			IFile file = project.getFile("/src/main/resources/rebel.xml"); //$NON-NLS-1$
			if (file.isAccessible()) {
				String path = file.getRawLocation() != null ? file.getRawLocation().toString() : null;

				if (path != null) {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

					DocumentBuilder db = factory.newDocumentBuilder();
					Document doc = db.parse(new File(path));

					Element binElement = null;
					NodeList nodeList = doc.getElementsByTagName("*"); //$NON-NLS-1$
					if (nodeList != null) {

						for (int i = 0; i < nodeList.getLength(); i++) {
							Node node = nodeList.item(i);
							if ((node instanceof Element) && node.getNodeName().equals("dir")) { //$NON-NLS-1$
								Element el = (Element) node;
								String att = el.getAttribute("name"); //$NON-NLS-1$
								if (att != null && att.endsWith("/bin")) { //$NON-NLS-1$
									binElement = el;
									break;
								}
							}
						}

						if (binElement != null) {
							NodeList binChildren = binElement.getChildNodes();
							Element existingExcludeLib = null;
							Element existingExcludeJavabuildpack = null;
							if (binChildren != null) {

								for (int i = 0; i < binChildren.getLength(); i++) {
									Node node = binChildren.item(i);
									if (node.getNodeName().equals("exclude") && node instanceof Element) { //$NON-NLS-1$
										Element excludeElement = (Element) node;
										Attr attr = excludeElement.getAttributeNode("name"); //$NON-NLS-1$
										if (attr != null) {
											if (attr.getNodeValue().equals("lib/**")) { //$NON-NLS-1$
												existingExcludeLib = excludeElement;
											}
											else if (attr.getNodeValue().equals(".java-buildpack/**")) { //$NON-NLS-1$
												existingExcludeJavabuildpack = excludeElement;
											}
										}
									}
								}
							}

							Element updatedExcludeLib = null;
							if (existingExcludeLib == null) {
								updatedExcludeLib = doc.createElement("exclude"); //$NON-NLS-1$
								updatedExcludeLib.setAttribute("name", "lib/**"); //$NON-NLS-1$ //$NON-NLS-2$
								binElement.appendChild(updatedExcludeLib);
							}
							Element updatedExcludeJavabuildpack = null;
							if (existingExcludeJavabuildpack == null) {
								updatedExcludeJavabuildpack = doc.createElement("exclude"); //$NON-NLS-1$
								updatedExcludeJavabuildpack.setAttribute("name", ".java-buildpack/**"); //$NON-NLS-1$ //$NON-NLS-2$
								binElement.appendChild(updatedExcludeJavabuildpack);
							}
							if (updatedExcludeLib != null || updatedExcludeJavabuildpack != null) {

								// If replacing the exist rebel.xml file, be
								// sure to switch off automatic rebel.xml
								// generation
								project.setPersistentProperty(new QualifiedName("org.zeroturnaround.eclipse.jrebel",//$NON-NLS-1$
										"autoGenerateRebelXml"), "false"); //$NON-NLS-1$ //$NON-NLS-2$

								Transformer transformer = TransformerFactory.newInstance().newTransformer();
								transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
								DOMSource source = new DOMSource(doc);
								StreamResult console = new StreamResult(new File(path));
								transformer.transform(source, console);
							}
						}
					}
				}
			}
		}
		catch (ParserConfigurationException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (SAXException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (IOException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (TransformerException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	protected boolean isSpringBootJar(CloudFoundryApplicationModule appModule) {

		if (CloudUtil.isBootApp(appModule)) {
			IProject project = appModule.getLocalModule().getProject();
			if (project != null) {
				IProjectFacet facet = ProjectFacetsManager.getProjectFacet(ID_MODULE_STANDALONE);
				try {
					IFacetedProject facetedProject = ProjectFacetsManager.create(project);
					return facetedProject != null && facetedProject.hasProjectFacet(facet);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.log(e);
				}
			}
		}
		return false;
	}

	protected boolean shouldAddRemotingUrl(CloudServerEvent event) {
		return event != null
				&& (event.getType() == CloudServerEvent.EVENT_APP_URL_CHANGED
						|| event.getType() == CloudServerEvent.EVENT_APP_STARTED || event.getType() == CloudServerEvent.EVENT_SERVER_CONNECTED);
	}

	protected boolean shouldRemoveRemotingUrl(CloudServerEvent event) {
		return event != null
				&& (event.getType() == CloudServerEvent.EVENT_APP_DELETED
						|| event.getType() == CloudServerEvent.EVENT_APP_STOPPED || event.getType() == CloudServerEvent.EVENT_SERVER_DISCONNECTED);
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
}
