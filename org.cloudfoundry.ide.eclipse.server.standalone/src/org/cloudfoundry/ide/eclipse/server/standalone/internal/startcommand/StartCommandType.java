/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.startcommand;

public enum StartCommandType {
	Java("Java start command"), Other("Other start command");

	private String description;

	private StartCommandType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}