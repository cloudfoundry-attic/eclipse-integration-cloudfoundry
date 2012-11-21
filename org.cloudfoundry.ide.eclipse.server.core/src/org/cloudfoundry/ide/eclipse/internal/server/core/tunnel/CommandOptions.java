/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

public class CommandOptions extends CommandMetaElement {
	
	private final String options;

	public CommandOptions(String options) {
		super("CommandOption");
		this.options = options;
	}
	
	public String getOptions() {
		return options;
	}

}
