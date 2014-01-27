/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

public class CloudApplicationURL {

	private String subDomain;

	private String domain;

	private String url;

	public CloudApplicationURL(String subDomain, String domain) {
		this.subDomain = subDomain;
		this.domain = domain;
		url = subDomain + '.' + domain;
	}

	/**
	 * Subdomain is generally the first segments of a URL appended to a known domain:
	 * e.g. "subdomain.my.domain" 
	 * @return the first segments not part of a known domain. It may be empty.
	 */
	public String getSubdomain() {
		return subDomain;
	}

	/**
	 * Trailing segments of a URL. 
	 * @return trailing segments of a URL.
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * 
	 * @return full URL with both subdomain and domain appended together.
	 */
	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return getSubdomain() + " - " + getDomain();
	}

	/*
	 * GENERATED
	 */

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((subDomain == null) ? 0 : subDomain.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CloudApplicationURL other = (CloudApplicationURL) obj;
		if (domain == null) {
			if (other.domain != null)
				return false;
		}
		else if (!domain.equals(other.domain))
			return false;
		if (subDomain == null) {
			if (other.subDomain != null)
				return false;
		}
		else if (!subDomain.equals(other.subDomain))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		}
		else if (!url.equals(other.url))
			return false;
		return true;
	}

}
