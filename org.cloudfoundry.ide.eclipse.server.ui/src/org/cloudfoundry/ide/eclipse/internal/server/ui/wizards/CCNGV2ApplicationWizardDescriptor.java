/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationPlan;

public class CCNGV2ApplicationWizardDescriptor extends ApplicationWizardDescriptor {

	private ApplicationPlan applicationPlan;

	/**
	 * Only applicable for CCNG V2 Cloud Foundry Servers. Required. Must not be
	 * null.
	 */
	public ApplicationPlan getApplicationPlan() {
		return applicationPlan;
	}

	/**
	 * Only applicable for CCNG V2 Cloud Foundry Servers
	 */
	public void setApplicationPlan(ApplicationPlan applicationPlan) {
		this.applicationPlan = applicationPlan;
	}

}
