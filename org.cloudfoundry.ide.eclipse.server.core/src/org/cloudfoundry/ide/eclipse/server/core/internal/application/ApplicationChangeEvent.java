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
package org.cloudfoundry.ide.eclipse.server.core.internal.application;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ApplicationChangeEvent extends CloudServerEvent {

	private final IStatus status;

	private final CloudFoundryApplicationModule appModule;

	public ApplicationChangeEvent(CloudFoundryServer server, int type, CloudFoundryApplicationModule appModule,
			IStatus status) {
		super(server, type);
		this.status = status != null ? status : Status.OK_STATUS;
		this.appModule = appModule;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IStatus getStatus() {
		return status;
	}

	public CloudFoundryApplicationModule getApplication() {
		return appModule;
	}

}
