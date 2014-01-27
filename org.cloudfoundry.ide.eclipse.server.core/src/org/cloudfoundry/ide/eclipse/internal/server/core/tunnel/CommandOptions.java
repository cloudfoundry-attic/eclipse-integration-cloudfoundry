/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

import java.io.StringWriter;

/**
 * 
 * Using getters and setters with no-argument constructors for JSON
 * serialisation
 * 
 */
public class CommandOptions {

	private String options;

	public CommandOptions() {

	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public static String getDefaultTunnelOptionsDescription() {
		StringWriter writer = new StringWriter();
		writer.append("Use the following variables for tunnel values that will be filled in automatically:");
		writer.append('\n');
		writer.append('\n');
		writer.append("${");
		writer.append(TunnelOptions.user.name());
		writer.append("}");
		writer.append('\n');
		writer.append("${");
		writer.append(TunnelOptions.password.name());
		writer.append("}");
		writer.append('\n');
		writer.append("${");
		writer.append(TunnelOptions.url.name());
		writer.append("}");
		writer.append('\n');
		writer.append("${");
		writer.append(TunnelOptions.databasename.name());
		writer.append("}");
		writer.append('\n');
		writer.append("${");
		writer.append(TunnelOptions.port.name());
		writer.append("}");
		return writer.toString();
	}

}
