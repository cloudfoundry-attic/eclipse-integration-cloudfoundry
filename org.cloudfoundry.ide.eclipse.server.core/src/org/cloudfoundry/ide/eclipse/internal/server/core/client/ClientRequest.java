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
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryLoginHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * A Request performs a CF request via the cloudfoundry-client-lib API. It
 * performs various error handling and connection checks that are generally
 * common to most client requests, and therefore any client request should be
 * wrapped around a Request.
 * <p/>
 * By default, the set of client calls in the Request is made twice, the first
 * time it is executed immediate. If it fails due to connection error error, it
 * will attempt a second time.
 * <p/>
 * Subtypes can modify this behaviour and add conditions that will result in
 * further retries aside from connection errors.
 * 
 * 
 * @param <T>
 * 
 */
public abstract class ClientRequest<T> extends BaseClientRequest<T> {

	public ClientRequest(String label) {
		super(label);
	}

	/**
	 * Attempts to execute the client request by first checking proxy settings,
	 * and if unauthorised/forbidden exceptions thrown the first time, will
	 * attempt to log in. If that succeeds, it will attempt one more time.
	 * Otherwise it will fail and not attempt the request any further.
	 * @param client
	 * @param cloudServer
	 * @param subProgress
	 * @return
	 * @throws CoreException if attempt to execute failed, even after a second
	 * attempt after a client login.
	 */
	@Override
	protected T runAndWait(CloudFoundryOperations client, SubMonitor subProgress) throws CoreException {

		try {
			return super.runAndWait(client, subProgress);
		}
		catch (CoreException ce) {
			CloudFoundryLoginHandler handler = new CloudFoundryLoginHandler(client);
			if (handler.shouldAttemptClientLogin(ce)) {
				int attempts = 3;
				handler.login(subProgress, attempts, CloudOperationsConstants.LOGIN_INTERVAL);
				return runAndWait(client, subProgress);
			}
			throw ce;
		}

	}

}