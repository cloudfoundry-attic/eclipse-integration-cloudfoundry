/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Defines an Eclipse CF console content. Generally, there is one console
 * content instance per file resource being streamed to the CF console, and the
 * responsibility of the console content is to create a stream that fetches
 * content from a CF server for a particular file and sends it as output to the
 * Eclipse CF console output stream.
 */
public interface ICloudFoundryConsoleStream {

	/**
	 * Fetch and write content to the console.
	 * @param monitor
	 * @return content that is written to the console output stream on each call
	 * to the method
	 * @throws CoreException if error occurred writing to output stream. This
	 * may not necessarily close the stream, as some cases may require
	 * re-attempts even with errors. To have the content manager close the
	 * stream, see {@link #isActive()}
	 */
	public String write(IProgressMonitor monitor) throws CoreException;

	/**
	 * Write given content to the console.
	 * @param content
	 * @return
	 * @throws CoreException
	 */
	public String write(String content) throws CoreException;

	/**
	 * Link the console content to an actual Eclipse console output stream.
	 * @param outputStream to the Eclipse console.
	 */
	public void initialiseStream(IOConsoleOutputStream outputStream);

	/**
	 * Permanently close the content stream. A closed content is always
	 * inactive.
	 */
	public void close();

	/**
	 * 
	 * @return true if stream is open and can still stream content. False
	 * otherwise. Console content managers will use this API to determine if
	 * further streaming requests should be made on the content.
	 */
	public boolean isActive();

	/**
	 * 
	 * @return non-null content type. Identifies the console content and may
	 * enable additional management on the content.
	 */
	public IContentType getContentType();
}
