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
package org.cloudfoundry.ide.eclipse.internal.server.core;

public enum ApplicationAction {
	RESTART, START("Run"), STOP, UPDATE_RESTART, DEBUG("Debug"),

	// This action should only be selected if an app is already deployed in
	// debug mode
	// but still needs to be connected to the local debugger.
	CONNECT_TO_DEBUGGER("Connect to Debugger");

	private String displayName = "";

	private String shortDisplay = "";

	private ApplicationAction(String displayName, String shortDisplay) {
		this.displayName = displayName;
		this.shortDisplay = shortDisplay;
	}

	private ApplicationAction(String displayName) {
		this.displayName = displayName;
	}

	private ApplicationAction() {

	}

	public String getDisplayName() {
		return displayName;
	}

	public String getShortDisplay() {
		return shortDisplay;
	}

}