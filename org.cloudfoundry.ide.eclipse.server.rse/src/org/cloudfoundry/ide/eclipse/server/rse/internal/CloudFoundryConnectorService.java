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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.BasicConnectorService;

/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class CloudFoundryConnectorService extends BasicConnectorService {

	public CloudFoundryConnectorService(IHost host) {
		super("Cloud Service Connector", "Manages connections to a cloud service", host, 80); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isConnected() {
		return true;
	}

	@Override
	protected void internalConnect(IProgressMonitor monitor) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void internalDisconnect(IProgressMonitor monitor) throws Exception {
		// TODO Auto-generated method stub

	}

}
