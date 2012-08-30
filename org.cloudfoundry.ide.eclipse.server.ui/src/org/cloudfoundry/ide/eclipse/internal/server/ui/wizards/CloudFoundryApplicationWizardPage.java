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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 */

public class CloudFoundryApplicationWizardPage extends AbstractCloudFoundryApplicationWizardPage {

	protected String filePath;

	private Map<String, String> frameworksByLabel;

	private Combo frameworkCombo;

	public CloudFoundryApplicationWizardPage(CloudFoundryServer server,
			CloudFoundryDeploymentWizardPage deploymentPage, ApplicationModule module) {
		super(server, deploymentPage, module);
	}

	protected Composite createAdditionalContents(Composite parent) {
		createFrameworkArea(parent);
		Composite composite = super.createAdditionalContents(parent);
		return composite;
	}

	protected Map<String, String> getFrameworksByLabel() {
		// Rails, Spring, Grails, Roo, JavaWeb, Sinatra, Node
		Map<String, String> valuesByLabel = new LinkedHashMap<String, String>();
		valuesByLabel.put("Spring", DeploymentConstants.SPRING);
		valuesByLabel.put("Grails", DeploymentConstants.GRAILS);
		valuesByLabel.put("Lift", DeploymentConstants.LIFT);
		valuesByLabel.put("Java Web", DeploymentConstants.JAVA_WEB);
		return valuesByLabel;
	}

	protected void createFrameworkArea(Composite composite) {
		frameworksByLabel = getFrameworksByLabel();
		Label frameworkLabel = new Label(composite, SWT.NONE);
		frameworkLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		frameworkLabel.setText("Application Type:");

		String defaultFramework = getInitialValue();

		frameworkCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		frameworkCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		int index = 0;
		for (Map.Entry<String, String> entry : frameworksByLabel.entrySet()) {
			frameworkCombo.add(entry.getKey());
			if (entry.getValue().equals(defaultFramework)) {
				index = frameworkCombo.getItemCount() - 1;
			}
		}
		frameworkCombo.select(index);

		setFramework(frameworksByLabel.get(frameworkCombo.getText()));

		frameworkCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				update();
				String selectedFramework = frameworksByLabel.get(frameworkCombo.getText());
				setFramework(selectedFramework);
			}
		});
	}

	protected ApplicationInfo detectApplicationInfo(ApplicationModule module) {
		ApplicationInfo applicationInfo = super.detectApplicationInfo(module);
		String framework = CloudUtil.getFramework(module);
		applicationInfo.setFramework(framework);
		return applicationInfo;
	}

	public File getWarFile() {
		if (filePath != null) {
			return new File(filePath);
		}
		return null;
	}

	protected String getInitialValue() {
		ApplicationInfo info = getLastApplicationInfo();
		return info != null ? info.getFramework() : null;
	}

}
