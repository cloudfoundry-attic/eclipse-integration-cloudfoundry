/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.console;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Console content for local standard output and error. It is meant write
 * content to the console via the {@link #write(String, IProgressMonitor)} API,
 * instead of fetching content remotely to be displayed to the console. A use
 * case for this is an CF Eclipse component that wishes to display content to
 * the console.
 * 
 */
public abstract class LocalConsoleStream extends CloudFoundryConsoleStream {

	public LocalConsoleStream(int swtColour) {
		super(null, swtColour, null, -1);
	}

	protected String getContent(IProgressMonitor monitor) throws CoreException {
		return null;
	}

}
