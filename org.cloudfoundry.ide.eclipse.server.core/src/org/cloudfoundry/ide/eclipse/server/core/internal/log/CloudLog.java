package org.cloudfoundry.ide.eclipse.server.core.internal.log;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.IAdaptable;

/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Apache License, Version 2.0 (the "License�);
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors: Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
public class CloudLog implements IAdaptable {

	private final LogContentType logType;

	private final String log;

	private final CloudFoundryServer cloudServer;

	private final CloudFoundryApplicationModule appModule;

	public CloudLog(String log, LogContentType logType) {
		this(log, logType, null, null);
	}

	public CloudLog(String log, LogContentType logType, CloudFoundryServer cloudServer,
			CloudFoundryApplicationModule appModule) {
		this.logType = logType;
		this.log = log;
		this.cloudServer = cloudServer;
		this.appModule = appModule;
	}

	public LogContentType getLogType() {
		return logType;
	}

	public String getLog() {
		return log;
	}

	public Object getAdapter(Class clazz) {
		if (clazz.equals(CloudFoundryServer.class)) {
			return cloudServer;
		}
		else if (clazz.equals(CloudFoundryApplicationModule.class)) {
			return appModule;
		}

		return null;
	}

	public String toString() {
		return logType + " - " + log; //$NON-NLS-1$
	}

}
