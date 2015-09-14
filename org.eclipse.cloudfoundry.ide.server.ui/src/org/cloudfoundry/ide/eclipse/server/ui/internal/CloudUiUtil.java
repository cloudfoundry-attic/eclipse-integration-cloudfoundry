/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
 *     IBM - Switching to use the more generic AbstractCloudFoundryUrl
 *     		instead concrete CloudServerURL, deprecating non-recommended methods
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.ide.eclipse.server.core.AbstractCloudFoundryUrl;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.server.core.internal.spaces.CloudOrgsAndSpaces;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.WebBrowserPreference;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.springframework.web.client.ResourceAccessException;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Terry Denney
 */
@SuppressWarnings("restriction")
public class CloudUiUtil {

	public static final String SERVERS_VIEW_ID = "org.eclipse.wst.server.ui.ServersView"; //$NON-NLS-1$

	public static String ATTR_USER_DEFINED_URLS = "org.cloudfoundry.ide.eclipse.server.user.defined.urls"; //$NON-NLS-1$

	public static IStatus runForked(final ICoreRunnable coreRunner, IWizard wizard) {
		try {
			IRunnableWithProgress runner = new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						coreRunner.run(monitor);
					}
					catch (Exception e) {
						throw new InvocationTargetException(e);
					}
					finally {
						monitor.done();
					}
				}
			};
			wizard.getContainer().run(true, false, runner);
		}
		catch (InvocationTargetException e) {
			IStatus status;
			if (e.getCause() instanceof CoreException) {
				status = new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, NLS.bind(
						Messages.CloudUiUtil_ERROR_FORK_OP_FAILED, e.getCause().getMessage()), e);
			}
			else {
				status = new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, NLS.bind(
						Messages.CloudUiUtil_ERROR_FORK_UNEXPECTED, e.getMessage()), e);
			}
			CloudFoundryServerUiPlugin.getDefault().getLog().log(status);
			IWizardPage page = wizard.getContainer().getCurrentPage();
			if (page instanceof DialogPage) {
				((DialogPage) page).setErrorMessage(status.getMessage());
			}
			return status;
		}
		catch (InterruptedException e) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}
	
	private static CloudServerURL convertAbstractCloudFoundryUrlToCloudServerURL (AbstractCloudFoundryUrl abstractUrl) {
		CloudServerURL cloudUrl = null;
		if (abstractUrl != null) {
			// Nothing to do, this is already an old CloudServerURL, just return it
			if (abstractUrl instanceof CloudServerURL) {
				cloudUrl = (CloudServerURL)(abstractUrl);
			} else {
				cloudUrl = new CloudServerURL(abstractUrl.getName(), abstractUrl.getUrl(), abstractUrl.getUserDefined(), 
						abstractUrl.getSignUpUrl(), abstractUrl.getSelfSigned()); 
			}
		}
		return cloudUrl;
	}
	
	private static List<CloudServerURL> convertAbstractCloudFoundryUrlListToCloudServerURLList (List <AbstractCloudFoundryUrl> abstractUrls) {
		if (abstractUrls == null)
			return null;
		
		List<CloudServerURL> urls = new ArrayList<CloudFoundryBrandingExtensionPoint.CloudServerURL>();
		for (AbstractCloudFoundryUrl abstractUrl : abstractUrls) {
			if (abstractUrl != null) {
				urls.add (convertAbstractCloudFoundryUrlToCloudServerURL(abstractUrl));	
			}
		}
		return urls;
	}

	/** 
	 * @deprecated use {@link CloudServerUIUtil#getAllUrls(String, IRunnableContext)}
	 */
	public static List<CloudServerURL> getAllUrls(String serverTypeId) {
		try {
			// Switch to new generic utility method, then convert to the expected return type
			return convertAbstractCloudFoundryUrlListToCloudServerURLList(CloudServerUIUtil.getAllUrls(serverTypeId, null));
		} catch (CoreException ex) {
			CloudFoundryServerUiPlugin.logError(ex);
		}
		
		// If an exception shows up, return an empty list (to have backwards compatibility)
		return new ArrayList<CloudServerURL>();
	}

	/** 
	 * @deprecated use {@link CloudServerUIUtil#getDefaultUrl(String, IRunnableContext)}
	 */
	public static CloudServerURL getDefaultUrl(String serverTypeId) {		
		CloudServerURL url = null;
		try {
			// Switch to new generic utility method, then convert to the expected return type
			AbstractCloudFoundryUrl abstractUrl = CloudServerUIUtil.getDefaultUrl(serverTypeId, null);
			if (abstractUrl != null) {
				url = convertAbstractCloudFoundryUrlToCloudServerURL(abstractUrl);
			} 
		} catch (CoreException ex) {
			CloudFoundryServerUiPlugin.logError(ex);
		}
		
		return url;
	}

	/** 
	 * @deprecated use {@link CloudServerUIUtil#getUrls(String, IRunnableContext)}
	 */
	public static List<CloudServerURL> getUrls(String serverTypeId) {
		try {
			// Switch to new generic utility method, then convert to the expected return type
			return convertAbstractCloudFoundryUrlListToCloudServerURLList(CloudServerUIUtil.getUrls(serverTypeId, null));
		} catch (CoreException ex) {
			CloudFoundryServerUiPlugin.logError(ex);
		}
		
		// If an exception shows up, return an empty list (to have backwards compatibility)
		return new ArrayList<CloudServerURL>();
	}

	/**
	 * @deprecated use {@link CloudServerUIUtil#getUserDefinedUrls(String))}
	 */
	public static List<CloudServerURL> getUserDefinedUrls(String serverTypeId) {
		// Switch to new generic utility method, then convert to the expected return type
		return convertAbstractCloudFoundryUrlListToCloudServerURLList(CloudServerUIUtil.getUserDefinedUrls(serverTypeId));
	}

	/**
	 * @deprecated user {@link CloudServerUIUtil#storeUserDefinedUrls(String, List)}
	 */
	public static void storeUserDefinedUrls(String serverTypeId, List<CloudServerURL> urls) {
		if (urls == null)
			return;
		
		List <AbstractCloudFoundryUrl> abstractUrls = new ArrayList <AbstractCloudFoundryUrl> ();
		for (CloudServerURL cloudUrl : urls) {
			abstractUrls.add(cloudUrl);
		}
		
		// Use the new correct method
		CloudServerUIUtil.storeUserDefinedUrls(serverTypeId, abstractUrls);
	}

	/**
	 * Validates the given credentials. Throws {@link CoreException} if error
	 * occurred during validation.
	 * @param userName
	 * @param password
	 * @param urlText
	 * @param displayURL
	 * @param selfSigned true if its a server using self-signed certificate. If
	 * this information is not known, set this to false
	 * @param context
	 * 
	 * @throws CoreException if validation failed and error type cannot be
	 * determined
	 * @throws OperationCanceledException if validation is cancelled.
	 */
	public static void validateCredentials(final String userName, final String password, final String urlText,
			final boolean displayURL, final boolean selfSigned, IRunnableContext context) throws CoreException,
			OperationCanceledException {
		try {
			ICoreRunnable coreRunner = new ICoreRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					String url = urlText;
					if (displayURL) {
						url = getUrlFromDisplayText(urlText);
					}
					CloudFoundryServerBehaviour.validate(url, userName, password, selfSigned, monitor);
				}
			};
			if (context != null) {
				runForked(coreRunner, context);
			}
			else {
				runForked(coreRunner);
			}
		}
		catch (CoreException ce) {
			if (ce.getCause() instanceof ResourceAccessException
					&& ce.getCause().getCause() instanceof javax.net.ssl.SSLPeerUnverifiedException) {
				// Self-signed error. Re-throw as it will involve a client
				// change
				throw CloudErrorUtil.toCoreException(ce.getCause().getCause());
			}
			else {
				throw ce;
			}
		}

	}

	/**
	 * Runnable context can be null. If so, default Eclipse progress service
	 * will be used as a runnable context. Display URL should be true if the
	 * display URL is passed. If so, and attempt will be made to parse the
	 * actual URL.
	 * 
	 * @param userName must not be null
	 * @param password must not be null
	 * @param urlText must not be null. Can be either display or actual URL
	 * @param displayURL true if URL is display URL
	 * @param selfSigned true if connecting to a self-signing server. False otherwise
	 * @param context may be optional
	 * @return spaces descriptor, or null if it couldn't be determined
	 * @throws CoreException
	 */
	public static CloudOrgsAndSpaces getCloudSpaces(final String userName, final String password, final String urlText,
			final boolean displayURL, final boolean selfSigned, IRunnableContext context) throws CoreException {

		try {
			final CloudOrgsAndSpaces[] supportsSpaces = new CloudOrgsAndSpaces[1];
			ICoreRunnable coreRunner = new ICoreRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					String url = urlText;
					if (displayURL) {
						url = getUrlFromDisplayText(urlText);
					}
					supportsSpaces[0] = CloudFoundryServerBehaviour.getCloudSpacesExternalClient(new CloudCredentials(
							userName, password), url, selfSigned, monitor);
				}
			};
			if (context != null) {
				runForked(coreRunner, context);
			}
			else {
				runForked(coreRunner);
			}

			return supportsSpaces[0];
		}
		catch (OperationCanceledException e) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
		}

	}

	public static String getUrlFromDisplayText(String displayText) {
		String url = displayText;
		if (url != null) {
			int pos = url.lastIndexOf(" - "); //$NON-NLS-1$
			if (pos >= 0) {
				return url.substring(pos + 3);
			}
		}

		return url;
	}

	public static String getDisplayTextFromUrl(String url, String serverTypeId) {
		try {
			List<AbstractCloudFoundryUrl> cloudUrls = CloudServerUIUtil.getAllUrls(serverTypeId, null);
			for (AbstractCloudFoundryUrl cloudUrl : cloudUrls) {
				if (cloudUrl.getUrl().equals(url)) {
					return cloudUrl.getName() + " - " + url; //$NON-NLS-1$
				}
			}
		} catch (CoreException ex) {
			CloudFoundryServerUiPlugin.logError(ex);
		}
		return url;
	}

	public static void runForked(final ICoreRunnable coreRunner) throws OperationCanceledException, CoreException {
		runForked(coreRunner, PlatformUI.getWorkbench().getProgressService());
	}

	public static void runForked(final ICoreRunnable coreRunner, IRunnableContext progressService)
			throws OperationCanceledException, CoreException {
		try {
			IRunnableWithProgress runner = new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
					try {
						coreRunner.run(monitor);
					}
					catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
					finally {
						monitor.done();
					}
				}

			};
			progressService.run(true, true, runner);
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				throw (CoreException) e.getCause();
			}
			else {
				CloudFoundryServerUiPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, "Unexpected exception", e)); //$NON-NLS-1$
			}
		}
		catch (InterruptedException e) {
			throw new OperationCanceledException();
		}
	}

	public static void openUrl(String location) {
		openUrl(location, WebBrowserPreference.getBrowserChoice());
	}

	public static void openUrl(String location, int browserChoice) {
		try {
			URL url = null;
			if (location != null) {
				url = new URL(location);
			}
			if (browserChoice == WebBrowserPreference.EXTERNAL) {
				try {
					IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
					support.getExternalBrowser().openURL(url);
				}
				catch (Exception e) {
				}
			}
			else {
				IWebBrowser browser;
				int flags;
				if (WorkbenchBrowserSupport.getInstance().isInternalWebBrowserAvailable()) {
					flags = IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.LOCATION_BAR
							| IWorkbenchBrowserSupport.NAVIGATION_BAR;
				}
				else {
					flags = IWorkbenchBrowserSupport.AS_EXTERNAL | IWorkbenchBrowserSupport.LOCATION_BAR
							| IWorkbenchBrowserSupport.NAVIGATION_BAR;
				}

				String generatedId = "org.eclipse.mylyn.web.browser-" + Calendar.getInstance().getTimeInMillis(); //$NON-NLS-1$
				browser = WorkbenchBrowserSupport.getInstance().createBrowser(flags, generatedId, null, null);
				browser.openURL(url);
			}
		}
		catch (PartInitException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					Messages.CloudUiUtil_ERROR_OPEN_BROWSER_FAIL_TITLE, Messages.CloudUiUtil_ERROR_OPEN_BROWSER_BODY);
		}
		catch (MalformedURLException e) {
			if (location == null || location.trim().equals("")) { //$NON-NLS-1$
				MessageDialog.openInformation(Display.getDefault().getActiveShell(),
						Messages.CloudUiUtil_ERROR_OPEN_BROWSER_FAIL_TITLE,
						NLS.bind(Messages.CloudUiUtil_ERROR_EMPTY_URL_BODY, location));
			}
			else {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(),
						Messages.CloudUiUtil_ERROR_OPEN_BROWSER_FAIL_TITLE,
						NLS.bind(Messages.CloudUiUtil_ERROR_MALFORM_URL_BODY, location));
			}
		}

	}

	/**
	 * Prompts user to define a value for the wildcard in the cloud URL, then
	 * return the new URL
	 * 
	 * @param cloudUrl
	 * @param allCloudUrls
	 * @param shell
	 * @return new URL, null if no wildcard appears in cloudUrl or if user
	 * cancels out of defining a new value
	 * @deprecated use {@link CloudServerUIUtil#getWildcardUrl(AbstractCloudFoundryUrl, List, Shell)} instead.
	 */
	public static CloudServerURL getWildcardUrl(CloudServerURL cloudUrl, List<CloudServerURL> allCloudUrls, Shell shell) {
		// Switch to new generic utility method, then convert to the expected return type
		ArrayList <AbstractCloudFoundryUrl> allCloudFoundryUrls = new ArrayList<AbstractCloudFoundryUrl>();
		if (allCloudUrls != null) {
			for (CloudServerURL _cloudUrl : allCloudUrls) {
				allCloudFoundryUrls.add (_cloudUrl);
			}
		}
		
		AbstractCloudFoundryUrl returnUrl = CloudServerUIUtil.getWildcardUrl(cloudUrl, allCloudFoundryUrls, shell);
		if (returnUrl != null) {
			return new CloudServerURL(returnUrl.getName(), returnUrl.getUrl(), true, returnUrl.getSelfSigned());
		}
		
		return null;
	}

	/**
	 * If the Servers view is available and it contains a selection, the
	 * corresponding structured selection is returned. In any other case,
	 * including the Servers view being unavailable, either because it is not
	 * installed or it is closed, null is returned.
	 * @return structured selection in the Servers view, if the Servers view is
	 * open and available, or null otherwise
	 */
	public static IStructuredSelection getServersViewSelection() {

		IViewRegistry registry = PlatformUI.getWorkbench().getViewRegistry();
		String serversViewID = SERVERS_VIEW_ID;

		// fast check to verify that the servers View is available.
		IViewDescriptor serversViewDescriptor = registry.find(serversViewID);
		if (serversViewDescriptor != null) {

			// Granular null checks required as any of the workbench components
			// may not be available at some given point in time (e.g., during
			// start/shutdown)
			IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

			if (activeWorkbenchWindow != null) {

				IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();

				if (activePage != null) {
					IViewReference[] references = activePage.getViewReferences();

					if (references != null) {
						IViewPart serversViewPart = null;
						for (IViewReference reference : references) {
							if (serversViewID.equals(reference.getId())) {
								serversViewPart = reference.getView(true);
								break;
							}
						}

						if (serversViewPart != null) {

							IViewSite viewSite = serversViewPart.getViewSite();
							if (viewSite != null) {
								ISelectionProvider selectionProvider = viewSite.getSelectionProvider();
								if (selectionProvider != null) {
									ISelection selection = selectionProvider.getSelection();
									if (selection instanceof IStructuredSelection) {
										return (IStructuredSelection) selection;
									}
								}
							}
						}
					}
				}
			}

		}
		return null;
	}

	/**
	 * Returns the current shell or null.
	 * @return
	 */
	public static Shell getShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}

}
