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
package org.eclipse.cft.server.core.internal.application;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;

/**
 * Event indicating any type of change on a given module, whether the publish
 * state has changed (added or removed), module is started/stopped, or any
 * service bindings, URL mapping changes, or scaling that occurs for the
 * associated Cloud application. It may also be used to indicate that the
 * module's application has been updated from Cloud space.
 *
 */
public class ModuleChangeEvent extends CloudServerEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final IStatus status;

	private final IModule module;

	public ModuleChangeEvent(CloudFoundryServer server, int type, IModule module, IStatus status) {
		super(server, type);
		this.status = status != null ? status : Status.OK_STATUS;
		this.module = module;
	}

	public IStatus getStatus() {
		return status;
	}

	public IModule getModule() {
		return module;
	}

}
