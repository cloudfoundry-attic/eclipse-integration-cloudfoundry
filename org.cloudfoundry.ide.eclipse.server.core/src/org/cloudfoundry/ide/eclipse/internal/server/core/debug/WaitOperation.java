/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.debug;

import org.eclipse.core.runtime.IProgressMonitor;

abstract class WaitOperation {

	/**
	 * Return true if successful after all tries are attempted. False if
	 * unsuccessful after maximum tries reached.
	 * @param monitor
	 * @return
	 */
	public boolean doWait(IProgressMonitor monitor) {
		boolean success = false;
		int retry = 0;

		do {
			success = runInWaitCycle(monitor);

			if (!success && (retry < maximumTries())) {
				try {
					Thread.sleep(getWaitTime());
				}
				catch (InterruptedException e) {
					// InterruptedExceptions not generally expected,
					// therefore break if thrown
					break;
				} 
			}
			else {
				break;
			}
			retry++;
		} while (!success);

		return success;
	}

	protected int getWaitTime() {
		return 50;
	}

	protected int maximumTries() {
		return 5;
	}

	/**
	 * performs an operation during one run and wait cycle.
	 * @param monitor
	 * @return true if run is successful, and no further tries are
	 * necessary. return false, if further tries are necessary
	 */
	protected abstract boolean runInWaitCycle(IProgressMonitor monitor);
}