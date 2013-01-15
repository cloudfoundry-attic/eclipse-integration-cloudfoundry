/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import org.cloudfoundry.ide.eclipse.internal.server.core.PlatformUtil;
import org.eclipse.core.runtime.Platform;

/**
 * 
 * Using getters and setters with no-argument constructors for JSON serialisation
 * 
 */
public class CommandTerminal {

	private String terminalLaunchCommand;

	public CommandTerminal() {
	}

	public String getTerminalLaunchCommand() {
		return terminalLaunchCommand;
	}

	public void setTerminalLaunchCommand(String terminalLaunchCommand) {
		this.terminalLaunchCommand = terminalLaunchCommand;
	}

	public static CommandTerminal getDefaultOSTerminal() {
		String os = PlatformUtil.getOS();
		String terminalCommand = null;
		if (Platform.OS_MACOSX.equals(os)) {
			terminalCommand = "/usr/bin/open -a Terminal";
		}
		else if (Platform.OS_LINUX.equals(os)) {

		}
		else if (Platform.OS_WIN32.equals(os)) {
			terminalCommand = "cmd.exe /c";
		}

		if (terminalCommand != null) {
			CommandTerminal terminal = new CommandTerminal();
			terminal.setTerminalLaunchCommand(terminalCommand);
			return terminal;
		}

		return null;
	}

}
