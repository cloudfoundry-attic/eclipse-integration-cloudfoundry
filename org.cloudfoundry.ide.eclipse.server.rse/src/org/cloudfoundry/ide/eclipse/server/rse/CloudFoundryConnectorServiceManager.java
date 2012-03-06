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
