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
package org.cloudfoundry.ide.eclipse.internal.server.core.standalone;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.AbstractModuleResourceArchive;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * Generates a deployable archive for standalone applications where the
 * structure of the deployed resources is derived from the target or output
 * directories of the project.
 * 
 */
public class StandaloneApplicationArchive extends AbstractModuleResourceArchive {

	public StandaloneApplicationArchive(IModule module, List<IModuleResource> resources) {
		super(module, resources);
	}

	@Override
	protected ModuleFolderEntryAdapter getModuleFolderAdapter(IModuleFolder folder) {
		return new StandaloneModuleFolderEntryAdapter(folder);
	}

	@Override
	protected ModuleFileEntryAdapter getFileResourceEntryAdapter(IModuleFile moduleFile) {
		return new StandaloneModuleFileEntryAdapter(moduleFile);
	}

	public class StandaloneModuleFileEntryAdapter extends ModuleFileEntryAdapter {

		public StandaloneModuleFileEntryAdapter(IModuleFile moduleFile) {
			super(moduleFile);
		}

		@Override
		protected String computeName(IModuleResource resource) {
			String name = resource.getModuleRelativePath().append(resource.getName()).toString();
			return name;
		}

	}

	public class StandaloneModuleFolderEntryAdapter extends ModuleFolderEntryAdapter {
		public StandaloneModuleFolderEntryAdapter(IModuleFolder folder) {
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