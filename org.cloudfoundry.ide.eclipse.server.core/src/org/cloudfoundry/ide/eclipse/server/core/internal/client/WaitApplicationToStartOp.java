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
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;

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
