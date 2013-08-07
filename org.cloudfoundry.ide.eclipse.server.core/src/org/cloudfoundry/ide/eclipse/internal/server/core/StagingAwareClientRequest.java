/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.NotFinishedStagingException;
import org.cloudfoundry.client.lib.StagingErrorException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;

public abstract class StagingAwareClientRequest<T> extends CloudFoundryClientRequest<T> {

	public StagingAwareClientRequest(CloudFoundryOperations client, CloudFoundryServer server, long requestTimeOut) {
		super(client, server, requestTimeOut);
	}

	protected long getWaitInterval(Throwable exception, SubMonitor monitor) throws CoreException {

		if (exception instanceof CoreException) {
			exception = ((CoreException) exception).getCause();
		}

		if (exception instanceof NotFinishedStagingException) {
			return ONE_SECOND_INTERVAL * 2;
		}
		else if (exception instanceof StagingErrorException) {
			return -1;
		}
		else if (exception instanceof CloudFoundryException
				&& CloudErrorUtil.isAppStoppedStateError((CloudFoundryException) exception)) {
			return ONE_SECOND_INTERVAL;
		}
		return -1;
	}
}
