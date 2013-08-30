/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;


public class CloudApplicationURL {

	private String host;

	private String domain;

	private String url;

	public CloudApplicationURL(String host, String domain) {
		this.host = host;
		this.domain = domain;
		url = host + '.' + domain;
	}

	public String getHost() {
		return host;
	}

	public String getDomain() {
		return domain;
	}

	public String getUrl() {
		return url;
	}


}
