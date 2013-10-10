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


/**
 * A working copy of an application's {@link ApplicationDeploymentInfo}. Changes are not persisted in a related {@link CloudFoundryApplicationModule} unless
 * {@link #save()} is performed
 */
public abstract class DeploymentInfoWorkingCopy extends ApplicationDeploymentInfo {


	public DeploymentInfoWorkingCopy(String appName) {
		super(appName);
	}

	/**
	 * Saves the working copy in an associated {@link CloudFoundryApplicationModule}
	 */
	abstract public void save();

}
