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
package org.cloudfoundry.ide.eclipse.internal.uaa;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * @author Steffen Pingel 
 * @author Christian Dupuis
 */
public class RequestFactory extends SimpleClientHttpRequestFactory {

	/**
	 * For testing.
	 */
	public static boolean proxyEnabled = true;

	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		List<Proxy> proxies = ProxySelector.getDefault().select(uri);
		if (proxyEnabled && proxies != null && proxies.size() > 0) {
			setProxy(proxies.get(0));			
		}else {
			setProxy(null);
		}
		return super.createRequest(uri, httpMethod);
	}

}