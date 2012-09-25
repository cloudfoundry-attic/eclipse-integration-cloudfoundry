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
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.ServiceConfiguration;
import org.cloudfoundry.client.lib.domain.ServiceConfiguration.Option;
import org.cloudfoundry.client.lib.domain.ServiceConfiguration.Tier;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * @author Steffen Pingel
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class CloudFoundryServiceWizardPage extends AbstractCloudFoundryServiceWizardPage {

	protected CloudFoundryServiceWizardPage(CloudFoundryServer cloudServer) {
		super(cloudServer);
	}

	protected List<ServicePlan> getPlans(ServiceConfiguration configuration) {
		List<Tier> tiers = configuration.getTiers();
		List<ServicePlan> plans = new ArrayList<AbstractCloudFoundryServiceWizardPage.ServicePlan>();
		if (tiers != null) {
			for (Tier tier : tiers) {
				plans.add(new ServiceTier(service, tier));
			}
		}
		return plans;
	}

	protected String getValidationErrorMessage() {
		return "Select a tier";
	}

	protected String getPlanLabel() {
		return "Tier";
	}

	protected void refreshPlanDetails(ServicePlan tier, Composite planDetailsComposite) {

		if (tier instanceof ServiceTier) {
			ServiceTier serviceTier = (ServiceTier) tier;
			for (Control control : planDetailsComposite.getChildren()) {
				control.dispose();
			}
			for (final Option option : serviceTier.getTier().getOptions()) {
				Label label = new Label(planDetailsComposite, SWT.NONE);
				label.setText(option.getDescription());

				final Combo combo = new Combo(planDetailsComposite, SWT.READ_ONLY);
				final List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(option
						.getPriceByValue().entrySet());
				for (Entry<String, Integer> entry : list) {
					if (entry.getValue() != null) {
						combo.add(NLS.bind("{0} (${1}/{2})", new Object[] { entry.getKey(), entry.getValue(),
								serviceTier.getTier().getPricingPeriod() }));
					}
					else {
						combo.add(entry.getKey());
					}
				}
				combo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						service.getOptions().put(option.getName(), list.get(combo.getSelectionIndex()).getKey());
					}
				});

				bindingContext.bindValue(SWTObservables.observeSelection(combo), Observables.observeMapEntry(map,
						option.getName()), new UpdateValueStrategy().setAfterConvertValidator(new ComboValidator(NLS
						.bind("Select a {0}", option.getName()))), null);
			}

			planGroup.layout(true, true);
		}

	}

	protected void sortServicePlans(List<ServiceConfiguration> configurations) {
		for (ServiceConfiguration configuration : configurations) {
			Collections.sort(configuration.getTiers(), new Comparator<Tier>() {
				public int compare(Tier o1, Tier o2) {
					return o1.getOrder() - o2.getOrder();
				}
			});
		}
	}

	class ServiceTier extends ServicePlan {

		private Tier tier;

		public ServiceTier(CloudService service, Tier tier) {
			super(service);
			this.tier = tier;
		}

		protected Tier getTier() {
			return tier;
		}

		protected String getDisplayValue() {
			return tier.getDescription() != null ? tier.getDescription() : tier.getType();
		}

		public void updateInService() {
			getService().setTier(tier.getType());
		}

	}

	@Override
	protected void setCloudService(CloudService service, ServiceConfiguration configuration) {
		service.setType(configuration.getType());
		service.setVendor(configuration.getVendor());
		service.setVersion(configuration.getVersion());
	}

}
