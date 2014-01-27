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

/**
 * 
 * Using getters and setters with no-argument constructors for JSON
 * serialisation
 * 
 */
public class ExternalApplication {

	private String executableNameAndPath;

	public ExternalApplication() {

	}

	public void setExecutableNameAndPath(String executableName) {
		this.executableNameAndPath = executableName;
	}

	public String getExecutableNameAndPath() {
		return executableNameAndPath;
	}
}