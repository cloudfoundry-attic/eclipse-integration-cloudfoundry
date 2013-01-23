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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationPlan;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Creates the radio button UI to display the different application plans that
 * are available to a user. All values are displayed to a user, but if the list
 * of actual plans that are available is just 1, the the controls are disabled
 * and only the default value is shown ("free").
 * 
 */
public class ApplicationPlanPart {

	private boolean enableControls;

	private ApplicationPlan selectedPlan;

	private List<Button> buttons;

	public ApplicationPlanPart(List<ApplicationPlan> actualPlans, ApplicationPlan selectedPlan) {
		// If only one application plan is present, still show all plans, but
		// disable the controls
		// and only select the one plan that is present
		enableControls = actualPlans.size() > 1;

		// Default is always free plan
		this.selectedPlan = selectedPlan != null ? selectedPlan : ApplicationPlan.free;
	}

	/**
	 * Never null. Returns list of buttons associated with all application
	 * plans, or empty list if no plans are resolved
	 * @param parent
	 * @return
	 */
	public List<Button> createButtonControls(Composite parent) {

		if (buttons != null && !buttons.isEmpty()) {
			for (Button button : buttons) {
				if (!button.isDisposed()) {
					button.dispose();
				}
			}
		}

		buttons = new ArrayList<Button>();

		for (ApplicationPlan plan : ApplicationPlan.values()) {
			final Button runRadioButton = new Button(parent, SWT.RADIO);
			runRadioButton.setText(plan.getDisplay());
			runRadioButton.setSelection(plan.equals(selectedPlan));
			runRadioButton.setData(plan);
			runRadioButton.setEnabled(enableControls);
			runRadioButton.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {

					if (runRadioButton.getSelection()) {
						selectedPlan = (ApplicationPlan) runRadioButton.getData();
					}
				}

			});
			buttons.add(runRadioButton);

		}
		return buttons;

	}

	public void setSelection(ApplicationPlan plan) {
		if (buttons != null) {
			for (Button button : buttons) {
				if (!button.isDisposed()) {
					ApplicationPlan buttonPlan = (ApplicationPlan) button.getData();
					button.setSelection(buttonPlan != null && buttonPlan.equals(plan));
				}
			}
		}
	}

	public ApplicationPlan getSelectedPlan() {
		return selectedPlan;
	}

	public boolean isEnabled() {
		return enableControls;
	}

}
