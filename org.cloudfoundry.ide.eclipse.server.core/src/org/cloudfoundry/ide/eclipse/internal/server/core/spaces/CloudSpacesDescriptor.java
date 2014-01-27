/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.spaces;

public class CloudSpacesDescriptor {

	private final String descriptorID;

	private final CloudOrgsAndSpaces spaces;

	public CloudSpacesDescriptor(CloudOrgsAndSpaces spaces, String userName, String password, String actualServerURL) {
		this.spaces = spaces;
		descriptorID = getDescriptorID(userName, password, actualServerURL);

	}

	public CloudOrgsAndSpaces getOrgsAndSpaces() {
		return spaces;
	}

	public String getID() {
		return descriptorID;
	}

	public static String getDescriptorID(String userName, String password, String actualURL) {
		if (userName == null || password == null || actualURL == null) {
			return null;
		}
		return userName + password + actualURL;
	}

}
