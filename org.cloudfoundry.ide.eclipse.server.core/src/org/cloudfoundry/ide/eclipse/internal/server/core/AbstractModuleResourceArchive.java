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
package org.cloudfoundry.ide.eclipse.internal.server.core;

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
 * Base Cloud Foundry client archive that supports IModuleResource and computes
 * sha1 and input stream entries for module resources. Creates an archive entry
 * for every module resource specified in the this archive. Specialised classes
 * must define two entry types to create when a request is made for a module
 * resource: folder and file entries. 
 * 
 * These entries allow specialised classes to
 * determine how the sha1 and file sizes are computed for each corresponding
 * module resource, and whether they are obtained from cache or not.
 * 
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
			return null;
		}

		public InputStream getInputStream() throws IOException {
			return null;
		}

	}

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
