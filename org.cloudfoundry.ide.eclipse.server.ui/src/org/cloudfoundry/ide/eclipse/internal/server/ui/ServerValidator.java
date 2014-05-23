/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.eclipse.jface.operation.IRunnableContext;

public interface ServerValidator {

	/**
	 * Validates server credentials and orgs and spaces selection. The contract
	 * should be: if not validating against the server, validate locally (e.g.
	 * ensure password and username are entered and have valid characters). If
	 * validating against a server, do a full validation and ensure an org and
	 * space are selected for the credentials.
	 * @param validateAgainstServer true if validation should be local, or false
	 * if requires server validation.
	 * @param validateSpace true if cloud space should be validated.
	 * @param runnableContext. May be null. If null, a default runnable context
	 * should be used.
	 * @return non-null validation status
	 */
	public abstract ValidationStatus validate(boolean validateAgainstServer, boolean validateSpace,
			IRunnableContext runnableContext);

}