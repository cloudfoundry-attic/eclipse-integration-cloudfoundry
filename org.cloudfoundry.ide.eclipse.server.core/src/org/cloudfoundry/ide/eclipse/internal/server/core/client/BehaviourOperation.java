/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Behaviour operation that stops the refresh job prior to executing the
 * operation, and restarts it afterward.
 * 
 */
public abstract class BehaviourOperation implements ICloudFoundryOperation {

	private final CloudFoundryServerBehaviour behaviour;

	public BehaviourOperation(CloudFoundryServerBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		behaviour.stopRefreshModules();

		try {
			performOperation(monitor);
		}
		finally {
			behaviour.refreshModules(monitor);
		}

	}

	protected abstract void performOperation(IProgressMonitor monitor) throws CoreException;

}