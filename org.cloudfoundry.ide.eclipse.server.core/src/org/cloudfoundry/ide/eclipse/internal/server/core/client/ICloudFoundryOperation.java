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
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A Basic unit of operation that involves both local plugin as well as client
 * steps as part of one single operation. For example, for starting an
 * application, an operation may include all of these substeps:
 * <p/>
 * 1. Starting an operation from a UI event
 * <p/>
 * 2. Prompting the user for missing application deployment information
 * <p/>
 * 3. Sending a start request to the CF client
 * <p/>
 * 4. Checking that the application started
 * <p/>
 * 5. Firing a refresh event so listeners, like UI editors and views, can fresh
 * the state of the application in the UI
 * <p/>
 * 6. As well as handling errors and synchronizing with other background jobs
 * 
 * <p/>
 * All these steps are modeled by one operation abstraction, that may be
 * performed atomically if necessary.
 * 
 */
public interface ICloudFoundryOperation {

	public void run(IProgressMonitor monitor) throws CoreException;

}
