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
package org.cloudfoundry.ide.eclipse.server.ui.internal.console;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.log.LogContentType;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.wst.server.core.IServer;

/**
 * 
 * NOTE: Must stay internal as API may change. Should NOT be extended or used by
 * adopters of CF Eclipse until made public.
 *
 */
public abstract class CloudConsoleManager {

	/**
	 * @param server
	 * @param app
	 * @param instanceIndex
	 * @param show Start console if show is true, otherwise reset and start only
	 * if console was previously created already
	 * @param monitor NOTE: may be removed in the future, when consoles are
	 * purely client callbacks that do not require progress monitors *
	 * 
	 */
	public abstract void startConsole(CloudFoundryServer server, LogContentType type,
			CloudFoundryApplicationModule appModule, int instanceIndex, boolean show, boolean clear,
			IProgressMonitor monitor);

	/**
	 * Find the message console that corresponds to the server and a given
	 * module. If there are multiple instances of the application, only the
	 * first one will get returned.
	 * @param server the server for that console
	 * @param appModule the app for that console
	 * @return the message console. Null if no corresponding console is found.
	 */
	public abstract MessageConsole findCloudFoundryConsole(IServer server, CloudFoundryApplicationModule appModule);

	public abstract void writeToStandardConsole(String message, CloudFoundryServer server,
			CloudFoundryApplicationModule appModule, int instanceIndex, boolean clear, boolean isError);

	/**
	 * Displays existing log content for the given running application instance.
	 * @param server cloud server
	 * @param appModule running application
	 * @param instanceIndex app index
	 * @param clear true if current app instance console should be cleared.
	 * False otherwise to continue tailing from existing content.
	 * @param monitor NOTE: may be removed in the future, when consoles are
	 * purely client callbacks that do not require progress monitors
	 */
	public abstract void showCloudFoundryLogs(CloudFoundryServer server, CloudFoundryApplicationModule appModule,
			int instanceIndex, boolean clear, IProgressMonitor monitor);

	public abstract void stopConsole(IServer server, CloudFoundryApplicationModule appModule, int instanceIndex);

	public abstract void stopConsoles();

}