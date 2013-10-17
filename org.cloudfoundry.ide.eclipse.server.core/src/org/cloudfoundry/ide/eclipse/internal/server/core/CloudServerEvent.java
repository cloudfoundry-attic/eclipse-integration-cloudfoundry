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

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Leo Dos Santos
 * @author Terry Denney
 */
public class CloudServerEvent extends EventObject {

	public static final int EVENT_UPDATE_INSTANCES = 100;

	public static final int EVENT_UPDATE_SERVICES = 200;

	public static final int EVENT_UPDATE_PASSWORD = 300;

	public static final int EVENT_SERVER_REFRESHED = 400;

	private static final long serialVersionUID = 1L;

	private int type = -1;

	public CloudServerEvent(CloudFoundryServer server) {
		this(server, -1);
	}

	public CloudServerEvent(CloudFoundryServer server, int type) {
		super(server);
		Assert.isNotNull(server);
		this.type = type;
	}

	public CloudFoundryServer getServer() {
		return (CloudFoundryServer) getSource();
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
