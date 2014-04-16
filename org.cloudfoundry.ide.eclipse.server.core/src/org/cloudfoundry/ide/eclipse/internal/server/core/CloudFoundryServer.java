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
 *     Keith Chong, IBM - Allow module to bypass facet check 
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleCache.ServerData;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRegistry;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.SelfSignedStore;
import org.cloudfoundry.ide.eclipse.internal.server.core.spaces.CloudFoundrySpace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;

/**
 * Local representation of a Cloud Foundry server, with API to obtain local
 * {@link IModule} for deployed applications, as well as persist local server
 * information like the server name, or user credentials.
 * <p/>
 * Note that a local cloud foundry server is an instance that may be discarded
 * and created multiple times by the underlying local server framework even
 * while the same server is still connected, typically when server changes are
 * saved (e.g. changing a server name), therefore the server instance should NOT
 * hold state intended to be present during the life span of an Eclipse runtime
 * session. Use an appropriate caching mechanism if application or server state
 * needs to be cached during a runtime session. See {@link ModuleCache}.
 * <p/>
 * In addition, the local server instance delegates to a
 * {@link CloudFoundryServerBehaviour} for ALL CF client calls. Do NOT add
 * client calls in the server instance. These should be added to the
 * {@link CloudFoundryServerBehaviour}.
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class CloudFoundryServer extends ServerDelegate implements IURLProvider {

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

	public void updateApplicationModule(CloudFoundryApplicationModule module) {
		if (getData() != null) {
			getData().updateCloudApplicationModule(module);
		}
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
				// If the module, in a non-faceted project, has been determined
				// to be deployable to CF (ie. a single zip application
				// archive), then
				// this facet check is unnecessary.
				boolean ignoreFacetCheck = false;
				// FIXNS: Enable with IModule2 workaround is in place, as its
				// not available in Eclipse 4.3 and older.
//				 if (module instanceof IModule2) {
//					 String property = ((IModule2)module).getProperty(CloudFoundryConstants.PROPERTY_MODULE_NO_FACET);
//					 if (property != null && property.equals("true")) {
//						 ignoreFacetCheck = true;
//					 }
//				 }

// Workaround - Remove the following and use the above commented out code
				ClassLoader classLoader = module.getClass().getClassLoader();
				if (classLoader != null) {
					try {
						Class iModule2 = classLoader.loadClass("org.eclipse.wst.server.core.IModule2");
						if (iModule2 != null) {
							Method getProperty = iModule2.getMethod("getProperty", String.class);
							Object o = getProperty.invoke(module, CloudFoundryConstants.PROPERTY_MODULE_NO_FACET);
							if (o instanceof String && ((String)o).equals("true")) {
								ignoreFacetCheck = true;
							}
						}
					} catch (Exception e) {
						// If any issues, just go ahead and do the facet check below
					}
				}
// End of workaround

				if (module.getProject() != null && !ignoreFacetCheck) {
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
		ServerData data = getData();
		if (data != null) {
			data.clear();
		}
	}

	public IStatus error(String message, Exception e) {
		return new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind("{0} [{1}]", message, getServer()
				.getName()), e);
	}

	/**
	 * Fetches the corresponding Cloud Foundry-aware module for the given WST
	 * IModule. The Cloud Foundry-aware module contains additional information
	 * that is specific to Cloud Foundry. It will not create the module if it
	 * does not exist. For most cases where an cloud application module is
	 * expected to already exist, this method is preferable than
	 * {@link #getCloudModule(IModule)}, and avoids possible bugs when an
	 * application is being deleted. See {@link #getCloudModule(IModule)}.
	 * @param module WST local module
	 * @return Cloud module, if it exists, or null.
	 */
	public CloudFoundryApplicationModule getExistingCloudModule(IModule module) {
		if (module instanceof CloudFoundryApplicationModule) {
			return (CloudFoundryApplicationModule) module;
		}

		return getData() != null ? getData().getExistingCloudModule(module) : null;
	}

	private CloudFoundryApplicationModule getOrCreateCloudModule(IModule module) {
		if (module instanceof CloudFoundryApplicationModule) {
			return (CloudFoundryApplicationModule) module;
		}

		return getData() != null ? getData().getOrCreateCloudModule(module) : null;
	}

	/**
	 * Gets an existing Cloud module for the given {@link IModule} or if it
	 * doesn't exist, it will attempt to create it.
	 * <p/>
	 * NOTE: care should be taken when invoking this method. Only invoke in
	 * cases where a cloud module may not yet exist, for example, when
	 * refreshing list of currently deployed applications for the first time, or
	 * deploying an application for the first time. If a cloud module is already
	 * expected to exist for some operation (e.g., modifying properties for an
	 * application that is already deployed, like scaling memory, changing
	 * mapped URLs, binding services etc..) , use
	 * {@link #getExistingCloudModule(IModule)} instead. The reason for this is
	 * to avoid recreating a module that may be in the processing of being
	 * deleted by another operation, but the corresponding WST {@link IModule}
	 * may still be referenced by the local server. Using
	 * {@link #getExistingCloudModule(IModule)} is also preferable for better
	 * error detection, as if a module is expected to exist for an operation,
	 * but it doesn't it may indicate that an error occurred in refreshing the
	 * list of deployed applications.
	 * @param module
	 * @return existing cloud module, or if not yet created, creates and returns
	 * it.
	 */
	public CloudFoundryApplicationModule getCloudModule(IModule module) {
		if (module == null) {
			return null;
		}
		return getOrCreateCloudModule(module);
	}

	/**
	 * Update all Cloud Modules for the list of local server {@link IModule}. If
	 * all modules have been mapped to a Cloud module, {@link IStatus#OK} is
	 * returned. If no modules are present (nothing is deployed),
	 * {@link IStatus#OK} also returned. Otherwise, if there are modules with
	 * missing mapped Cloud Application modules, {@link IStatus#ERROR} is
	 * returned.
	 * @return {@link IStatus#OK} if all local server {@link IModule} have a
	 * Cloud Application module mapping, or list of {@link IModule} in the
	 * server is empty. Otherwise, {@link IStatus#ERROR} returned.
	 */
	public IStatus refreshCloudModules() {
		if (getServerOriginal() == null) {
			return CloudFoundryPlugin
					.getErrorStatus("Server original for " + getDeploymentName() + " cannot be found.");
		}
		IModule[] modules = getServerOriginal().getModules();
		if (modules != null) {
			StringWriter writer = new StringWriter();

			for (IModule module : modules) {
				CloudFoundryApplicationModule appModule = getCloudModule(module);
				if (appModule == null) {
					writer.append("Failed to create Cloud Foundry application module for: ");
					writer.append(module.getId());
					writer.append('\n');
				}
			}
			String message = writer.toString();
			if (message.length() > 0) {
				CloudFoundryPlugin.getErrorStatus(message);
			}
		}

		return Status.OK_STATUS;
	}

	/**
	 * Does not refresh the list of application modules. Returns the cached
	 * list, which may be empty.
	 * @return never null. May be empty
	 */
	public Collection<CloudFoundryApplicationModule> getExistingCloudModules() {
		return getData() != null ? getData().getExistingCloudModules()
				: new ArrayList<CloudFoundryApplicationModule>(0);
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

	/**
	 * Returns the cached server data for the server. In some case the data may
	 * be null, if the server has not yet been created but it's available to be
	 * configured (e.g while a new server instance is being created).
	 * @return cached server data. May be null.
	 */
	private ServerData getData() {
		return CloudFoundryPlugin.getModuleCache().getData(getServerOriginal());
	}

	public String getDeploymentName() {
		return getAttribute(PROPERTY_DEPLOYMENT_NAME, "");
	}

	public String getPassword() {
		if (secureStoreDirty) {
			return password;
		}
		String cachedPassword = getData() != null ? getData().getPassword() : null;
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

	public boolean hasCloudSpace() {
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
			if (getData() != null) {
				for (IModule module : remove) {
					getData().tagAsDeployed(module);
				}
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

			if (getData() != null) {
				for (IModule module : add) {
					// avoid automatic deletion before module has been deployed
					getData().tagAsUndeployed(module);
				}
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
		getServerWorkingCopy().setHost("Cloud");

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

		if (getData() != null) {
			getData().setPassword(password);
		}
	}

	public void setSpace(CloudSpace space) {

		secureStoreDirty = true;

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

	/**
	 * 
	 * @return true if server uses self-signed certificates. False otherwise,
	 * including if server preference can't be resolved.
	 */
	public boolean getSelfSignedCertificate() {
		try {
			return new SelfSignedStore(getUrl()).isSelfSignedCert();
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return false;
	}

	public void setSelfSignedCertificate(boolean isSelfSigned) {
		setSelfSignedCertificate(isSelfSigned, getUrl());
	}

	private void updateServerId() {
		StringWriter writer = new StringWriter();
		writer.append(getUsername());
		if (hasCloudSpace()) {
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

	/**
	 * Update the local (WST) ( {@link IModule} ) and corresponding cloud module
	 * ( {@link CloudFoundryApplicationModule} ) such that they are in synch
	 * with the actual deployed applications (represented by
	 * {@link CloudApplication} ). Local WST modules ( {@link IModule} ) that do
	 * not have a corresponding deployed application ( {@link CloudApplication})
	 * will be removed.
	 * @param deployedApplications
	 * @throws CoreException
	 */
	public void updateModules(Map<String, CloudApplication> deployedApplications) throws CoreException {
		Server server = (Server) getServer();

		final Set<CloudFoundryApplicationModule> allModules = new HashSet<CloudFoundryApplicationModule>();
		List<CloudFoundryApplicationModule> externalModules = new ArrayList<CloudFoundryApplicationModule>();
		final Set<IModule> deletedModules = new HashSet<IModule>();

		synchronized (this) {
			// Iterate through the local WST modules, and update them based on
			// which are external (have no accessible workspace resources),
			// which
			// have no corresponding deployed application .
			// Note that some IModules may also be in the process of being
			// deleted. DO NOT recreate cloud application modules for these
			// CHANGE
			for (IModule module : server.getModules()) {
				// Find the corresponding Cloud Foundry application module for
				// the given WST server IModule
				CloudFoundryApplicationModule cloudModule = getCloudModule(module);

				if (cloudModule == null) {
					CloudFoundryPlugin.logError("Unable to find local Cloud Foundry application module for : "
							+ module.getName()
							+ ". Try refreshing applications or disconnecting and reconnecting to the server.");
					continue;
				}

				// Now process the deployed application, and re-categorise it if
				// necessary (i.e. whether it's external or not)
				CloudApplication actualApplication = deployedApplications.remove(cloudModule
						.getDeployedApplicationName());

				// Update the cloud module mapping to the cloud application,
				// such that the cloud module
				// has the latest cloud application reference.
				cloudModule.setCloudApplication(actualApplication);

				// the modules maps to an existing application
				if (actualApplication != null) {
					if (cloudModule.isExternal()) {
						externalModules.add(cloudModule);
					}
					allModules.add(cloudModule);
				}
				else if (getData() != null && getData().isUndeployed(module)) {
					// deployment is still in progress
					allModules.add(cloudModule);
				}
				else {
					// the module maps to an application that no longer exists
					deletedModules.add(module);
				}
			}

			// create modules for new applications
			if (getData() != null) {
				for (CloudApplication application : deployedApplications.values()) {
					CloudFoundryApplicationModule appModule = getData().createModule(application);
					externalModules.add(appModule);
					allModules.add(appModule);
				}
			}

			// update state for cloud applications
			server.setExternalModules(externalModules.toArray(new IModule[0]));

			for (IModule module : server.getModules()) {
				CloudFoundryApplicationModule appModule = getExistingCloudModule(module);
				if (appModule != null) {
					updateState(server, appModule);
				}
			}

			// FIXNS: This seems to trigger an infinite "recursion", since
			// deleteModules(..) delegates to the server behaviour, which then
			// attempts to delete modules in a server instance that is not saved
			// and when server behaviour delete operation is complete, it will
			// trigger a refresh operation which then proceeds to update
			// modules, but since WST still indicates that the module has not
			// been deleted
			// deleteModule size will not be empty, which will again invoke the
			// server behaviour...
			// update state for deleted applications to trigger a refresh
			if (deletedModules.size() > 0) {
				for (IModule module : deletedModules) {
					server.setModuleState(new IModule[] { module }, IServer.STATE_UNKNOWN);
				}
				deleteModules(deletedModules);
			}

			if (getData() != null) {
				getData().removeObsoleteModules(allModules);
			}
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
		if (getData() != null) {
			getData().remove(cloudModule);
		}
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

			if (getData() != null) {
				getData().updateServerId(initialServerId, serverId);

				// cache password
				getData().setPassword(password);
			}

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

	public IStatus doDeleteModules(final Collection<IModule> deletedModules) {
		IServerWorkingCopy wc = getServer().createWorkingCopy();
		try {
			deleteServicesOnModuleRemove.set(Boolean.FALSE);
			wc.modifyModules(null, deletedModules.toArray(new IModule[deletedModules.size()]), null);
			wc.save(true, null);
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
			if (getData() != null) {
				getData().tagAsDeployed(module);
			}
		}
	}

	/**
	 * @return Cloud application module, if it exists for the given app name.
	 * Null otherwise.
	 */
	public CloudFoundryApplicationModule getExistingCloudModule(String appName) throws CoreException {

		CloudFoundryApplicationModule appModule = null;
		Collection<CloudFoundryApplicationModule> modules = getExistingCloudModules();
		if (modules != null) {
			for (CloudFoundryApplicationModule module : modules) {
				if (appName.equals(module.getDeployedApplicationName())) {
					appModule = module;
					break;
				}
			}
		}
		return appModule;
	}

	/**
	 * Convinience method to set signed certificate for server URLs that do not
	 * yet have a server instance (e.g. when managing server URLs)
	 * @param isSelfSigned true if server uses self-signed certificate
	 * @param cloudServerURL non-null Cloud Foundry server URL
	 */
	public static void setSelfSignedCertificate(boolean isSelfSigned, String cloudServerURL) {
		try {
			new SelfSignedStore(cloudServerURL).setSelfSignedCert(isSelfSigned);
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	public URL getModuleRootURL(final IModule curModule) {
		// Only publish if the server publish state is not synchronized.
		CloudFoundryApplicationModule cloudModule = getCloudModule(curModule);
		if (cloudModule == null) {
			return null;
		}

		// verify that URIs are set, as it may be a standalone application with
		// no URI
		List<String> uris = cloudModule != null && cloudModule.getApplication() != null ? cloudModule.getApplication()
				.getUris() : null;
		if (uris != null && !uris.isEmpty()) {
			try {
				return new URL("http://" + uris.get(0));
			}
			catch (MalformedURLException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return null;
	}
}
