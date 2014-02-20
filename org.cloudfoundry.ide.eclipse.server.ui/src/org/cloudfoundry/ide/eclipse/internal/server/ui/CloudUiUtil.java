/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudOrgsAndSpaces;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.springframework.web.client.ResourceAccessException;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Terry Denney
 */
@SuppressWarnings("restriction")
public class CloudUiUtil {

	public static final String SERVERS_VIEW_ID = "org.eclipse.wst.server.ui.ServersView";

	public static String ATTR_USER_DEFINED_URLS = "org.cloudfoundry.ide.eclipse.server.user.defined.urls";

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
						"The operation failed: {0}", e.getCause().getMessage()), e);
			}
			else {
				status = new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, NLS.bind(
						"Unexpected error: {0}", e.getMessage()), e);
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

	public static List<CloudServerURL> getAllUrls(String serverTypeId) {
		List<CloudServerURL> urls = new ArrayList<CloudFoundryBrandingExtensionPoint.CloudServerURL>();
		urls.add(getDefaultUrl(serverTypeId));
		urls.addAll(getUrls(serverTypeId));
		return urls;
	}

	public static CloudServerURL getDefaultUrl(String serverTypeId) {
		return CloudFoundryBrandingExtensionPoint.getDefaultUrl(serverTypeId);
	}

	public static List<CloudServerURL> getUrls(String serverTypeId) {
		List<CloudServerURL> cloudUrls = new ArrayList<CloudServerURL>();

		Set<String> urlNames = new HashSet<String>();

		List<CloudServerURL> userDefinedUrls = CloudUiUtil.getUserDefinedUrls(serverTypeId);
		for (CloudServerURL userDefinedUrl : userDefinedUrls) {
			cloudUrls.add(userDefinedUrl);
			urlNames.add(userDefinedUrl.getName());
		}

		List<CloudServerURL> defaultUrls = CloudFoundryBrandingExtensionPoint.getCloudUrls(serverTypeId);
		if (defaultUrls != null) {
			for (CloudServerURL defaultUrl : defaultUrls) {
				if (!urlNames.contains(defaultUrl.getName())) {
					cloudUrls.add(defaultUrl);
				}
			}

			Collections.sort(cloudUrls, new Comparator<CloudServerURL>() {

				public int compare(CloudServerURL o1, CloudServerURL o2) {
					return o1.getName().compareToIgnoreCase(o2.getName());
				}

			});
		}

		return cloudUrls;
	}

	public static List<CloudServerURL> getUserDefinedUrls(String serverTypeId) {
		List<CloudServerURL> urls = new ArrayList<CloudServerURL>();

		IPreferenceStore prefStore = CloudFoundryServerUiPlugin.getDefault().getPreferenceStore();
		String urlString = prefStore.getString(ATTR_USER_DEFINED_URLS + "." + serverTypeId);

		if (urlString != null && urlString.length() > 0) {
			// Split on "||"
			String[] urlEntries = urlString.split("\\|\\|");
			if (urlEntries != null) {
				for (String entry : urlEntries) {
					if (entry.length() > 0) {
						String[] values = entry.split(",");
						if (values != null) {
							String name = null;
							String url = null;

							if (values.length >= 2) {
								name = values[0];
								url = values[1];
							}

							urls.add(new CloudServerURL(name, url, true));
						}
					}

				}
			}
		}

		return urls;
	}

	public static void storeUserDefinedUrls(String serverTypeId, List<CloudServerURL> urls) {
		IPreferenceStore prefStore = CloudFoundryServerUiPlugin.getDefault().getPreferenceStore();
		StringBuilder builder = new StringBuilder();

		for (CloudServerURL url : urls) {
			if (url.getUserDefined()) {
				builder.append(url.getName());

				builder.append(",");
				builder.append(url.getUrl());

				builder.append("||");
			}
		}

		prefStore.setValue(ATTR_USER_DEFINED_URLS + "." + serverTypeId, builder.toString());
	}

	public static String updatePassword(final String newPassword, final CloudFoundryServer cfServer,
			final IServerWorkingCopy serverWc) {
		ICoreRunnable coreRunner = new ICoreRunnable() {
			public void run(final IProgressMonitor monitor) throws CoreException {
				cfServer.getBehaviour().updatePassword(newPassword, monitor);
				cfServer.setPassword(newPassword);
				// cfServer.saveConfiguration(monitor);
				ServerEventHandler.getDefault().firePasswordUpdated(cfServer);
				serverWc.save(true, monitor);
			}
		};

		try {
			CloudUiUtil.runForked(coreRunner);
		}
		catch (OperationCanceledException ex) {
		}
		catch (CoreException ex) {
			return ex.getMessage();
		}

		return null;
	}

	/**
	 * Validates the given credentials. If an error occurred, it either returns
	 * a validation message if the error can be recognised, or throws
	 * {@link CoreException} if error cannot be recognised.
	 * @param userName
	 * @param password
	 * @param urlText
	 * @param displayURL
	 * @param selfSigned true if its a server using self-signed certificate. If
	 * this information is not known, set this to false
	 * @param context
	 * @return null if validation was successful. Error message if validation
	 * error is recognised
	 * @throws CoreException if validation failed and error type cannot be
	 * determined
	 */
	public static String validateCredentials(final String userName, final String password, final String urlText,
			final boolean displayURL, final boolean selfSigned, IRunnableContext context) throws CoreException {
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

			return null;
		}
		catch (CoreException ce) {
			if (ce.getCause() instanceof ResourceAccessException
					&& ce.getCause().getCause() instanceof javax.net.ssl.SSLPeerUnverifiedException) {
				// Self-signed error. Re-throw as it will involve a client
				// change
				throw CloudErrorUtil.toCoreException(ce.getCause().getCause());
			}
			else {
				String message = CloudErrorUtil.getConnectionError(ce);
				return message;
			}
		}
		catch (OperationCanceledException e) {
		}

		return "Can't validate credentials with server";
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
	 * @param context may be optional
	 * @param fork if true, an attempt will be made to get the cloud spaces
	 * asynchronously
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
			int pos = url.lastIndexOf(" - ");
			if (pos >= 0) {
				return url.substring(pos + 3);
			}
		}

		return url;
	}

	public static String getDisplayTextFromUrl(String url, String serverTypeId) {
		List<CloudServerURL> cloudUrls = getAllUrls(serverTypeId);
		for (CloudServerURL cloudUrl : cloudUrls) {
			if (cloudUrl.getUrl().equals(url)) {
				return cloudUrl.getName() + " - " + url;
			}
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
					monitor.beginTask("", IProgressMonitor.UNKNOWN);
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
						.log(new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, "Unexpected exception", e));
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

				String generatedId = "org.eclipse.mylyn.web.browser-" + Calendar.getInstance().getTimeInMillis();
				browser = WorkbenchBrowserSupport.getInstance().createBrowser(flags, generatedId, null, null);
				browser.openURL(url);
			}
		}
		catch (PartInitException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Failed to Open Browser",
					"Browser could not be initiated");
		}
		catch (MalformedURLException e) {
			if (location == null || location.trim().equals("")) {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Failed to Open Browser",
						"No URL to open." + location);
			}
			else {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Failed to Open Browser",
						"Could not open URL: " + location);
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
	 */
	public static CloudServerURL getWildcardUrl(CloudServerURL cloudUrl, List<CloudServerURL> allCloudUrls, Shell shell) {
		String url = cloudUrl.getUrl();
		if (url.contains("{")) {
			int startIndex = url.indexOf("{");
			int endIndex = url.indexOf("}");
			String wildcard = url.substring(startIndex + 1, endIndex);

			TargetURLDialog dialog = new TargetURLDialog(shell, cloudUrl, wildcard, allCloudUrls);
			if (dialog.open() == IDialogConstants.OK_ID) {
				url = dialog.getUrl();
				String name = dialog.getName();
				// CloudUiUtil.addUserDefinedUrl(serverTypeId, name, url);
				return new CloudServerURL(name, url, true);
			}
			else {
				return null;
			}
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
