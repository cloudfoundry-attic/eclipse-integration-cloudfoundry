/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;

public interface IConsoleContents {

	/**
	 * Return a list of streams that provide content to the Cloud Foundry
	 * console. user.
	 * @param cloudServer
	 * @param appName
	 * @return
	 */
	public List<ICloudFoundryConsoleStream> getContents(CloudFoundryServer cloudServer, String appName,
			int instanceIndex);

}