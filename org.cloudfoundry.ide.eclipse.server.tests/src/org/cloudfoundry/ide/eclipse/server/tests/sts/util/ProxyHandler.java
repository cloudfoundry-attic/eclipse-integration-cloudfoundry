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
package org.cloudfoundry.ide.eclipse.server.tests.sts.util;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;

public abstract class ProxyHandler {

	private final String host;

	private final int port;

	private IProxyData[] originalData;

	private boolean originalProxiesEnabled;

	private boolean originalSystemProxiesEnabled;

	private final boolean enableProxies;

	private final String proxyDataType;

	public ProxyHandler(String host, int port) {
		this.host = host;
		this.port = port;
		enableProxies = true;
		proxyDataType = IProxyData.HTTP_PROXY_TYPE;
	}

	public ProxyHandler(String host, int port, boolean enableProxies, String proxyDataType) {
		this.host = host;
		this.port = port;
		this.enableProxies = enableProxies;
		this.proxyDataType = proxyDataType;
	}

	protected IProxyService getProxyService() {
		return CloudFoundryPlugin.getDefault().getProxyService();
	}

	protected IProxyData[] getOriginalProxyData() {
		return originalData;
	}

	protected boolean getOriginalProxiesEnabled() {
		return originalProxiesEnabled;

	}

	protected boolean getOriginalSystemProxiesEnabled() {
		return originalSystemProxiesEnabled;
	}

	public void run() throws CoreException {
		IProxyService proxyService = getProxyService();
		originalSystemProxiesEnabled = proxyService.isSystemProxiesEnabled();
		originalProxiesEnabled = proxyService.isProxiesEnabled();
		originalData = proxyService.getProxyData();

		try {
			// set new proxy
			proxyService.setSystemProxiesEnabled(false);
			proxyService.setProxiesEnabled(enableProxies);
			IProxyData[] data = proxyService.getProxyData();
			IProxyData matchedData = null;

			for (IProxyData singleData : data) {
				if (singleData.getType().equals(proxyDataType)) {
					matchedData = singleData;
					break;
				}
			}

			if (matchedData == null) {
				throw new CoreException(CloudFoundryPlugin.getErrorStatus("No matched proxy data type found for: "
						+ proxyDataType));
			}

			matchedData.setHost(host);
			matchedData.setPort(port);
			proxyService.setProxyData(data);

			handleProxyChange();
		}
		finally {
			// restore proxy settings
			proxyService.setSystemProxiesEnabled(originalSystemProxiesEnabled);
			proxyService.setProxiesEnabled(originalProxiesEnabled);
			proxyService.setProxyData(originalData);
		}
	}

	protected abstract void handleProxyChange() throws CoreException;
}
