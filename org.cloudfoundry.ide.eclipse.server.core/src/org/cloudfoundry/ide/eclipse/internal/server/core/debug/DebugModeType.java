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

import org.cloudfoundry.client.lib.domain.CloudApplication.DebugMode;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;


/**
 * Although only one Debug mode is active ("Suspend" mode) for STS Cloud
 * Foundry, the infrastructure to re-introduce "No Suspend" or any other mode is
 * left here. To add new modes, define a new enum type for that mode. As a
 * historical note, "No Suspend" mode was removed in an earlier version of Cloud
 * Foundry.
 * 
 * @author Nieraj Singh
 * 
 */
public enum DebugModeType {

	SUSPEND(DebugMode.suspend, ApplicationAction.DEBUG);

	private DebugMode mode;

	private String jobName;

	private ApplicationAction applicationAction;

	private DebugModeType(DebugMode mode, ApplicationAction applicationAction) {
		this.mode = mode;
		this.applicationAction = applicationAction;
	}

	public ApplicationAction getApplicationAction() {
		return applicationAction;
	}

	public DebugMode getDebugMode() {
		return mode;
	}

	public String getJobName() {
		if (jobName == null) {
			jobName = "Debugging in " + applicationAction.getDisplayName().toLowerCase() + " mode";
		}
		return jobName;
	}

	public static DebugModeType getDebugModeType(DebugMode mode) {
		if (mode == null) {
			return null;
		}
		switch (mode) {
		case suspend:
			return DebugModeType.SUSPEND;
		}
		return null;
	}

}