/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;

public class RemapModuleProjectCommand extends UpdateMappingCommand {

	protected ICloudFoundryOperation getCloudOperation(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {

		if (partSite == null || partSite.getShell() == null) {
			CloudFoundryPlugin.logError("Unable to remap project. No shell resolved to open dialogues."); //$NON-NLS-1$
		}
		else {
			return new MapToProjectOperation(appModule, cloudServer, partSite.getShell());
		}
		return null;
	}
}
