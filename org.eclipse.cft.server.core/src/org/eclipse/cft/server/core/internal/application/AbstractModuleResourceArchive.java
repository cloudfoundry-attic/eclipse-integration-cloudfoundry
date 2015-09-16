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
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.archive.AbstractApplicationArchiveEntry;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * An application archive generates input streams for an application's files
 * when the Cloud Foundry framework is ready to the publish an application to a
 * Cloud Foundry server. Files are represented by IModuleResource, and the
 * archive generates an input stream from the IModuleResource.
 * <p/>
 * In addition, the application archive is also used to calculate sha1 has codes
 * for each application file so that the Cloud Foundry server can determine what
 * resources have changed prior to publishing any changes to the application,
 * and to generate an input stream for all files that need to be published.
 * <p/>
 * For Java Web type applications (Spring, Grails, Java web), it is not
 * necessary to provide a explicit application archive, as the Cloud Foundry
 * plugin framework generates .war files for such applications and uses a
 * built-in default application archive that works on this .war files.
 * <p/>
 * However for some other application types like Java standalone, .war files are
 * not generated, and therefore serialisation of the application files is
 * performed through an application archive that works on the application
 * IModuleResource directly
 * <p/>
 * 
 * This application archive works directly on IModuleResource and computes sha1
 * and input stream entries for an application from its IModuleResource.
 */
public abstract class AbstractModuleResourceArchive implements ApplicationArchive {

	protected List<Entry> entries;

	private final IModule module;

	private final List<IModuleResource> resources;

	protected AbstractModuleResourceArchive(IModule module, List<IModuleResource> resources) {
		this.module = module;
		this.resources = resources;
	}

	public Iterable<Entry> getEntries() {
		if (entries == null) {
			entries = new ArrayList<ApplicationArchive.Entry>();
			collectEntriesPriorToDeployment(entries, resources.toArray(new IModuleResource[0]));
		}
		return entries;
	}

	protected List<IModuleResource> getModuleResources() {
		return resources;
	}

	/**
	 * All entries must be collected, for both resources that have changed as
	 * well as those that haven't, as the CF client must first use that
	 * collected list of entries to determine what has changed.
	 * @param entries
	 * @param resources
	 */
	protected void collectEntriesPriorToDeployment(List<Entry> entries, IModuleResource[] members) {
		if (members == null) {
			return;
		}

		for (IModuleResource resource : members) {

			if (resource instanceof IModuleFile) {
				ModuleFileEntryAdapter fileAdapter = getFileResourceEntryAdapter((IModuleFile) resource);
				if (fileAdapter != null) {
					entries.add(fileAdapter);
				}
			}
			else if (resource instanceof IModuleFolder) {
				IModuleFolder folder = (IModuleFolder) resource;
				ModuleFolderEntryAdapter folderAdapter = getModuleFolderAdapter(folder);
				if (folderAdapter != null) {
					entries.add(folderAdapter);
					collectEntriesPriorToDeployment(entries, folder.members());
				}
			}
		}
	}

	abstract protected ModuleFolderEntryAdapter getModuleFolderAdapter(IModuleFolder folder);

	abstract protected ModuleFileEntryAdapter getFileResourceEntryAdapter(IModuleFile moduleFile);

	protected IModule getModule() {
		return module;
	}

	/**
	 * Base folder entry adapter for module resouce folders.
	 * 
	 */
	public abstract class ModuleFolderEntryAdapter extends AbstractModuleResourceEntryAdapter {

		public ModuleFolderEntryAdapter(IModuleFolder moduleResource) {
			super(moduleResource);
		}

		public boolean isDirectory() {
			return true;
		}

		@Override
		public long getSize() {
			return UNDEFINED_SIZE;
		}

		public byte[] getSha1Digest() {
			// No sha1 hash code as it's a folder.
			return null;
		}

		public InputStream getInputStream() throws IOException {
			// No input stream needed for folders.
			return null;
		}

	}

	/**
	 * Base file entry adapter that resolves an input stream to a File for each
	 * module resource file.
	 * 
	 */
	public abstract class ModuleFileEntryAdapter extends AbstractModuleResourceEntryAdapter {

		protected final File file;

		public ModuleFileEntryAdapter(IModuleFile moduleResource) {
			super(moduleResource);
			file = getFile(moduleResource);
		}

		public boolean isDirectory() {
			return false;
		}

		protected File getFile(IModuleResource moduleResource) {
			File file = (File) moduleResource.getAdapter(File.class);
			if (file == null) {
				IFile iFile = (IFile) moduleResource.getAdapter(IFile.class);

				if (iFile != null) {
					IPath location = iFile.getLocation();
					if (location != null) {
						return new File(location.toString());
					}
				}
			}
			return file;
		}

		protected boolean canComputeResourceEntry() {
			return file != null && file.exists();
		}

		public InputStream getInputStream() throws IOException {

			if (canComputeResourceEntry()) {
				return new FileInputStream(file);
			}

			return null;
		}

	}

	/**
	 * Parent class that integrated webtool Module-based resources into the
	 * Cloud Foundry Java client. Whereas the Java client operates on
	 * directories or zip archives, this specialisation works on webtools module
	 * resources. Further specialisations for files and folders are responsible
	 * for calculate sha1 and file sizes, and allows specialisations to rely on
	 * caching for those values.
	 * 
	 */
	public abstract class AbstractModuleResourceEntryAdapter extends AbstractApplicationArchiveEntry {
		private final IModuleResource moduleResource;

		protected String name;

		public static final long UNDEFINED_SIZE = AbstractApplicationArchiveEntry.UNDEFINED_SIZE;

		public AbstractModuleResourceEntryAdapter(IModuleResource moduleResource) {
			this.moduleResource = moduleResource;
			name = computeName(moduleResource);
		}

		public IModuleResource getResource() {
			return moduleResource;
		}

		public String getName() {
			return name;
		}

		abstract protected String computeName(IModuleResource resource);

	}

}
