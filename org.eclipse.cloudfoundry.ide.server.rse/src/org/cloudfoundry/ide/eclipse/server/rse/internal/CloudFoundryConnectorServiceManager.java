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

import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.AbstractConnectorServiceManager;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ISubSystem;

/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class CloudFoundryConnectorServiceManager extends AbstractConnectorServiceManager {

	private static CloudFoundryConnectorServiceManager instance;

	@Override
	public IConnectorService createConnectorService(IHost host) {
		return new CloudFoundryConnectorService(host);
	}

	@Override
	public Class getSubSystemCommonInterface(ISubSystem subsystem) {
		return IApplicationSubSystem.class;
	}

	@Override
	public boolean sharesSystem(ISubSystem otherSubSystem) {
		return (otherSubSystem instanceof IApplicationSubSystem);
	}

	public static CloudFoundryConnectorServiceManager getInstance() {
		if (instance == null) {
			instance = new CloudFoundryConnectorServiceManager();
		}
		return instance;
	}

}
