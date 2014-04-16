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

import org.eclipse.swt.SWT;

/**
 * 
 * Local std error content for the Eclipse console. Intention is to write
 * local content to the console using the
 * {@link #write(String, org.eclipse.core.runtime.IProgressMonitor)}
 * <p/>
 * To fetch std error content from a remote server (e.g. a std  log file), use
 * {@link FileConsoleStream} instead.
 */
public class LocalStdErrorConsoleStream extends LocalConsoleStream {

	public LocalStdErrorConsoleStream() {
		super(SWT.COLOR_RED);
	}

	public IContentType getContentType() {
		return StdContentType.STD_ERROR;
	}

}
