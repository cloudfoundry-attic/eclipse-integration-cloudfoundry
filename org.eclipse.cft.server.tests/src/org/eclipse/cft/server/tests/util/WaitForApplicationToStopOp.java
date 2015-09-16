/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
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
package org.eclipse.cft.server.tests.util;

import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;

/**
 * Wait for an application to stop. Will attempt various times. If error occurs,
 * will terminate any further attempts.
 *
 */
public class WaitForApplicationToStopOp extends AbstractWaitForStateOperation {

	public WaitForApplicationToStopOp(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule) {
		super(cloudServer, appModule);
	}

	@Override
	protected boolean isInState(AppState state) {
		return state.equals(AppState.STOPPED);
	}

}
