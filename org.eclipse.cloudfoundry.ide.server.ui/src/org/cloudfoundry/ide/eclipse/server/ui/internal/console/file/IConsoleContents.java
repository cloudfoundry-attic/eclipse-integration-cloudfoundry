/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.console.file;

import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;

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