/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;

public class BehaviourEvent<T> {

	public final CloudFoundryApplicationModule appModule;

	public final T behaviourResult;

	public final BehaviourEventType type;

	public final CloudFoundryServer server;

	public BehaviourEvent(CloudFoundryApplicationModule appModule, CloudFoundryServer server, T behaviourResult,
			BehaviourEventType type) {
		this.appModule = appModule;
		this.behaviourResult = behaviourResult;
		this.type = type;
		this.server = server;
	}

	public CloudFoundryApplicationModule getApplicationModule() {
		return appModule;
	}

	public CloudFoundryServer getServer() {
		return server;
	}

	public T getResult() {
		return behaviourResult;
	}

	public BehaviourEventType getType() {
		return type;
	}

}
