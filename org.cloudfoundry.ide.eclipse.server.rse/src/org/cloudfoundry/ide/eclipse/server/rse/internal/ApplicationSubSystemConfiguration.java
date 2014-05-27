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
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.rse.internal;

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
 * IMPORTANT NOTE: This class can be referred by the branding extension from adopter so this class 
 * should not be moved or renamed to avoid breakage to adopters.
 * 
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
