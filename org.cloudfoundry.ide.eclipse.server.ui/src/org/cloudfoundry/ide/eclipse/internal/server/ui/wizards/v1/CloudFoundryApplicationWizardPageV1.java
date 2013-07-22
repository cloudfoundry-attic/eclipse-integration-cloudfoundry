/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationFramework;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ApplicationRuntime;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDescriptor;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryApplicationWizardPage;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.CloudFoundryDeploymentWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Old V1 CF implementation that contains frameworks and runtimes. Kept only as
 * a reference.
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 * @deprecated
 */
public class CloudFoundryApplicationWizardPageV1 extends CloudFoundryApplicationWizardPage {

	protected Map<String, ApplicationRuntime> runtimeByLabels;

	private Combo runtimeCombo;

	protected Map<String, ApplicationFramework> frameworksByLabel;

	private Combo frameworkCombo;

	protected ApplicationRuntime selectedRuntime;

	protected ApplicationFramework selectedFramework;

	public CloudFoundryApplicationWizardPageV1(CloudFoundryServer server,
			CloudFoundryDeploymentWizardPage deploymentPage, CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor) {
		super(server, deploymentPage, module, descriptor);

	}

	protected void initRuntimesAndFrameworks() {
		runtimeByLabels = new HashMap<String, ApplicationRuntime>();
		CloudFoundryApplicationWizardV1 v1Wizard = (CloudFoundryApplicationWizardV1) getApplicationWizard();
		List<ApplicationRuntime> allRuntimes = v1Wizard.getRuntimes();

		if (allRuntimes != null) {
			for (ApplicationRuntime runtime : allRuntimes) {
				runtimeByLabels.put(runtime.getDisplayName(), runtime);
			}
		}

		List<ApplicationFramework> frameworks = v1Wizard.getFrameworks();
		frameworksByLabel = new HashMap<String, ApplicationFramework>();

		if (frameworks != null) {
			for (ApplicationFramework framework : frameworks) {
				frameworksByLabel.put(framework.getDisplayName(), framework);
			}
		}

		// See if there already is a current staging. If so read the framework
		// and runtime
		if (descriptor.getStaging() != null) {
			String frameworkValue = descriptor.getStaging().getFramework();
			String runtimeValue = descriptor.getStaging().getRuntime();

			for (ApplicationFramework fw : frameworksByLabel.values()) {
				if (fw.getFramework().equals(frameworkValue)) {
					selectedFramework = fw;
					break;
				}
			}

			for (ApplicationRuntime rt : runtimeByLabels.values()) {
				if (rt.getRuntime().equals(runtimeValue)) {
					selectedRuntime = rt;
					break;
				}
			}
		}
		else {
			// Otherwise, set default values
			selectedFramework = frameworksByLabel.size() > 0 ? frameworksByLabel.values().iterator().next() : null;
			selectedRuntime = runtimeByLabels.size() > 0 ? runtimeByLabels.values().iterator().next() : null;
			setStaging();
		}

		// Override the default framework value with the one in the last
		// application info, if available
		ApplicationInfo lastApplicationInfo = module.getLastApplicationInfo();
		if (lastApplicationInfo != null) {
			// Use the framework from the previous application info
			String lastFramework = lastApplicationInfo.getFramework();
			if (lastFramework != null) {
				selectedFramework = frameworksByLabel.get(lastFramework);
			}
		}

	}

	protected void createRuntimeArea(Composite composite) {

		if (runtimeByLabels.size() > 0) {

			Label runtimeLabel = new Label(composite, SWT.NONE);
			runtimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			runtimeLabel.setText("Runtime: ");

			// Either show the runtime as a label if only one runtime exists, or
			// show a combo if
			// multiple runtimes exist
			if (runtimeByLabels.size() == 1) {
				Label runtime = new Label(composite, SWT.NONE);
				runtime.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
				String label = selectedRuntime != null ? selectedRuntime.getDisplayName() : null;
				if (label != null) {
					runtime.setText(label);

				}
			}
			else if (runtimeByLabels.size() > 1) {

				runtimeCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
				runtimeCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				int index = 0;
				for (Map.Entry<String, ApplicationRuntime> entry : runtimeByLabels.entrySet()) {
					runtimeCombo.add(entry.getKey());
					if (entry.getValue().equals(selectedRuntime)) {
						index = runtimeCombo.getItemCount() - 1;
					}
				}
				runtimeCombo.select(index);

				runtimeCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {

						selectedRuntime = runtimeByLabels.get(runtimeCombo.getText());

						update();
					}
				});
			}
		}
	}

	protected void setStaging() {
		if (selectedFramework != null && selectedRuntime != null) {
			descriptor.setStaging(selectedFramework, selectedRuntime);
		}
	}

	@Override
	protected void update(boolean updateButtons) {
		setStaging();
		super.update(updateButtons);
	}

	protected Composite createContents(Composite parent) {

		Composite composite = super.createContents(parent);

		initRuntimesAndFrameworks();
		createFrameworkArea(composite);
		createRuntimeArea(composite);

		return composite;

	}

	protected void createFrameworkArea(Composite composite) {

		// Don't show combo if only one framework entry is present
		if (frameworksByLabel.size() < 2) {
			return;
		}

		Label frameworkLabel = new Label(composite, SWT.NONE);
		frameworkLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		frameworkLabel.setText("Application Type:");

		frameworkCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		frameworkCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		int index = 0;
		for (Map.Entry<String, ApplicationFramework> entry : frameworksByLabel.entrySet()) {
			frameworkCombo.add(entry.getKey());
			if (entry.getValue().equals(selectedFramework)) {
				index = frameworkCombo.getItemCount() - 1;
			}
		}
		frameworkCombo.select(index);

		frameworkCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {

				selectedFramework = frameworksByLabel.get(frameworkCombo.getText());

				update();
			}
		});
	}

}
