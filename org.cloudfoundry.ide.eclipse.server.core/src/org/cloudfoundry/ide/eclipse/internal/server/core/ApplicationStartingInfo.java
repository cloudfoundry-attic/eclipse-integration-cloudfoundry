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

import java.util.List;

import org.cloudfoundry.client.lib.StartingInfo;

/**
 * Contains information like staging logs when an application is starting.
 */
public class ApplicationStartingInfo {

	final private List<String> stagingLog;

	final private StartingInfo startingInfo;

	public ApplicationStartingInfo(List<String> stagingLog, StartingInfo startingInfo) {
		this.stagingLog = stagingLog;
		this.startingInfo = startingInfo;
	}

	public List<String> getStagingLog() {
		return stagingLog;
	}

	public StartingInfo getStartingInfo() {
		return startingInfo;
	}

}
