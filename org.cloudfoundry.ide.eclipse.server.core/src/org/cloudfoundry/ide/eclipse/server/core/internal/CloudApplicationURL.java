/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
 * IMPORTANT NOTE: This class can be referred by the branding extension from adopter so this class 
 * should not be moved or renamed to avoid breakage to adopters. 
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.io.StringWriter;

/**
 * Application URL that can only be defined by subdomain and domain segments.
 * Unparsed Application URLs are not meant to be supported by this
 * representation.
 *
 */
public class CloudApplicationURL {

	private String subdomain;

	private String domain;

	private String url;

	public CloudApplicationURL(String subdomain, String domain) {
		this.subdomain = subdomain;
		this.domain = domain;
		url = getSuggestedApplicationURL(subdomain, domain);
	}

	protected String getSuggestedApplicationURL(String subdomain, String domain) {
		if (subdomain == null && domain == null) {
			return null;
		}
		StringWriter writer = new StringWriter();
		if (subdomain != null) {
			writer.append(subdomain);
			if (domain != null) {
				writer.append('.');
			}
		}
		if (domain != null) {
			writer.append(domain);
		}

		return writer.toString();
	}

	/**
	 * Subdomain is generally the first segments of a URL appended to a known
	 * domain: e.g. "subdomain.my.domain"
	 * @return the first segments not part of a known domain. It may be empty.
	 */
	public String getSubdomain() {
		return subdomain;
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

	/*
	 * GENERATED
	 */

	@Override
	public String toString() {
		return "CloudApplicationURL [subdomain=" + subdomain + ", domain=" + domain + ", url=" + url + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((subdomain == null) ? 0 : subdomain.hashCode());
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
		if (subdomain == null) {
			if (other.subdomain != null)
				return false;
		}
		else if (!subdomain.equals(other.subdomain))
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
