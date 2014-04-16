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
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * The primary purpose of the module application archive is to calculate sha1
 * has codes for each application file so that the Cloud Foundry server can
 * determine what resources have changed, and to generate an input stream for
 * all files that need to be published. Since the application resources are
 * typically represented as IModuleResource, the role of the application archive
 * is to transform these IModuleResource into application archive entries that
 * calculate sha1 and open an input stream when the Cloud Foundry client is
 * ready to publish the application.
 * 
 * <p/>
 * Although the Cloud Foundry plugin framework does generate .war file for a
 * Java Web type application's IModuleResources, and uses a default archive that
 * works on .war files, some other application types like Java standalone
 * generate payloads for Cloud Foundry servers differently. In this case, this
 * Module resource application archive provides an alternative way of
 * serialising application files without having to go through the .war file
 * step.
 * 
 * <p/>
 * The default implementation provides a general way to generated an application
 * archive from IModuleResources for a given IModule. Subclasses can override
 * this and provide their own archive which converts IModuleResource into
 * archive entries.
 */
public class ModuleResourceApplicationArchive extends AbstractModuleResourceArchive {

	/**
	 * 
	 * @param module corresponding the the application that needs to be
	 * published.
	 * @param resources to publish to the CF server.
	 * @throws CoreException if resources is null or empty. There must be at
	 * least one resource to archive and publish to the CF server.
	 */
	public ModuleResourceApplicationArchive(IModule module, List<IModuleResource> resources) throws CoreException {
		super(module, resources);
		if (resources == null || resources.isEmpty()) {
			throw new CoreException(
					CloudFoundryPlugin.getErrorStatus("Unable to deploy module. No deployable resources found for: "
							+ module.getName() + " " + module.getId()));
		}
	}

	@Override
	protected ModuleFolderEntryAdapter getModuleFolderAdapter(IModuleFolder folder) {
		return new ModuleResourceFolderEntryAdapter(folder);
	}

	@Override
	protected ModuleFileEntryAdapter getFileResourceEntryAdapter(IModuleFile moduleFile) {
		return new ModuleResourceFileEntryAdapter(moduleFile);
	}

	public class ModuleResourceFileEntryAdapter extends ModuleFileEntryAdapter {

		public ModuleResourceFileEntryAdapter(IModuleFile moduleFile) {
			super(moduleFile);
		}

		@Override
		protected String computeName(IModuleResource resource) {
			String name = resource.getModuleRelativePath().append(resource.getName()).toString();
			return name;
		}

	}

	public class ModuleResourceFolderEntryAdapter extends ModuleFolderEntryAdapter {
		public ModuleResourceFolderEntryAdapter(IModuleFolder folder) {
			super(folder);
		}

		@Override
		protected String computeName(IModuleResource resource) {
			String name = resource.getModuleRelativePath().append(resource.getName()).toString() + "/";
			return name;
		}

	}

	public String getFilename() {
		return getModule().getName();
	}
}