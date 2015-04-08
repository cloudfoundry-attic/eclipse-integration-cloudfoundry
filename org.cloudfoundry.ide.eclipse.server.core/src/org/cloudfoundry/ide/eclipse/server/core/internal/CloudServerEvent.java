/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Leo Dos Santos
 * @author Terry Denney
 */
public class CloudServerEvent extends EventObject {

	public static final int EVENT_INSTANCES_UPDATED = 100;

	public static final int EVENT_UPDATE_SERVICES = 200;

	public static final int EVENT_UPDATE_PASSWORD = 300;

	public static final int EVENT_APPLICATION_REFRESHED = 310;

	public static final int EVENT_SERVER_REFRESHED = 400;

	public static final int EVENT_APP_DEPLOYMENT_CHANGED = 410;

	public static final int EVENT_APP_DELETED = 420;

	public static final int EVENT_APP_STOPPED = 425;

	public static final int EVENT_APP_STARTED = 427;

	public static final int EVENT_APP_URL_CHANGED = 430;

	public static final int EVENT_APP_DEBUG = 500;

	public static final int EVENT_CLOUD_OP_ERROR = 600;

	private static final long serialVersionUID = 1L;

	private int type = -1;

	private IStatus status;

	public CloudServerEvent(CloudFoundryServer server) {
		this(server, -1);
	}

	public CloudServerEvent(CloudFoundryServer server, int type) {
		this(server, type, null);
	}

	public CloudServerEvent(CloudFoundryServer server, int type, IStatus status) {
		super(server);
		Assert.isNotNull(server);
		this.type = type;
		this.status = status;
	}

	public CloudFoundryServer getServer() {
		return (CloudFoundryServer) getSource();
	}

	public int getType() {
		return type;
	}

	public IStatus getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return "CloudServerEvent [type=" + type + ", server=" + getServer().getServer().getId() + ")]";
	}
}
