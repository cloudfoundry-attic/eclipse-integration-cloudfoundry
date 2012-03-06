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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint.CloudURL;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
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

	public static List<CloudURL> getAllUrls(String serverTypeId) {
		List<CloudURL> urls = new ArrayList<CloudFoundryBrandingExtensionPoint.CloudURL>();
		urls.add(getDefaultUrl(serverTypeId));
		urls.addAll(getUrls(serverTypeId));
		return urls;
	}

	public static CloudURL getDefaultUrl(String serverTypeId) {
		return CloudFoundryBrandingExtensionPoint.getDefaultUrl(serverTypeId);
	}

	public static List<CloudURL> getUrls(String serverTypeId) {
		List<CloudURL> cloudUrls = new ArrayList<CloudURL>();

		Set<String> urlNames = new HashSet<String>();

		List<CloudURL> userDefinedUrls = CloudUiUtil.getUserDefinedUrls(serverTypeId);
		for (CloudURL userDefinedUrl : userDefinedUrls) {
			cloudUrls.add(userDefinedUrl);
			urlNames.add(userDefinedUrl.getName());
		}

		List<CloudURL> defaultUrls = CloudFoundryBrandingExtensionPoint.getCloudUrls(serverTypeId);
		if (defaultUrls != null) {
			for (CloudURL defaultUrl : defaultUrls) {
				if (!urlNames.contains(defaultUrl.getName())) {
					cloudUrls.add(defaultUrl);
				}
			}

			Collections.sort(cloudUrls, new Comparator<CloudURL>() {

				public int compare(CloudURL o1, CloudURL o2) {
					return o1.getName().compareToIgnoreCase(o2.getName());
				}

			});
		}

		return cloudUrls;
	}

	public static List<CloudURL> getUserDefinedUrls(String serverTypeId) {
		List<CloudURL> urls = new ArrayList<CloudURL>();

		IPreferenceStore prefStore = CloudFoundryServerUiPlugin.getDefault().getPreferenceStore();
		String urlString = prefStore.getString(ATTR_USER_DEFINED_URLS + "." + serverTypeId);

		while (urlString.length() > 0) {
			int index = urlString.indexOf(",");
			String name = urlString.substring(0, index);

			urlString = urlString.substring(index + 1);
			index = urlString.indexOf("||");
			String url = urlString.substring(0, index);
			urlString = urlString.substring(index + 2);

			urls.add(new CloudURL(name, url, true));
		}

		return urls;
	}

	public static void storeUserDefinedUrls(String serverTypeId, List<CloudURL> urls) {
		IPreferenceStore prefStore = CloudFoundryServerUiPlugin.getDefault().getPreferenceStore();
		StringBuilder builder = new StringBuilder();

		for (CloudURL url : urls) {
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
				CloudFoundryPlugin.getDefault().firePasswordUpdated(cfServer);
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

	public static String validateCredentials(CloudFoundryServer cfServer, final String userName, final String password,
			final String urlText, final boolean displayURL, IRunnableContext context) {
		if (cfServer != null) {
			try {
				ICoreRunnable coreRunner = new ICoreRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
						String url = urlText;
						if (displayURL) {
							url = getUrlFromDisplayText(urlText);
						}
						CloudFoundryServerBehaviour.validate(url, userName, password, monitor);
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
			catch (CoreException e) {
				String message = CloudUtil.getValidationErrorMessage(e);
				return message;
			}
			catch (OperationCanceledException e) {
			}
		}

		return "Can't validate credentials with server";
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
		List<CloudURL> cloudUrls = getAllUrls(serverTypeId);
		for (CloudURL cloudUrl : cloudUrls) {
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
	public static CloudURL getWildcardUrl(CloudURL cloudUrl, List<CloudURL> allCloudUrls, Shell shell) {
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
				return new CloudURL(name, url, true);
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
			// may not be available at some given point in time (e.g., during start/shutdown)
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

}
