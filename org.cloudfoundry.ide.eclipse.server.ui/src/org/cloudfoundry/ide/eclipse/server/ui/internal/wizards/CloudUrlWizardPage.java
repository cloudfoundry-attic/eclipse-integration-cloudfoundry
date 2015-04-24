/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
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

	private boolean selfSigned;

	private Label messageLabel;

	boolean canFinish = false;

	private final List<CloudServerURL> allCloudUrls;

	protected static final String TITLE = Messages.CloudUrlWizardPage_TITLE_CLOUD_URL;

	protected static final String DESCRIPTION = Messages.CloudUrlWizardPage_TEXT_DESCRIPT;

	protected CloudUrlWizardPage(List<CloudServerURL> allCloudUrls, ImageDescriptor descriptor, String url,
			String name, boolean selfSigned) {
		super(Messages.CloudUrlWizardPage_TEXT_CLOUD_URL);
		this.allCloudUrls = allCloudUrls;
		this.name = name;
		this.url = url;
		this.selfSigned = selfSigned;

		setTitle(TITLE);
		setDescription(DESCRIPTION);

		if (descriptor != null) {
			setImageDescriptor(descriptor);
		}
	}

	public void createControl(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(400, SWT.DEFAULT).applyTo(area);
		GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(1).applyTo(area);

		Composite composite = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);

		messageLabel = new Label(composite, SWT.NONE);
		messageLabel.setText(Messages.CloudUrlWizardPage_LABEL_CLOUD_URL_NAME);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(messageLabel);

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText(Messages.COMMONTXT_NAME_WITH_COLON);
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
		urlLabel.setText(Messages.COMMONTXT_URL);
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

		Composite buttonArea = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(buttonArea);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(buttonArea);

		final Button selfSignedButton = new Button(buttonArea, SWT.CHECK);
		selfSignedButton.setText(Messages.CloudUrlWizardPage_SELF_SIGNED);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(selfSignedButton);
		selfSignedButton.setSelection(selfSigned);

		selfSignedButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				CloudUrlWizardPage.this.selfSigned = selfSignedButton.getSelection();
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
			messageLabel.setText(Messages.CloudUrlWizardPage_LABEL_A_CLOUD_URL);
			canFinish = false;
		}
		else if (url == null || url.length() == 0) {
			messageLabel.setText(Messages.CloudUrlWizardPage_LABEL_ENTER_CLOUD_URL);
			canFinish = false;
		}
		else {
			canFinish = true;

			// List<CloudURL> cloudUrls = CloudUiUtil.getAllUrls(serverTypeId);
			for (CloudServerURL cloudUrl : allCloudUrls) {
				if (!cloudUrl.getUrl().contains("{")) { //$NON-NLS-1$
					if (cloudUrl.getName().equals(name)) {
						canFinish = false;
						messageLabel.setText(NLS.bind(Messages.CloudUrlWizardPage_LABEL_SET_DIFF_URL, name));
					}
				}
			}

			if (canFinish) {
				try {
					URL urlObject = new URL(url);
					String host = urlObject.getHost();
					if (host == null || host.length() == 0) {
						canFinish = false;
						messageLabel.setText(Messages.COMMONTXT_ENTER_VALID_URL);
					}
				}
				catch (MalformedURLException e) {
					messageLabel.setText(Messages.COMMONTXT_ENTER_VALID_URL);
					canFinish = false;
				}
				catch (CloudFoundryException e) {
					messageLabel.setText(Messages.CloudUrlWizardPage_LABEL_INVALID_CONTROLLER);
					canFinish = false;
				}

				if (canFinish) {
					messageLabel.setText(Messages.CloudUrlWizardPage_LABEL_CREATE_NEW_URL);
				}
			}
		}

		if (canFinish) {
			setErrorMessage(null);
		}
		else {
			String messageText = messageLabel.getText();

			setErrorMessage(messageText != null && messageText.length() > 0 ? messageLabel.getText()
					: Messages.CloudUrlWizardPage_ERROR_INVALID_URL);
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

	public boolean getSelfSigned() {
		return selfSigned;
	}

	@Override
	public boolean isPageComplete() {
		return canFinish;
	}

}
