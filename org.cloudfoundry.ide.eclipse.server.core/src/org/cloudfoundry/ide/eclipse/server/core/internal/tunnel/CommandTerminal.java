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
package org.cloudfoundry.ide.eclipse.server.core.internal.tunnel;

import org.cloudfoundry.ide.eclipse.server.core.internal.PlatformUtil;
import org.eclipse.core.runtime.Platform;

/**
 * 
 * Using getters and setters with no-argument constructors for JSON
 * serialisation
 * 
 */
public class CommandTerminal {

	private String terminal;

	public CommandTerminal() {
	}

	public String getTerminal() {
		return terminal;
	}

	public void setTerminal(String terminalLaunchCommand) {
		this.terminal = terminalLaunchCommand;
	}

	public static CommandTerminal getDefaultOSTerminal() {
		String os = PlatformUtil.getOS();
		String terminalCommand = null;
		if (Platform.OS_MACOSX.equals(os)) {
			terminalCommand = "/usr/bin/open -a Terminal"; //$NON-NLS-1$
		}
		else if (Platform.OS_LINUX.equals(os)) {
			terminalCommand = "xterm -e"; //$NON-NLS-1$
		}
		else if (Platform.OS_WIN32.equals(os)) {
			terminalCommand = "cmd.exe /c start cmd.exe /k"; //$NON-NLS-1$
		}

		if (terminalCommand != null) {
			CommandTerminal terminal = new CommandTerminal();
			terminal.setTerminal(terminalCommand);
			return terminal;
		}

		return null;
	}

}
