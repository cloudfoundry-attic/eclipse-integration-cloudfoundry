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
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;

/**
 * Wait for an application to start. Will attempt various times. If error
 * occurs, will terminate any further attempts.
 * 
 */
public class WaitApplicationToStartOp extends AbstractWaitForStateOperation {

	public WaitApplicationToStartOp(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule) {
		super(cloudServer, appModule);
	}

	@Override
	protected boolean isInState(AppState state) {
		return state.equals(CloudApplication.AppState.STARTED);
	}

}
