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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * Creates or edits an existing Cloud URL. Wizard page can only be completed if
 * a valid name and URL are entered. Validation of the URL is limited only to
 * protocol and format. Further validation of the URL in terms of connectivity
 * is performed in the wizard itself
 * 
 * @author Terry Denney
 * @author Nieraj Singh
 */
public class CloudUrlWizardPage extends WizardPage {

	private String name;

	private String url;

	private Label messageLabel;

	boolean canFinish = false;

	private final List<CloudServerURL> allCloudUrls;

	protected static final String TITLE = "Add a Cloud URL";

	protected static final String DESCRIPTION = "Finish to validate the URL.";

	protected CloudUrlWizardPage(List<CloudServerURL> allCloudUrls, ImageDescriptor descriptor, String url, String name) {
		super("Cloud URL");
		this.allCloudUrls = allCloudUrls;
		this.name = name;
		this.url = url;

		setTitle(TITLE);
		setDescription(DESCRIPTION);

		if (descriptor != null) {
			setImageDescriptor(descriptor);
		}
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(400, SWT.DEFAULT).applyTo(composite);
		GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).applyTo(composite);

		messageLabel = new Label(composite, SWT.NONE);
		messageLabel.setText("Enter cloud URL name");
		GridDataFactory.fillDefaults().span(2, 1).applyTo(messageLabel);

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText("Name: ");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(nameLabel);

		final Text nameText = new Text(composite, SWT.BORDER);
		nameText.setEditable(true);
		if (name != null) {
			nameText.setText(name);
		}
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(nameText);
		nameText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				name = nameText.getText();
				update();
			}
		});

		Label urlLabel = new Label(composite, SWT.NONE);
		urlLabel.setText("URL: ");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(urlLabel);

		final Text urlText = new Text(composite, SWT.BORDER);
		urlText.setEditable(true);
		if (url != null) {
			urlText.setText(url);
		}
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(urlText);
		urlText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				url = urlText.getText();
				// remove leading and trailing whitespaces
				if (url != null) {
					url = url.trim();
				}
				update();
			}
		});

		Dialog.applyDialogFont(composite);
		setControl(composite);

		// If url information is pre-populated, force a refresh
		if (name != null || url != null) {
			update();
		}
	}

	private void update() {

		if (name == null || name.length() == 0) {
			messageLabel.setText("Enter a cloud URL name.");
			canFinish = false;
		}
		else if (url == null || url.length() == 0) {
			messageLabel.setText("Enter a cloud URL.");
			canFinish = false;
		}
		else {
			canFinish = true;

			// List<CloudURL> cloudUrls = CloudUiUtil.getAllUrls(serverTypeId);
			for (CloudServerURL cloudUrl : allCloudUrls) {
				if (!cloudUrl.getUrl().contains("{")) {
					if (cloudUrl.getName().equals(name)) {
						canFinish = false;
						messageLabel
								.setText("A URL with name " + name + " already exists. Enter a different url name.");
					}
				}
			}

			if (canFinish) {
				try {
					URL urlObject = new URL(url);
					String host = urlObject.getHost();
					if (host == null || host.length() == 0) {
						canFinish = false;
						messageLabel.setText("Enter a valid URL.");
					}
				}
				catch (MalformedURLException e) {
					messageLabel.setText("Enter a valid URL.");
					canFinish = false;
				}
				catch (CloudFoundryException e) {
					messageLabel.setText("Enter a valid cloud controller URL.");
					canFinish = false;
				}

				if (canFinish) {
					messageLabel.setText("Create a new cloud URL.");
				}
			}
		}

		if (canFinish) {
			setErrorMessage(null);
		}
		else {
			String messageText = messageLabel.getText();

			setErrorMessage(messageText != null && messageText.length() > 0 ? messageLabel.getText()
					: "Invalid URL or name.");
		}

		getWizard().getContainer().updateButtons();
		setPageComplete(canFinish);
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public boolean isPageComplete() {
		return canFinish;
	}

}
