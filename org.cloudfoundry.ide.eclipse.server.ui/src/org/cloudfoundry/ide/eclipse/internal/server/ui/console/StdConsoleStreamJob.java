/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

class StdConsoleStreamJob extends Job implements IConsoleJob {

	private String toStream;

	private LocalConsoleStream content;

	public StdConsoleStreamJob(LocalConsoleStream content) {
		super(content.getContentType().getId());
		this.content = content;
	}

	protected IStatus run(IProgressMonitor monitor) {

		if (toStream != null) {
			try {
				content.write(toStream, monitor);
				toStream = null;
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(
						"Failed to write message to Cloud Foundry console due to - " + e.getMessage(), e);
			}
		}

		return Status.OK_STATUS;
	}

	public void write(String message, IProgressMonitor monitor) {
		this.toStream = message;
		if (this.toStream != null) {
			schedule();
		}
	}

	public void close() {
		content.close();
	}

	public IContentType getContentType() {
		return content.getContentType();
	}

}