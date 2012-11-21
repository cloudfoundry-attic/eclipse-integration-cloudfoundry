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

public class ServiceCommand extends CommandMetaElement {

	private CommandOptions options;

	private final ExternalApplicationLaunchInfo appInfo;

	private final ServiceInfo serviceInfo;

	public ServiceCommand(ExternalApplicationLaunchInfo appInfo, ServiceInfo serviceInfo, CommandOptions options) {
		super("ServiceCommand");
		this.appInfo = appInfo;
		this.serviceInfo = serviceInfo;
		this.options = options;
	}

	public ExternalApplicationLaunchInfo getExternalApplicationLaunchInfo() {
		return appInfo;
	}

	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	public CommandOptions getOptions() {
		return options;
	}

	public ServiceCommand getServiceCommand(String location, String displayName, String options) {
		CommandOptions commandOptions = new CommandOptions(options);
		ServiceCommand command = new ServiceCommand(new ExternalApplicationLaunchInfo(displayName, location),
				serviceInfo, commandOptions);
		return command;
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
