/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import java.util.ArrayList;
import java.util.List;

public class ServiceCommand extends CommandMetaElement {

	private final List<CommandOption> options;

	private final ExternalApplicationLaunchInfo appInfo;

	private final ServiceInfo serviceInfo;

	public ServiceCommand(ExternalApplicationLaunchInfo appInfo, ServiceInfo serviceInfo) {
		super("ServiceCommand");
		this.appInfo = appInfo;
		this.serviceInfo = serviceInfo;

		options = new ArrayList<CommandOption>();
	}

	public ExternalApplicationLaunchInfo getExternalApplicationLaunchInfo() {
		return appInfo;
	}

	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	public List<CommandOption> getOptions() {
		return options;
	}

	public static class ExternalApplicationLaunchInfo extends CommandMetaElement {

		private final String displayName;

		private final String executableName;

		public ExternalApplicationLaunchInfo(String displayName, String executableName) {
			super("ApplicationInfo");
			this.displayName = displayName;
			this.executableName = executableName;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getExecutableName() {
			return executableName;
		}
	}

	public static class ServiceInfo extends CommandMetaElement {
		private final String serviceName;

		private final String version;

		public ServiceInfo(String serviceName, String version) {
			super("ServiceInfo");
			this.serviceName = serviceName;
			this.version = version;
		}

		public String getServiceName() {
			return serviceName;
		}

		public String getVersion() {
			return version;
		}
	}

}
