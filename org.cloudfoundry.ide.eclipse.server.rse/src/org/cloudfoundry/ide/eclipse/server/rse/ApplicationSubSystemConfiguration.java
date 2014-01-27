/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.rse;

import org.cloudfoundry.ide.eclipse.internal.server.core.ServerEventHandler;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.filters.ISystemFilterPoolManager;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.services.clientserver.SystemSearchString;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.search.IHostSearchResultConfiguration;
import org.eclipse.rse.services.search.IHostSearchResultSet;
import org.eclipse.rse.services.search.ISearchService;
import org.eclipse.rse.subsystems.files.core.ILanguageUtilityFactory;
import org.eclipse.rse.subsystems.files.core.model.RemoteFileFilterString;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.FileServiceSubSystem;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.FileServiceSubSystemConfiguration;
import org.eclipse.rse.subsystems.files.core.subsystems.IHostFileToRemoteFileAdapter;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;
import org.eclipse.wst.server.core.ServerCore;



/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class ApplicationSubSystemConfiguration extends FileServiceSubSystemConfiguration {

	private IHostFileToRemoteFileAdapter fileAdapter;

	public ApplicationSubSystemConfiguration() {
		super();
		setIsUnixStyle(true);
	}

	public IFileService createFileService(IHost host) {
		return new CloudFoundryFileService(host);
	}

	public IHostSearchResultConfiguration createSearchConfiguration(IHost host, IHostSearchResultSet resultSet,
			Object searchTarget, SystemSearchString searchString) {
		// TODO Auto-generated method stub
		return null;
	}

	public ISearchService createSearchService(IHost host) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISubSystem createSubSystemInternal(IHost conn) {
		CloudFoundryConnectorService connectorService = (CloudFoundryConnectorService) getConnectorService(conn);
		ISubSystem subsys = new ApplicationSubSystem(conn, connectorService, getFileService(conn),
				getHostFileAdapter(), getSearchService(conn));
		return subsys;
	}

	public IConnectorService getConnectorService(IHost conn) {
		return CloudFoundryConnectorServiceManager.getInstance().getConnectorService(conn, getServiceImplType());
	}

	public IHostFileToRemoteFileAdapter getHostFileAdapter() {
		if (fileAdapter == null) {
			fileAdapter = new CloudFoundryFileAdapter();
		}
		return fileAdapter;
	}

	public ILanguageUtilityFactory getLanguageUtilityFactory(IRemoteFileSubSystem ss) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class getServiceImplType() {
		return ICloudFoundryFileService.class;
	}

	@Override
	public boolean isFactoryFor(Class subSystemType) {
		boolean isFor = FileServiceSubSystem.class.equals(subSystemType);
		return isFor;
	}

	public void setConnectorService(IHost host, IConnectorService connectorService) {
		CloudFoundryConnectorServiceManager.getInstance().setConnectorService(host, getServiceImplType(), connectorService);
	}

	public boolean supportsArchiveManagement() {
		return false;
	}

	@Override
	public boolean supportsSubSystemConnect() {
		return false;
	}

	@Override
	protected void addSubSystem(ISubSystem subsys) {
		super.addSubSystem(subsys);
		if (subsys instanceof ApplicationSubSystem) {
			ServerCore.addServerLifecycleListener((ApplicationSubSystem) subsys);
			ServerEventHandler.getDefault().addServerListener((ApplicationSubSystem) subsys);
		}
	}

	@Override
	protected ISystemFilterPool createDefaultFilterPool(ISystemFilterPoolManager mgr) {
		ISystemFilterPool pool = null;
		try {
			String poolName = getDefaultFilterPoolName(mgr.getName(), getId());
			pool = mgr.createSystemFilterPool(poolName, false);
			if (pool != null) {
				RemoteFileFilterString accountsFilterString = new RemoteFileFilterString(this);
				accountsFilterString.setPath(getSeparator());
				String[] filterStrings = new String[] { accountsFilterString.toString() };
				ISystemFilter filter = mgr.createSystemFilter(pool, "Accounts", filterStrings);
				filter.setNonDeletable(true);
				filter.setNonRenamable(true);
			}
		}
		catch (Exception e) {
			CloudFoundryRsePlugin.logError("An error occurred creating default filter pool", e);
		}
		return pool;
	}

	@Override
	protected void removeSubSystem(ISubSystem subsys) {
		if (subsys instanceof ApplicationSubSystem) {
			ServerCore.removeServerLifecycleListener((ApplicationSubSystem) subsys);
			ServerEventHandler.getDefault().removeServerListener((ApplicationSubSystem) subsys);
		}
		super.removeSubSystem(subsys);
	}

}
