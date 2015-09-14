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
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ModuleChangeEvent;
import org.eclipse.wst.server.core.IModule;

public class CloudRefreshEvent extends ModuleChangeEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final List<CloudService> services;

	public CloudRefreshEvent(CloudFoundryServer server, IModule module, int type, List<CloudService> services) {
		super(server, type, module, null);
		this.services = services;
	}

	public List<CloudService> getServices() {
		return services;
	}

}