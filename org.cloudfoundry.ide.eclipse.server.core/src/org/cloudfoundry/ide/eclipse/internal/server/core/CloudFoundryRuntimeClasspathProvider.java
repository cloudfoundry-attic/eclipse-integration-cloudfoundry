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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.server.core.IRuntime;
import org.osgi.framework.Bundle;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class CloudFoundryRuntimeClasspathProvider extends RuntimeClasspathProviderDelegate {

	String[] CP_BUNDLES = { "javax.servlet", "javax.servlet.jsp" };

	public IClasspathEntry[] resolveClasspathContainer(IProject project, IRuntime runtime) {
		List<IClasspathEntry> cp = new ArrayList<IClasspathEntry>(2);
		for (String id : CP_BUNDLES) {
			Bundle bundle = Platform.getBundle(id);
			try {
				File file = FileLocator.getBundleFile(bundle);
				Path path = new Path(file.getCanonicalPath());
				cp.add(JavaCore.newLibraryEntry(path, null, null));
			}
			catch (IOException e) {
				// ignore
			}
		}
		return cp.toArray(new IClasspathEntry[0]);
	}

}
