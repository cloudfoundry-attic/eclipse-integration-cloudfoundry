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
package org.cloudfoundry.ide.eclipse.server.tests.util;

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;

public class CloudFoundryTestUtil {

	/**
	 * @param host
	 * @param proxyType
	 * @return proxy or null if it cannot be resolved
	 */
	public static Proxy getProxy(String host, String proxyType) {
		Proxy foundProxy = null;
		try {

			URI uri = new URI(proxyType, "//" + host, null);
			List<Proxy> proxies = ProxySelector.getDefault().select(uri);

			if (proxies != null) {
				for (Proxy proxy : proxies) {
					if (proxy != Proxy.NO_PROXY) {
						foundProxy = proxy;
						break;
					}
				}
			}

		}
		catch (URISyntaxException e) {
			// No proxy
		}
		return foundProxy;
	}

	public static void waitIntervals(long timePerTick) {
		try {
			Thread.sleep(timePerTick);
		}
		catch (InterruptedException e) {

		}
	}

}
