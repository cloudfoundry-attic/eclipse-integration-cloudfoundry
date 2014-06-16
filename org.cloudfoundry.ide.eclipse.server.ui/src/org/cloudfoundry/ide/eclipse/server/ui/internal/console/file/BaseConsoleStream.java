/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.console.file;

import java.io.IOException;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Basic console stream that manages an output stream to an Eclipse console,
 * including initialising the stream, as well as closing streams.
 */
public abstract class BaseConsoleStream {

	private IOConsoleOutputStream outputStream;

	public synchronized void initialiseStream(IOConsoleOutputStream outputStream) {
		this.outputStream = outputStream;
		if (this.outputStream != null && !this.outputStream.isClosed()) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					doInitialiseStream(BaseConsoleStream.this.outputStream);
				}
			});
		}
	}

	public synchronized void close() {
		if (outputStream != null && !outputStream.isClosed()) {
			try {
				outputStream.close();
			}
			catch (IOException e) {
				CloudFoundryPlugin.logError("Failed to close console output stream due to: " + e.getMessage(), e);
			}
		}
	}

	public synchronized boolean isActive() {
		return outputStream != null && !outputStream.isClosed();
	}

	/**
	 * Returns the output stream IFF it is active. Conditions for determining if
	 * a stream is active is done through {@link #isActive()}
	 * @return Returns the output stream IFF it is active. Returns null
	 * otherwise.
	 */
	protected synchronized IOConsoleOutputStream getActiveOutputStream() {
		if (isActive()) {
			return outputStream;
		}
		return null;
	}

	protected void doInitialiseStream(IOConsoleOutputStream outputStream) {
		// Subclasses can override if necessary.
	};
}
