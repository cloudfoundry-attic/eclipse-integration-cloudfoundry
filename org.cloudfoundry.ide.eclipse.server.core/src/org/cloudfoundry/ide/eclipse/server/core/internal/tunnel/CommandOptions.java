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

import java.io.StringWriter;

import org.cloudfoundry.ide.eclipse.server.core.internal.Messages;

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
		writer.append(Messages.CommandOptions_DESCRIPTION_VARIABLES_FOR_TUNNEL);
		writer.append('\n');
		writer.append('\n');
		writer.append("${"); //$NON-NLS-1$
		writer.append(TunnelOptions.user.name());
		writer.append("}"); //$NON-NLS-1$
		writer.append('\n');
		writer.append("${"); //$NON-NLS-1$
		writer.append(TunnelOptions.password.name());
		writer.append("}"); //$NON-NLS-1$
		writer.append('\n');
		writer.append("${"); //$NON-NLS-1$
		writer.append(TunnelOptions.url.name());
		writer.append("}"); //$NON-NLS-1$
		writer.append('\n');
		writer.append("${"); //$NON-NLS-1$
		writer.append(TunnelOptions.databasename.name());
		writer.append("}"); //$NON-NLS-1$
		writer.append('\n');
		writer.append("${"); //$NON-NLS-1$
		writer.append(TunnelOptions.port.name());
		writer.append("}"); //$NON-NLS-1$
		return writer.toString();
	}

}
