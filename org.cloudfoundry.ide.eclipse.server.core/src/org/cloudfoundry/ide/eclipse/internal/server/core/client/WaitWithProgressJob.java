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
