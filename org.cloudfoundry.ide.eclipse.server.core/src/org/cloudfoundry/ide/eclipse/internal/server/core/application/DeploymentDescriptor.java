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
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationDeploymentInfo;

/**
 * Contains all the necessary information to create an application in a Cloud
 * Foundry server
 */
public class DeploymentDescriptor {

	public ApplicationDeploymentInfo deploymentInfo;

	public ApplicationAction deploymentMode;

	public ApplicationArchive applicationArchive;

	public boolean isIncrementalPublish;

	public Staging staging;

}