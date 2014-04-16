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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Configures a server after a server operation (for example, creating a
 * server). A typical use of this callback is to set credentials in the server
 * @author Steffen Pingel
 */
public abstract class ServerHandlerCallback {

	public abstract void configureServer(IServerWorkingCopy wc) throws CoreException;

}