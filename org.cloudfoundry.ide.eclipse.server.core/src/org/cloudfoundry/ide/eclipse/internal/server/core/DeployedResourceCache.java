/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache for sha1 hash entries and file sizes for incremental publishing of
 * deployed resources. This avoid recalculating hash entries for resources that
 * have not changed in the server. A server poll may still required to obtain a
 * list of unchanged resources.
 * 
 */
public class DeployedResourceCache {
	private final Map<CachedDeployedApplication, Map<String, DeployedResourceEntry>> cacheMap = new HashMap<CachedDeployedApplication, Map<String, DeployedResourceEntry>>();

	public synchronized void add(CachedDeployedApplication applicationID, DeployedResourceEntry entry) {
		Map<String, DeployedResourceEntry> appEntries = cacheMap.get(applicationID);
		if (appEntries == null) {
			appEntries = new HashMap<String, DeployedResourceCache.DeployedResourceEntry>();
			cacheMap.put(applicationID, appEntries);
		}
		appEntries.put(entry.getZipRelativeFileName(), entry);
	}

	public synchronized DeployedResourceEntry getEntry(CachedDeployedApplication applicationID,
			String zipRelativeFileName) {
		Map<String, DeployedResourceEntry> appEntries = cacheMap.get(applicationID);

		return appEntries != null ? appEntries.get(zipRelativeFileName) : null;
	}

	public static class DeployedResourceEntry {
		private final byte[] sha1;

		private final long fileSize;

		private final String zipRelativeFileName;

		public DeployedResourceEntry(byte[] sha1, long fileSize, String zipRelativeFileName) {
			this.sha1 = sha1;
			this.fileSize = fileSize;
			this.zipRelativeFileName = zipRelativeFileName;
		}

		public String getZipRelativeFileName() {
			return zipRelativeFileName;
		}

		public byte[] getSha1() {
			return sha1;
		}

		public long getFileSize() {
			return fileSize;
		}
	}

	/**
	 * Light-weight representation of an app only for purposes of caching
	 * deployed resources for that app.
	 * 
	 */
	public static class CachedDeployedApplication {

		private final String appName;

		public CachedDeployedApplication(String appName) {
			this.appName = appName;
		}

		public String getAppName() {
			return appName;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((appName == null) ? 0 : appName.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			CachedDeployedApplication other = (CachedDeployedApplication) obj;
			if (appName == null) {
				if (other.appName != null) {
					return false;
				}
			}
			else if (!appName.equals(other.appName)) {
				return false;
			}
			return true;
		}

		public String toString() {
			return appName.toString();
		}

	}

}
