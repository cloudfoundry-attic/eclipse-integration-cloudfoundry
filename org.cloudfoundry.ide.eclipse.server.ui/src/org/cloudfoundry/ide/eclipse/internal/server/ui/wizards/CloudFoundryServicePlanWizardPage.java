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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.ServiceConfiguration;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.swt.widgets.Composite;

public class CloudFoundryServicePlanWizardPage extends AbstractCloudFoundryServiceWizardPage {

	protected CloudFoundryServicePlanWizardPage(CloudFoundryServer cloudServer) {
		super(cloudServer);
	}

	@Override
	protected void refreshPlanDetails(ServicePlan plan, Composite refreshDetailsComposite) {
		// Nothing for now
	}

	@Override
	protected void sortServicePlans(List<ServiceConfiguration> configurations) {

		for (ServiceConfiguration configuration : configurations) {
			CloudServiceOffering offering = configuration.getCloudServiceOffering();
			if (offering != null) {
				Collections.sort(offering.getCloudServicePlans(), new Comparator<CloudServicePlan>() {
					public int compare(CloudServicePlan o1, CloudServicePlan o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});
			}
		}
	}

	@Override
	protected List<ServicePlan> getPlans(ServiceConfiguration configuration) {
		List<ServicePlan> plans = new ArrayList<AbstractCloudFoundryServiceWizardPage.ServicePlan>();

		CloudServiceOffering offering = configuration.getCloudServiceOffering();
		if (offering != null) {
			List<CloudServicePlan> cloudPlans = offering.getCloudServicePlans();

			if (cloudPlans != null) {
				for (CloudServicePlan plan : cloudPlans) {
					plans.add(new InternalCloudServicePlan(service, plan));
				}
			}
		}
		return plans;

	}

	@Override
	protected String getValidationErrorMessage() {
		return "Select a plan";
	}

	@Override
	protected String getPlanLabel() {
		return "Plan";
	}

	static class InternalCloudServicePlan extends ServicePlan {

		private CloudServicePlan plan;

		public InternalCloudServicePlan(CloudService service, CloudServicePlan plan) {
			super(service);
			this.plan = plan;
		}

		@Override
		public void updateInService() {
			getService().setPlan(plan.getName());
		}

		@Override
		protected String getDisplayValue() {
			return plan.getName();
		}

	}

}
