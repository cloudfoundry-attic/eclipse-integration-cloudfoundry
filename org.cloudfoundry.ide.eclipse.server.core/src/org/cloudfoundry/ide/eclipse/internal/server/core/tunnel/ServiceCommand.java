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

/**
 * Using getters and setters for JSON serialisation.
 * 
 */
public class ServiceCommand {

	private CommandOptions options;

	private ExternalApplicationLaunchInfo appInfo;

	private ServiceInfo serviceInfo;

	public ServiceCommand() {

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

	public void setExternalApplicationLaunchInfo(ExternalApplicationLaunchInfo appInfo) {
		this.appInfo = appInfo;
	}

	public void setServiceInfo(ServiceInfo serviceInfo) {
		this.serviceInfo = serviceInfo;
	}

	public void setOptions(CommandOptions options) {
		this.options = options;
	}

	/**
	 * 
	 */
	public static class ExternalApplicationLaunchInfo {

		private String displayName;

		private String executableName;

		public ExternalApplicationLaunchInfo() {

		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public void setExecutableName(String executableName) {
			this.executableName = executableName;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getExecutableName() {
			return executableName;
		}
	}

	public static class ServiceInfo {

		private String serviceName;

		private String version;

		public ServiceInfo() {

		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		public void setVersion(String version) {
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
