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

public abstract class AbstractApplicationWizardDelegate implements IApplicationWizardDelegate {

	public boolean isValid(ApplicationWizardDescriptor applicationDescriptor) {
		boolean canFinish = applicationDescriptor.getApplicationInfo() != null
				&& applicationDescriptor.getDeploymentInfo() != null;

		if (canFinish) {
			if (applicationDescriptor instanceof CCNGV2ApplicationWizardDescriptor) {
				canFinish = ((CCNGV2ApplicationWizardDescriptor) applicationDescriptor).getApplicationPlan() != null;
			}
			else {
				// Only "legacy" V1 servers require staging, framework and a
				// runtime
				canFinish = applicationDescriptor.getStaging() != null
						&& applicationDescriptor.getStaging().getFramework() != null
						&& applicationDescriptor.getStaging().getRuntime() != null;
			}

		}

		return canFinish;
	}

}
