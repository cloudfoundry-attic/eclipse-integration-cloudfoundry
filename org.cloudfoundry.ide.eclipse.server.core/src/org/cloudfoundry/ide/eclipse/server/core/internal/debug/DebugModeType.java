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
package org.cloudfoundry.ide.eclipse.server.core.internal.debug;

import org.cloudfoundry.client.lib.domain.CloudApplication.DebugMode;
import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationAction;


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