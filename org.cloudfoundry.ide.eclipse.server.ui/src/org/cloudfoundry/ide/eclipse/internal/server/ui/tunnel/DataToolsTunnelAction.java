/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.tunnel;

import java.util.Properties;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CloudFoundryEditorAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.datatools.connectivity.ConnectionProfileException;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.datatools.connectivity.drivers.DriverInstance;
import org.eclipse.datatools.connectivity.drivers.DriverManager;
import org.eclipse.datatools.connectivity.drivers.IDriverMgmtConstants;
import org.eclipse.datatools.connectivity.drivers.IPropertySet;
import org.eclipse.datatools.connectivity.drivers.PropertySetImpl;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;
import org.eclipse.datatools.connectivity.drivers.models.TemplateDescriptor;
import org.eclipse.datatools.connectivity.internal.ui.dialogs.DriverDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

/**
 * Creates a connection to a Data Tools profile based on service tunnel
 * credentials. If a tunnel doesn't already exist, a new tunnel will be created
 * automatically.
 * <p/>
 * If a Data Tools driver instance does not yet exist, this will prompt the user
 * for a driver information (e.g., the JDBC jar file for the driver in the local
 * file system).
 * <p/>
 * Data tools connection profiles and driver instance names are generated to
 * reflect the CF service and server where the service resides. Generally, they
 * should be unique enough such that they will not clash with other existing
 * profiles and driver instances.
 * <p/>
 * Existing connection profiles will be reused, if and only if the profile
 * information matches the tunnel information (i.e. username, URL,
 * databasename). Otherwise, the profile may be deleted if it contains old
 * information and replaced with a new one.
 */
public abstract class DataToolsTunnelAction extends CloudFoundryEditorAction {

	private static final String CLOUD_FOUNDRY_JDBC_PROFILE_DESCRIPTION = "Cloud Foundry JDBC Profile";

	private final static String DATA_TOOLS_DRIVER_DEFINITION_ID = "org.eclipse.datatools.connectivity.driverDefinitionID";

	private final static String ACTION_NAME = "Connect to Data Tools...";

	private DataSourceDescriptor descriptor;

	private final CloudFoundryServer cloudServer;

	private final String JOBNAME = "Creating Data Tools Connection";

	private CaldecottTunnelDescriptor tunnelDescriptor;

	protected DataToolsTunnelAction(CloudFoundryApplicationsEditorPage editorPage, DataSourceDescriptor descriptor,
			CloudFoundryServer cloudServer) {
		super(editorPage, RefreshArea.ALL);
		this.descriptor = descriptor;
		setText(ACTION_NAME);
		setImageDescriptor(CloudFoundryImages.JDBC_DATA_TOOLS);
		this.cloudServer = cloudServer;

		/**
		 * FIXNS: Disabled for CF 1.5.0 until tunnel support at client level are
		 * updated.
		 */
		setEnabled(false);
		setToolTipText(TunnelActionProvider.DISABLED_V2_TOOLTIP_MESSAGE);
	}

	public static DataToolsTunnelAction getAction(CloudFoundryApplicationsEditorPage editorPage,
			CloudService cloudService, CaldecottTunnelDescriptor descriptor, CloudFoundryServer cloudServer) {
		String serviceVendor = descriptor != null ? descriptor.getServiceVendor() : null;
		if (serviceVendor == null && cloudService != null) {
			serviceVendor = CloudUtil.getServiceVendor(cloudService);
		}

		if ("mysql".equals(serviceVendor)) {
			return MySQLDataSourceTunnelAction.getTunnelAction(editorPage, descriptor, cloudService, cloudServer);
		}
		return null;
	}

	abstract protected void setProperties(Properties properties, CaldecottTunnelDescriptor tunnelDescriptor);

	abstract protected boolean matchesProfile(IConnectionProfile connection, CaldecottTunnelDescriptor tunnelDescriptor);

	protected DataSourceDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public String getJobName() {
		return JOBNAME;
	}

	protected Job getJob() {
		Job job = super.getJob();
		job.setUser(true);
		return job;
	}

	public ICloudFoundryOperation getOperation() throws CoreException {

		return new EditorOperation() {

			@Override
			protected void performEditorOperation(IProgressMonitor monitor) throws CoreException {
				IStatus status = openConnection(monitor);
				if (!status.isOK()) {
					throw new CoreException(status);
				}
			}
		};
	}

	protected IStatus openConnection(IProgressMonitor monitor) {
		tunnelDescriptor = descriptor.getTunnelDescriptor();

		// if there is no tunnel descriptor, create the tunnel first
		if (tunnelDescriptor == null && descriptor.getCloudService() != null) {
			try {
				TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
				tunnelDescriptor = handler.startCaldecottTunnel(descriptor.getCloudService().getName(), monitor, false);
			}
			catch (CoreException e) {
				return CloudFoundryPlugin.getErrorStatus(e);
			}

		}

		if (tunnelDescriptor != null) {
			// Now check if there are any options that require values,
			// and fill in any tunnel
			// options
			// THis must be wrapped in a UI Job

			UIJob uiJob = new UIJob(getJobName()) {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {

					IConnectionProfile profile = getConnectionProfile();
					IStatus status = Status.OK_STATUS;

					if (profile != null) {
						status = profile.connect();
					}
					else {
						status = CloudFoundryPlugin.getErrorStatus("Unable to create Data Tools profile for: "
								+ tunnelDescriptor.getServiceName());
					}

					return status;
				}
			};

			uiJob.schedule();
			return Status.OK_STATUS;

		}
		else {
			return CloudFoundryPlugin.getErrorStatus("Failed to create tunnel for: "
					+ tunnelDescriptor.getServiceName());
		}
	}

	protected IConnectionProfile getConnectionProfile() {
		// Check if there is an existing profile
		IConnectionProfile profile = null;
		IConnectionProfile[] profiles = ProfileManager.getInstance().getProfiles();

		if (profiles != null) {

			// If a profile exists with the same exact name as given by the
			// CF data source integration, then check credentials. If they
			// match, use it
			// If not, delete and create a new profile. If in the future
			// profiles should also be checked against the provider (e.g.
			// mysql, psql), and a new
			// name given, also check the provider ID
			for (IConnectionProfile prf : profiles) {
				if (
				// prf.getProviderId().equals(descriptor.getDriverProviderID())
				// &&
				prf.getName().equals(descriptor.getProfileName())) {
					profile = prf;
					break;
				}
			}
		}

		// Otherwise create one, and if necessary also create a new driver
		// definition
		if (profile == null) {
			DriverInstance driverInstance = null;

			// See if there is already a driver instance that can be used;
			driverInstance = DriverManager.getInstance().getDriverInstanceByName(descriptor.getDriverName());

			// Prompt for new driver instance
			if (driverInstance == null) {

				TemplateDescriptor[] templates = TemplateDescriptor.getDriverTemplateDescriptors();
				TemplateDescriptor driverTemplate = null;

				if (templates != null) {
					for (TemplateDescriptor temp : templates) {
						String templateID = temp.getId();
						if (templateID.contains(descriptor.getDriverTemplateIdentifier())) {
							driverTemplate = temp;
							break;
						}
					}
				}

				if (driverTemplate != null) {
					IPropertySet properties = new PropertySetImpl(descriptor.getDriverName(), driverTemplate.getName());
					Properties props = new Properties();
					props.setProperty(IDriverMgmtConstants.PROP_DEFN_JARLIST, "replace with actual jar location");

					props.setProperty(IDriverMgmtConstants.PROP_DEFN_TYPE, driverTemplate.getId());

					// Must set the properties as the driver dialogue
					// throws exceptions if all properties are not set with
					// values
					// Properties get set again later regardless if a driver
					// instance
					// was created or not
					setProperties(props, tunnelDescriptor);

					properties.setBaseProperties(props);

					Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
					if (shell != null) {
						DriverDialog jdbcDriverDialog = new DriverDialog(shell, driverTemplate.getParentCategory());

						jdbcDriverDialog.setPropertySet(properties);
						jdbcDriverDialog.setEditMode(true);
						jdbcDriverDialog.setIsEditable(true);

						if (jdbcDriverDialog.open() == Window.OK) {
							properties = jdbcDriverDialog.getPropertySet();
							if (properties != null) {

								DriverManager.getInstance().addDriverInstance(properties);
								driverInstance = DriverManager.getInstance().getDriverInstanceByName(
										descriptor.getDriverName());

							}
						}
					}
				}
			}

			if (driverInstance != null) {
				Properties props = driverInstance.getPropertySet().getBaseProperties();
				props.setProperty(DATA_TOOLS_DRIVER_DEFINITION_ID, driverInstance.getId());
				try {
					String providerID = descriptor.getDriverProviderID();
					profile = ProfileManager.getInstance().createProfile(descriptor.getProfileName(),
							CLOUD_FOUNDRY_JDBC_PROFILE_DESCRIPTION, providerID, props);
				}
				catch (ConnectionProfileException e) {
					CloudFoundryPlugin.logError(e);
				}
			}
			else {
				CloudFoundryPlugin.logError("Failed to create driver instance for: "
						+ CloudUtil.getServiceVendor(descriptor.getCloudService()) + " - "
						+ tunnelDescriptor.getServiceName());
			}

		}

		// Check if the profile credentials have changed, which may happen if a
		// service
		// in CF server is deleted and then recreated again with the same name
		if (profile != null && !matchesProfile(profile, tunnelDescriptor)) {
			// Different credentials for the same profile, most likely
			// meaning that a new tunnel was created, and the old profile
			// has obsolete credentials.
			if (profile.getConnectionState() == IConnectionProfile.CONNECTED_STATE) {
				IStatus status = profile.disconnect();
				if (!status.isOK()) {
					CloudFoundryPlugin.log(status);
				}
			}

			// Change the properties to reflect changes
			// Set the properties for the tunnel values
			Properties props = profile.getBaseProperties();
			setProperties(props, tunnelDescriptor);
			profile.setBaseProperties(props);

		}

		return profile;

	}

	/**
	 * 
	 * Descriptor that contains information necessary to create a connection to
	 * a data tools profile
	 * 
	 */
	static class DataSourceDescriptor {

		protected final CaldecottTunnelDescriptor tunnelDescriptor;

		protected final CloudService cloudService;

		protected final String profileName;

		protected final String driverProviderID;

		protected final String driverName;

		protected final String driverTemplateIdentifier;

		public DataSourceDescriptor(CaldecottTunnelDescriptor tunnelDescriptor, CloudService cloudService,
				String profileName, String driverProviderID, String driverName, String driverTemplateIdentifier) {
			this.tunnelDescriptor = tunnelDescriptor;
			this.cloudService = cloudService;
			this.profileName = profileName;
			this.driverProviderID = driverProviderID;
			this.driverName = driverName;
			this.driverTemplateIdentifier = driverTemplateIdentifier;
		}

		public CaldecottTunnelDescriptor getTunnelDescriptor() {
			return tunnelDescriptor;
		}

		public CloudService getCloudService() {
			return cloudService;
		}

		/**
		 * 
		 * @return A unique name given to the data tools connection profile for
		 * the given CF service.
		 */
		public String getProfileName() {
			return profileName;
		}

		/**
		 * E.g. org.eclipse.datatools.enablement.mysql.connectionProfile for
		 * MySQL Data Tools provider.
		 * @return the data tools provider ID for the given data service vendor.
		 */
		public String getDriverProviderID() {
			return driverProviderID;
		}

		/**
		 * A unique name given to the driver instance that should be used by the
		 * profile. This driver instance points to the JDBC jar file in the
		 * local file system and may have additional configuration.
		 * @return driver name to be used by the connection profile
		 */
		public String getDriverName() {
			return driverName;
		}

		/**
		 * A template identifier is a portion of a driver template ID that
		 * should be used to match a template that corresponds to a desired
		 * driver template. For example, "mysql" to find a MySQL Data Tools
		 * templates. Templates are needed in order to correctly create a Data
		 * Tools driver instance.
		 * @return a driver template identifier like "mysql". Used to search for
		 * registered Data Tools driver templates
		 */
		public String getDriverTemplateIdentifier() {
			return driverTemplateIdentifier;
		}

	}

	static class MySQLDataSourceTunnelAction extends DataToolsTunnelAction {

		public MySQLDataSourceTunnelAction(CloudFoundryApplicationsEditorPage editorPage,
				DataSourceDescriptor descriptor, CloudFoundryServer cloudServer) {
			super(editorPage, descriptor, cloudServer);
		}

		@Override
		protected void setProperties(Properties properties, CaldecottTunnelDescriptor tunnelDescriptor) {
			properties.setProperty(IJDBCConnectionProfileConstants.USERNAME_PROP_ID, tunnelDescriptor.getUserName());
			properties.setProperty(IJDBCConnectionProfileConstants.PASSWORD_PROP_ID, tunnelDescriptor.getPassword());
			properties.setProperty(IJDBCConnectionProfileConstants.URL_PROP_ID, tunnelDescriptor.getURL());
			properties.setProperty(IJDBCConnectionProfileConstants.DATABASE_NAME_PROP_ID,
					tunnelDescriptor.getDatabaseName());
		}

		/**
		 * 
		 * @param editorPage not null
		 * @param tunnelDescriptor it may be null at this stage if the tunnel
		 * hasn't yet been created
		 * @param cloudService not null
		 * @param cloudServer not null
		 * @return
		 */
		static DataToolsTunnelAction getTunnelAction(CloudFoundryApplicationsEditorPage editorPage,
				CaldecottTunnelDescriptor tunnelDescriptor, CloudService cloudService, CloudFoundryServer cloudServer) {

			String profileName = "Cloud Foundry" + " - " + cloudServer.getServer().getName() + " - "
					+ CloudUtil.getServiceVendor(cloudService) + " - " + cloudService.getName();

			String driverProviderID = "org.eclipse.datatools.enablement.mysql.connectionProfile";

			String driverName = "Cloud Foundry Tunnel " + " " + CloudUtil.getServiceVendor(cloudService) + " "
					+ cloudService.getVersion();

			String driverTemplateIdentifier = "mysql";

			DataSourceDescriptor dataSourceDescriptor = new DataSourceDescriptor(tunnelDescriptor, cloudService,
					profileName, driverProviderID, driverName, driverTemplateIdentifier);
			return new MySQLDataSourceTunnelAction(editorPage, dataSourceDescriptor, cloudServer);
		}

		@Override
		protected boolean matchesProfile(IConnectionProfile connection, CaldecottTunnelDescriptor tunnelDescriptor) {
			Properties properties = connection.getBaseProperties();
			String userName = properties.getProperty(IJDBCConnectionProfileConstants.USERNAME_PROP_ID);

			String password = properties.getProperty(IJDBCConnectionProfileConstants.PASSWORD_PROP_ID);

			String url = properties.getProperty(IJDBCConnectionProfileConstants.URL_PROP_ID);

			String dataBaseName = properties.getProperty(IJDBCConnectionProfileConstants.DATABASE_NAME_PROP_ID);

			// For MySQL username, url and database name should not be null in
			// the tunnel descriptor.
			return tunnelDescriptor.getUserName().equals(userName) && tunnelDescriptor.getURL().equals(url)
					&& tunnelDescriptor.getDatabaseName().equals(dataBaseName)
					&& tunnelDescriptor.getPassword().equals(password);
		}

	}

}
