/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeployedResourceCache.CachedDeployedApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeployedResourceCache.DeployedResourceEntry;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.AbstractModuleResourceArchive;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * Application archive for incremental publishing that optimises sha1 code and
 * file size calculation by using cached sha1 codes for files that have not changed.
 * Resources that have not been changed will be skipped from this calculation as
 * an optimisation step.
 * <p/>
 * This speeds up the process of publishing incremental changes in a
 * application, as sha1 hash codes need not be computed for files that haven't
 * changed.
 * <p/>
 * 
 * Two separate operations occur with this archive.
 * 
 * <p/>
 * 1) entries for all module resources are collected, regardless if the resource
 * has changed or not. The reason for this is that the CF client requires all
 * entries in order to determine what has changed on the server side. During
 * this phase, either cached entries are used for resources that have not
 * changed, or entries are recalculated for resources that have changed.
 * 
 * <p/>
 * 2) The second phase involves handling the list of resources that the server
 * indicates have not changed. This is done through a callback handler, which
 * then builds the partial war file with only those resources that have changed.
 * 
 */
public class CachingApplicationArchive extends AbstractModuleResourceArchive {

	private final Set<String> changedResources;

	private String fileName;

	private final CachedDeployedApplication appID;

	public CachingApplicationArchive(List<IModuleResource> allResources, List<IModuleResource> changedResources,
			IModule module, String appName) {
		super(module, allResources);
		this.appID = new CachedDeployedApplication(appName);
		this.changedResources = changedResourcesAsZipNames(changedResources);
	}

	protected Set<String> changedResourcesAsZipNames(List<IModuleResource> changedResources) {
		Set<String> names = new HashSet<String>();
		for (IModuleResource resource : changedResources) {
			names.add(CloudUtil.getZipRelativeName(resource));
		}
		return names;
	}

	public String getFilename() {
		return fileName;
	}

	protected ModuleFolderEntryAdapter getModuleFolderAdapter(IModuleFolder folder) {
		return new ZipModuleFolderEntryAdapter(folder);
	}

	protected ModuleFileEntryAdapter getFileResourceEntryAdapter(IModuleFile file) {
		String zipName = CloudUtil.getZipRelativeName(file);
		boolean changed = changedResources != null && changedResources.contains(zipName);
		return new ZipModuleFileEntryAdapter(file, appID, changed);
	}

	public void generatePartialWarFile(Set<String> knownResourceNames) {
		Iterable<Entry> localEntries = getEntries();
		Map<String, AbstractModuleResourceEntryAdapter> missingChangedEntries = new HashMap<String, AbstractModuleResourceEntryAdapter>();
		Set<IModuleResource> missingChangedResources = new HashSet<IModuleResource>();

		for (Entry entry : localEntries) {

			if (entry.isDirectory() || !knownResourceNames.contains(entry.getName())) {
				missingChangedEntries.put(entry.getName(), (AbstractModuleResourceEntryAdapter) entry);
				missingChangedResources.add(((AbstractModuleResourceEntryAdapter) entry).getResource());
			}
		}

		// Build war file with changed/missing resources
		try {

			File partialWar = CloudUtil.createWarFile(getModuleResources(), getModule(), missingChangedResources, null);

			if (partialWar.exists()) {
				fileName = partialWar.getName();
				ZipFile zipPartialWar = new ZipFile(partialWar);
				Enumeration<? extends ZipEntry> zipEntries = zipPartialWar.entries();
				List<Entry> toDeploy = new ArrayList<ApplicationArchive.Entry>();
				while (zipEntries.hasMoreElements()) {
					ZipEntry zipEntry = zipEntries.nextElement();

					AbstractModuleResourceEntryAdapter archiveEntry = missingChangedEntries.get(zipEntry.getName());
					if (archiveEntry != null) {
						DeployedResourceEntry deployedResourcesEntry = archiveEntry instanceof ZipModuleFileEntryAdapter ? ((ZipModuleFileEntryAdapter) archiveEntry)
								.getDeployedResourcesEntry() : null;
						toDeploy.add(new PartialZipEntryAdapter(deployedResourcesEntry, zipEntry, zipPartialWar));

					}

				}
				entries = toDeploy;
			}
		}
		catch (CoreException e) {
			// Failed to create partial war
			CloudFoundryPlugin.log(e);
		}
		catch (ZipException e) {
			// Failed to create partial war
			CloudFoundryPlugin.logError(e);
		}
		catch (IOException e) {
			// Failed to create partial war
			CloudFoundryPlugin.logError(e);
		}
	}

	/**
	 * Entry to be used to access actual payload files. Sha1 entries should be
	 * computed prior to creating the entry, ideally without uncompressing the
	 * archive file, and possibly using a cache
	 * 
	 */
	public class PartialZipEntryAdapter implements ApplicationArchive.Entry {
		private final DeployedResourceEntry deployedResourcesEntry;

		private final ZipEntry zipEntry;

		private final ZipFile zipFile;

		public PartialZipEntryAdapter(DeployedResourceEntry deployedResourcesEntry, ZipEntry zipEntry, ZipFile zipFile) {
			this.zipFile = zipFile;
			this.zipEntry = zipEntry;
			this.deployedResourcesEntry = deployedResourcesEntry;
		}

		public boolean isDirectory() {
			return zipEntry.isDirectory();
		}

		public String getName() {
			return zipEntry.getName();
		}

		/**
		 * 
		 * @return deployed resource, or null if not defined for this zip entry.
		 * Note that for directory cases, there won't be a deployed resource so
		 * this would be one case where null is returned
		 */
		protected DeployedResourceEntry getDeployedResourceEntry() {
			return deployedResourcesEntry;
		}

		public long getSize() {
			return getDeployedResourceEntry() != null ? getDeployedResourceEntry().getFileSize()
					: AbstractModuleResourceEntryAdapter.UNDEFINED_SIZE;
		}

		public byte[] getSha1Digest() {
			// Do not obtain it from the zip Entry to avoid unzipping the
			// archive.
			return getDeployedResourceEntry() != null ? getDeployedResourceEntry().getSha1() : null;
		}

		public InputStream getInputStream() throws IOException {
			if (isDirectory()) {
				return null;
			}

			return zipFile.getInputStream(zipEntry);
		}

	}

	public class ZipModuleFolderEntryAdapter extends ModuleFolderEntryAdapter {

		public ZipModuleFolderEntryAdapter(IModuleFolder moduleResource) {
			super(moduleResource);
		}

		protected String computeName(IModuleResource resource) {
			return CloudUtil.getZipRelativeName(resource);
		}

	}

	/**
	 * Module file resource specialisation of the Cloud Foundry client entry
	 * adapter. This computes sha1 and file sizes and manages caching of such
	 * values for resources that have changed.
	 * 
	 */
	public class ZipModuleFileEntryAdapter extends ModuleFileEntryAdapter {

		private final CachedDeployedApplication appName;

		private final boolean recalculate;

		public ZipModuleFileEntryAdapter(IModuleFile moduleFile, CachedDeployedApplication appName, boolean recalculate) {
			super(moduleFile);

			this.appName = appName;
			this.recalculate = recalculate;
		}

		@Override
		public long getSize() {
			DeployedResourceEntry entry = getDeployedResourcesEntry();
			return entry != null ? entry.getFileSize() : UNDEFINED_SIZE;
		}

		public DeployedResourceEntry getDeployedResourcesEntry() {

			DeployedResourceEntry deployedResourcesEntry = CloudFoundryPlugin.getDefault().getDeployedResourcesCache()
					.getEntry(appName, getName());

			if (canComputeResourceEntry() && (recalculate || deployedResourcesEntry == null)) {
				byte[] sha1 = super.getSha1Digest();
				long fileSize = super.getSize();
				deployedResourcesEntry = new DeployedResourceEntry(sha1, fileSize, getName());
				CloudFoundryPlugin.getDefault().getDeployedResourcesCache().add(appName, deployedResourcesEntry);
			}

			return deployedResourcesEntry;
		}

		public byte[] getSha1Digest() {
			DeployedResourceEntry entry = getDeployedResourcesEntry();
			return entry != null ? entry.getSha1() : null;
		}

		protected String computeName(IModuleResource resource) {
			return CloudUtil.getZipRelativeName(resource);
		}
	}

}
