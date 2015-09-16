/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.eclipse.cft.server.core.internal.client;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public abstract class WaitWithProgressJob extends AbstractWaitWithProgressJob<Boolean> {

	public WaitWithProgressJob(int attempts, long sleepTime) {
		super(attempts, sleepTime);
	}

	@Override
	protected Boolean runInWait(IProgressMonitor monitor) throws CoreException {
		boolean result = internalRunInWait(monitor);
		return new Boolean(result);
	}

	abstract protected boolean internalRunInWait(IProgressMonitor monitor) throws CoreException;

	@Override
	protected boolean isValid(Boolean result) {
		return result != null && result.booleanValue();
	}

}
