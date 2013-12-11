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
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.util.ProjectModule;

/**
 * Use a regular project module delegate for Java applications as the Java
 * applications are archived based on Java launch configurations and other
 * application repackaging only on deployment.
 * 
 */
public class JavaLauncherModuleDelegate extends ProjectModule {

	public JavaLauncherModuleDelegate(IProject project) {
		super(project);
	}

}
