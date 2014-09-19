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
package org.cloudfoundry.ide.eclipse.server.ui.internal.editor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * @author Terry Denney
 */
public class CloudUrlDialog extends Dialog {

	private String name;
	
	private String url;

	private Label messageLabel;

	private final List<CloudServerURL> allCloudUrls;
	
	public CloudUrlDialog(Shell parentShell, List<CloudServerURL> allCloudUrls) {
		super(parentShell);
		this.allCloudUrls = allCloudUrls;
	}
	
	public CloudUrlDialog(Shell parentShell, String name, String url, List<CloudServerURL> allCloudUrls) {
		super(parentShell);
		this.name = name;
		this.url = url;
		this.allCloudUrls = allCloudUrls;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Messages.CloudUrlDialog_TEXT_CLOUD_URL);
		
		Composite composite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).hint(400, SWT.DEFAULT).applyTo(composite);
		GridLayoutFactory.fillDefaults().margins(10, 10).numColumns(2).applyTo(composite);
		
		messageLabel = new Label(composite, SWT.NONE);
		messageLabel.setText(Messages.CloudUrlDialog_TEXT_ENTER_URL_LABEL);
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
				update();
			}
		});
		
		return super.createDialogArea(parent);
	}
	
	private void update() {
		boolean canFinish = false;
		
		if (name == null || name.length() == 0 ) {
			messageLabel.setText(Messages.CloudUrlDialog_TEXT_ENTER_URL_NAME);
		} else if (url == null || url.length() == 0) {
			messageLabel.setText(Messages.CloudUrlDialog_TEXT_ENTER_URL);
		} else {
			canFinish = true;
			
//			List<CloudURL> cloudUrls = CloudUiUtil.getAllUrls(serverTypeId);
			for(CloudServerURL cloudUrl: allCloudUrls) {
				if (! cloudUrl.getUrl().contains("{")) { //$NON-NLS-1$
					if (cloudUrl.getName().equals(name)) {
						canFinish = false;
						messageLabel.setText(NLS.bind(Messages.CloudUrlDialog_TEXT_URL_EXISTS, name));
					}
				}
			}
			
			if (canFinish) {
				try {
					URL urlObject = new URL(url);
					String host = urlObject.getHost();
					if (host == null || host.length() == 0) {
						canFinish = false;
						messageLabel.setText(Messages.CloudUrlDialog_TEXT_ENTER_VALID_URL);
					}
				} catch (MalformedURLException e) {
					messageLabel.setText(Messages.CloudUrlDialog_TEXT_ENTER_VALID_URL);
					canFinish = false;
				} catch (CloudFoundryException e) {
					messageLabel.setText(Messages.CloudUrlDialog_TEXT_ENTER_VALID_CONTROLLER);
					canFinish = false;
				}
				
				if (canFinish) {
					messageLabel.setText(Messages.CloudUrlDialog_TEXT_CREATE_NEW_URL);
				}
			}
		}		
		getButton(IDialogConstants.OK_ID).setEnabled(canFinish);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		update();
	}
	
	public String getName() {
		return name;
	}
	
	public String getUrl() {
		return url;
	}
	
	@Override
	protected void okPressed() {
		final boolean[] shouldProceed = new boolean[] {false};
		
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			
			public void run() {
				try {
					CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(url).getCloudInfo();
					shouldProceed[0] = true;
				}
				catch (Exception e) {
					shouldProceed[0] = MessageDialog.openQuestion(getParentShell(), Messages.CloudUrlDialog_TEXT_INVALID_URL, NLS.bind(Messages.CloudUrlDialog_TEXT_CONN_FAILED, url));
				}
			}
		});

		if (shouldProceed[0]) {
			super.okPressed();
		}
	}

}
