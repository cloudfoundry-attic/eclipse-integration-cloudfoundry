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
package org.cloudfoundry.ide.eclipse.server.rse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.BasicConnectorService;

/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class CloudFoundryConnectorService extends BasicConnectorService {

	public CloudFoundryConnectorService(IHost host) {
		super("Cloud Service Connector", "Manages connections to a cloud service", host, 80);
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
