/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.core.internal;

public enum ApplicationAction {
	RESTART, START("Run"), STOP, UPDATE_RESTART, DEBUG("Debug"), //$NON-NLS-1$ //$NON-NLS-2$

	// This action should only be selected if an app is already deployed in
	// debug mode
	// but still needs to be connected to the local debugger.
	CONNECT_TO_DEBUGGER("Connect to Debugger"); //$NON-NLS-1$

	private String displayName = ""; //$NON-NLS-1$

	private String shortDisplay = ""; //$NON-NLS-1$

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