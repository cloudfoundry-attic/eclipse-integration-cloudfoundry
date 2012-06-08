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

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.util.ProjectModule;

/**
 * The module delegate maps WTP module resources to the specified project's
 * resources. For standalone apps, all project files and folders are mapped to
 * corresponding module resources. If additional filtering is required, it can
 * be done by overriding module resource member getters from the parent class.
 * <p/>
 * The delegate gets invoked indirectly by WTP through an IModule created by the
 * Module factory, when it is adapted to a Module delegate.
 * <p/>
 * This may occur when a request is made for IModuleResource for all the
 * resources in a project , and this request may typically happen only when
 * publishing or updating the resources of a project
 * <p/>
 * Note that when a IModule is created by the module factory, a reference to
 * that factory is passed into the IModule. The factory then is responsible for
 * creating a module delegate, when a module delegate is requested via an
 * adapter call to the IModule.
 * </p> See implementations of IModule
 * 
 * @see StandAloneModuleFactory
 * @see IModule
 */
public class StandaloneModuleDelegate extends ProjectModule {

	public StandaloneModuleDelegate(IProject project) {
		super(project);
	}

}
