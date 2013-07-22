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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleCache.ServerData;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRegistry;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.jst.server.core.internal.J2EEUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.server.core.model.ServerDelegate;

/**
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class CloudFoundryServer extends ServerDelegate {

	private static ThreadLocal<Boolean> deleteServicesOnModuleRemove = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return Boolean.TRUE;
		}
	};

	/**
	 * Attribute key for the a unique server ID used to store credentials in the
	 * secure store.
	 */
	static final String PROP_SERVER_ID = "org.cloudfoundry.ide.eclipse.serverId";

	/**
	 * Attribute key for the password.
	 */
	static final String PROP_PASSWORD_ID = "org.cloudfoundry.ide.eclipse.password";

	/**
	 * Attribute key for the API url.
	 */
	static final String PROP_URL = "org.cloudfoundry.ide.eclipse.url";

	/**
	 * Attribute key for the username.
	 */
	static final String PROP_USERNAME_ID = "org.cloudfoundry.ide.eclipse.username";

	static final String PROP_ORG_ID = "org.cloudfoundry.ide.eclipse.org";

	static final String PROP_ORG_GUID = "org.cloudfoundry.ide.eclipse.org.guid";

	static final String PROP_SPACE_GUID = "org.cloudfoundry.ide.eclipse.space.guid";

	static final String PROP_SPACE_ID = "org.cloudfoundry.ide.eclipse.space";

	static final String TUNNEL_SERVICE_COMMANDS_PROPERTY = "org.cloudfoundry.ide.eclipse.tunnel.service.commands";

	private static final String PROPERTY_DEPLOYMENT_NAME = "deployment_name";

	static void updateState(Server server, CloudFoundryApplicationModule appModule) throws CoreException {
		IModule localModule = appModule.getLocalModule();
		server.setModuleState(new IModule[] { localModule }, appModule.getState());
		if (server.getModulePublishState(new IModule[] { localModule }) == IServer.PUBLISH_STATE_UNKNOWN) {
			server.setModulePublishState(new IModule[] { localModule }, appModule.getPublishState());
		}
	}

	private String serverTypeId;

	private ServerCredentialsStore credentialsStore;

	private boolean secureStoreDirty;

	private String initialServerId;

	private String password;

	private CloudFoundrySpace cloudSpace;

	public CloudFoundryServer() {
		// constructor
	}

	public void updateApplication(CloudFoundryApplicationModule module) {
		getData().updateModule(module);
	}

	@Override
	public IStatus canModifyModules(IModule[] add, IModule[] remove) {
		if (add != null) {
			int size = add.length;
			for (int i = 0; i < size; i++) {
				IModule module = add[i];
				if (!ApplicationRegistry.isSupportedModule(module)) {
					return new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, 0,
							"This server does not support applications of type: " + module.getModuleType().getId(),
							null);
				}

				IStatus status;
				if (module.getProject() != null) {
					status = FacetUtil.verifyFacets(module.getProject(), getServer());
					if (status != null && !status.isOK()) {
						return status;
					}
				}
			}
		}
		// if (remove != null) {
		// for (IModule element : remove) {
		// if (element instanceof ApplicationModule) {
		// return new Status(IStatus.ERROR, CloudfoundryPlugin.PLUGIN_ID, 0,
		// "Some modules can not be removed.", null);
		// }
		// }
		// }

		return Status.OK_STATUS;
	}

	public void clearApplications() {
		getData().clear();
	}

	public IStatus error(String message, Exception e) {
		return new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind("{0} [{1}]", message, getServer()
				.getName()), e);
	}

	public CloudFoundryApplicationModule getApplication(IModule module) {
		if (module instanceof CloudFoundryApplicationModule) {
			return (CloudFoundryApplicationModule) module;
		}
		return getData().getOrCreateApplicationModule(module);
	}

	public CloudFoundryApplicationModule getApplication(IModule[] modules) {
		if (modules != null && modules.length > 0) {
			return getApplication(modules[0]);
		}
		return null;
	}

	public Collection<CloudFoundryApplicationModule> getApplications() {
		return getData().getApplications();
	}

	public CloudFoundryServerBehaviour getBehaviour() {
		return (CloudFoundryServerBehaviour) getServer().loadAdapter(CloudFoundryServerBehaviour.class, null);
	}

	@Override
	public IModule[] getChildModules(IModule[] module) {
		if (module == null) {
			return null;
		}

		// IModuleType moduleType = module[0].getModuleType();
		//
		// if (module.length == 1 && moduleType != null &&
		// ID_WEB_MODULE.equals(moduleType.getId())) {
		// IWebModule webModule = (IWebModule)
		// module[0].loadAdapter(IWebModule.class, null);
		// if (webModule != null) {
		// IModule[] modules = webModule.getModules();
		// return modules;
		// }
		// }

		return new IModule[0];
	}

	private ServerData getData() {
		return CloudFoundryPlugin.getModuleCache().getData(getServerOriginal());
	}

	public boolean hasValidServerData() {
		return getServerOriginal() != null && getData() != null;
	}

	public String getDeploymentName() {
		return getAttribute(PROPERTY_DEPLOYMENT_NAME, "");
	}

	public String getPassword() {
		if (secureStoreDirty) {
			return password;
		}
		String cachedPassword = getData().getPassword();
		if (cachedPassword != null) {
			return cachedPassword;
		}
		String legacyPassword = getAttribute(PROP_PASSWORD_ID, (String) null);
		if (legacyPassword != null) {
			return legacyPassword;
		}
		return new ServerCredentialsStore(getServerId()).getPassword();
	}

	/**
	 * Public for testing.
	 */
	public synchronized ServerCredentialsStore getCredentialsStore() {
		if (credentialsStore == null) {
			credentialsStore = new ServerCredentialsStore(initialServerId);
		}
		return credentialsStore;
	}

	@Override
	public IModule[] getRootModules(IModule module) throws CoreException {
		if (ApplicationRegistry.isSupportedModule(module)) {
			IStatus status = canModifyModules(new IModule[] { module }, null);
			if (status == null || !status.isOK()) {
				throw new CoreException(status);
			}
			return new IModule[] { module };
		}

		return J2EEUtil.getWebModules(module, null);
	}

	public CloudFoundryServerRuntime getRuntime() {
		return (CloudFoundryServerRuntime) getServer().getRuntime().loadAdapter(CloudFoundryServerRuntime.class, null);
	}

	/**
	 * This method is API used by CloudFoundry Code.
	 */
	public String getUrl() {
		return getAttribute(PROP_URL, (String) null);
	}

	public String getUsername() {
		return getAttribute(PROP_USERNAME_ID, (String) null);
	}

	public String getServerId() {
		return getAttribute(PROP_SERVER_ID, (String) null);
	}

	public boolean supportsCloudSpaces() {
		return getCloudFoundrySpace() != null;
	}

	public CloudFoundrySpace getCloudFoundrySpace() {

		if (cloudSpace == null) {
			String orgName = getOrg();
			String spaceName = getSpace();

			String[] checkValidity = { orgName, spaceName };
			boolean valid = false;
			for (String value : checkValidity) {
				valid = validSpaceValue(value);
				if (!valid) {
					break;
				}
			}
			if (valid) {
				cloudSpace = new CloudFoundrySpace(orgName, spaceName);
			}

		}
		return cloudSpace;
	}

	protected boolean validSpaceValue(String value) {
		return value != null && value.length() > 0;
	}

	public boolean isConnected() {
		return getServer().getServerState() == IServer.STATE_STARTED;
	}

	@Override
	public void modifyModules(final IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
		if (remove != null && remove.length > 0) {
			for (IModule module : remove) {
				getData().tagAsDeployed(module);
			}

			try {
				getBehaviour().deleteModules(remove, deleteServicesOnModuleRemove.get(), monitor);
			}
			catch (CoreException e) {
				// ignore deletion of applications that didn't exist
				if (!CloudErrorUtil.isNotFoundException(e)) {
					throw e;
				}
			}
		}

		if (add != null && add.length > 0) {
			for (IModule module : add) {
				// avoid automatic deletion before module has been deployed
				getData().tagAsUndeployed(module);
			}

			// callback to disable auto deploy when testing
			if (CloudFoundryPlugin.getCallback().isAutoDeployEnabled()) {
				Job deployModules = new Job("Deploying application to Cloud Foundry") {
					@Override
					protected IStatus run(IProgressMonitor monitor2) {
						Set<IModule> pending = new HashSet<IModule>(Arrays.asList(add));
						try {
							for (IModule module : add) {
								getBehaviour().deployOrStartModule(new IModule[] { module }, true, monitor2);
								pending.remove(module);
							}
						}
						catch (OperationCanceledException e) {
							// remove module from server
							doDeleteModules(pending);
							return Status.CANCEL_STATUS;
						}
						catch (CoreException e) {
							CloudFoundryPlugin
									.getDefault()
									.getLog()
									.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
											"Failed to deploy module", e));
						}
						return Status.OK_STATUS;
					}
				};
				deployModules.schedule();
			}
		}
	}

	@Override
	public void setDefaults(IProgressMonitor monitor) {
		super.setDefaults(monitor);
		String typeName = CloudFoundryBrandingExtensionPoint.getServerDisplayName(serverTypeId);
		if (typeName == null || typeName.trim().length() == 0) {
			typeName = getServer().getServerType().getName();
		}
		String name = typeName;
		int i = 2;
		while (ServerPlugin.isNameInUse(getServerWorkingCopy().getOriginal(), name)) {
			name = NLS.bind("{0} ({1})", new String[] { typeName, i + "" });
			i++;
		}
		getServerWorkingCopy().setName(name);

		setAttribute("auto-publish-setting", 1);
	}

	public void setDeploymentName(String name) {
		setAttribute(PROPERTY_DEPLOYMENT_NAME, name);
	}

	public void setPassword(String password) {
		this.secureStoreDirty = true;
		this.password = password;

		// remove password in case an earlier version stored it in server
		// properties
		if (getServerWorkingCopy() != null) {
			getServerWorkingCopy().setAttribute(PROP_PASSWORD_ID, (String) null);
		}
		// in case setUrl() or setPassword() were never called, e.g. for legacy
		// servers
		updateServerId();

		getData().setPassword(password);
	}

	public void setSpace(CloudSpace space) {
		this.secureStoreDirty = true;

		if (space != null) {
			this.cloudSpace = new CloudFoundrySpace(space);
			internalSetOrg(cloudSpace.getOrgName());
			internalSetSpace(cloudSpace.getSpaceName());
		}
		else {
			// Otherwise clear the org and space
			internalSetOrg(null);
			internalSetSpace(null);
			cloudSpace = null;
		}

		updateServerId();
	}

	public void setUrl(String url) {
		setAttribute(PROP_URL, url);
		updateServerId();
	}

	public void setUsername(String username) {
		setAttribute(PROP_USERNAME_ID, username);
		updateServerId();
	}

	protected void internalSetOrg(String org) {
		setAttribute(PROP_ORG_ID, org);
	}

	protected void internalSetSpace(String space) {
		setAttribute(PROP_SPACE_ID, space);
	}

	protected String getOrg() {
		return getAttribute(PROP_ORG_ID, (String) null);
	}

	protected String getSpace() {
		return getAttribute(PROP_SPACE_ID, (String) null);
	}

	private void updateServerId() {
		StringWriter writer = new StringWriter();
		writer.append(getUsername());
		if (supportsCloudSpaces()) {
			writer.append('_');
			writer.append(getOrg());
			writer.append('_');
			writer.append(getSpace());
		}
		writer.append('@');
		writer.append(getUrl());

		setAttribute(PROP_SERVER_ID, writer.toString());
	}

	@Override
	protected void initialize() {
		super.initialize();
		serverTypeId = getServer().getServerType().getId();
		// legacy in case password was saved by an earlier version
		this.password = getAttribute(PROP_PASSWORD_ID, (String) null);
		this.initialServerId = getAttribute(PROP_SERVER_ID, (String) null);
	}

	void updateModules(Map<String, CloudApplication> applicationByName) throws CoreException {
		Server server = (Server) getServer();

		final Set<CloudFoundryApplicationModule> allModules = new HashSet<CloudFoundryApplicationModule>();
		List<CloudFoundryApplicationModule> externalModules = new ArrayList<CloudFoundryApplicationModule>();
		final Set<IModule> deletedModules = new HashSet<IModule>();

		synchronized (this) {
			// check for existing modules and remove them from applicationByName
			for (IModule module : server.getModules()) {
				CloudFoundryApplicationModule appModule = getApplication(module);
				CloudApplication application = applicationByName.remove(appModule.getApplicationId());
				appModule.setCloudApplication(application);
				if (application != null) {
					// the modules maps to an existing application
					if (appModule.isExternal()) {
						externalModules.add(appModule);
					}
					allModules.add(appModule);
				}
				else if (getData().isUndeployed(module)) {
					// deployment is still in progress
					allModules.add(appModule);
				}
				else {
					// the module maps to an application that no longer exists
					deletedModules.add(module);
				}
			}

			// create modules for new applications
			for (CloudApplication application : applicationByName.values()) {
				CloudFoundryApplicationModule appModule = getData().createModule(application);
				externalModules.add(appModule);
				allModules.add(appModule);
			}

			// update state for cloud applications
			server.setExternalModules(externalModules.toArray(new IModule[0]));

			for (IModule module : server.getModules()) {
				CloudFoundryApplicationModule appModule = getApplication(module);
				updateState(server, appModule);
			}

			// update state for deleted applications to trigger a refresh
			if (deletedModules.size() > 0) {
				for (IModule module : deletedModules) {
					server.setModuleState(new IModule[] { module }, IServer.STATE_UNKNOWN);
				}
				deleteModules(deletedModules);
			}

			getData().removeObsoleteModules(allModules);
		}
	}

	private void deleteModules(final Set<IModule> deletedModules) {
		Job deleteJob = new Job("Update Modules") {
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				return doDeleteModules(deletedModules);
			}
		};
		deleteJob.schedule();
	}

	public void removeApplication(CloudFoundryApplicationModule cloudModule) {
		getData().remove(cloudModule);
	}

	public IServer getServerOriginal() {
		// if a working copy is saved the delegate is replaced so getServer() is
		// not guaranteed to return an original even if the delegate was
		// accessed from an original
		IServer server = getServer();
		if (server instanceof IServerWorkingCopy) {
			return ((IServerWorkingCopy) server).getOriginal();
		}
		return server;
	}

	String getServerAttribute(String key, String defaultValue) {
		return super.getAttribute(key, defaultValue);
	}

	@Override
	public void saveConfiguration(IProgressMonitor monitor) throws CoreException {
		String serverId = getServerId();
		if (secureStoreDirty || (serverId != null && !serverId.equals(initialServerId))) {
			getData().updateServerId(initialServerId, serverId);

			// cache password
			getData().setPassword(password);
			// persist password
			ServerCredentialsStore store = getCredentialsStore();
			store.setUsername(getUsername());
			store.setPassword(password);
			store.flush(serverId);

			this.initialServerId = serverId;
			this.secureStoreDirty = false;
		}
		super.saveConfiguration(monitor);
	}

	private IStatus doDeleteModules(final Set<IModule> deletedModules) {
		IServerWorkingCopy wc = getServer().createWorkingCopy();
		try {
			deleteServicesOnModuleRemove.set(Boolean.FALSE);
			wc.modifyModules(null, deletedModules.toArray(new IModule[deletedModules.size()]), null);
			wc.save(false, null);
		}
		catch (CoreException e) {
			// log error to avoid pop-up dialog
			CloudFoundryPlugin
					.getDefault()
					.getLog()
					.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
							"Unexpected error while updating modules", e));
			return Status.CANCEL_STATUS;
		}
		finally {
			deleteServicesOnModuleRemove.set(Boolean.TRUE);
		}
		return Status.OK_STATUS;
	}

	public void tagAsDeployed(IModule module) {
		synchronized (this) {
			getData().tagAsDeployed(module);
		}
	}

	public CloudFoundryApplicationModule getApplicationModule(String appName) throws CoreException {

		CloudFoundryApplicationModule appModule = null;
		Collection<CloudFoundryApplicationModule> modules = getApplications();
		if (modules != null) {
			for (CloudFoundryApplicationModule module : modules) {
				if (appName.equals(module.getApplicationId())) {
					appModule = module;
					break;
				}
			}
		}
		return appModule;
	}
}
