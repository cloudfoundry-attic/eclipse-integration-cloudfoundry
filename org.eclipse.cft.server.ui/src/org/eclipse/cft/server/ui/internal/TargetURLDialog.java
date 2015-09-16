/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
 *     IBM - Switching to use the more generic AbstractCloudFoundryUrl
 *     		instead concrete CloudServerURL
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.AbstractCloudFoundryUrl;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.eclipse.jface.dialogs.Dialog;
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
public class TargetURLDialog extends Dialog {
	
	private final AbstractCloudFoundryUrl cloudUrl;
	
	private final String wildcard;
		
	private String value;
	
	private String name;
	
	private String url;

	private Text wildcardText;

	private Text nameText;

	private final List<AbstractCloudFoundryUrl> allCloudUrls;

	/**
	 * @deprecated use {@link #TargetURLDialog(Shell, AbstractCloudFoundryUrl, String, List)} instead
	 */
	public TargetURLDialog(Shell parentShell, CloudServerURL cloudUrl, String wildcard, List<CloudServerURL> allCloudUrls) {
		super(parentShell);
		this.cloudUrl = cloudUrl;
		this.wildcard = wildcard;
		this.allCloudUrls = new ArrayList <AbstractCloudFoundryUrl>();
		for (int i = 0; i < allCloudUrls.size(); ++i) {
			this.allCloudUrls.add(allCloudUrls.get(i));
		}
		this.url = cloudUrl.getUrl();
	}
	
	public TargetURLDialog(Shell parentShell, AbstractCloudFoundryUrl cloudUrl, String wildcard, List<AbstractCloudFoundryUrl> allCloudUrls) {
		super(parentShell);
		this.cloudUrl = cloudUrl;
		this.wildcard = wildcard;
		this.allCloudUrls = allCloudUrls;
		this.url = cloudUrl.getUrl();
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(NLS.bind(Messages.TargetURLDialog_TEXT_CREATE_DIALOG_SHELL, cloudUrl.getName()));
		
		Composite control = (Composite) super.createDialogArea(parent);
		
		Composite composite = new Composite(control, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(composite);
		GridLayoutFactory.fillDefaults().applyTo(composite);
		
		Label wildcardLabel = new Label(composite, SWT.NONE);
		wildcardLabel.setText(NLS.bind(Messages.TargetURLDialog_TEXT_WILDCARD_LABEL, wildcard));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).hint(300, SWT.DEFAULT).applyTo(wildcardLabel);
		
		wildcardText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(wildcardText);
		wildcardText.setEnabled(true);
		wildcardText.setText(wildcard);
		value = wildcard;
		wildcardText.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				if (nameText.getText().equals(cloudUrl.getName() + " (" + value + ")")) { //$NON-NLS-1$ //$NON-NLS-2$
					nameText.setText(cloudUrl.getName() + " (" + wildcardText.getText() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				value = wildcardText.getText();
			}
		});
		
		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText(Messages.TargetURLDialog_TEXT_NAMELABEL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).indent(0, 5).applyTo(nameLabel);
		
		nameText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(nameText);
		nameText.setEnabled(true);
		name = cloudUrl.getName() + " (" + value + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		nameText.setText(name);
		nameText.addModifyListener(new ModifyListener() {
			
			public void modifyText(ModifyEvent e) {
				name = nameText.getText();
			}
		});
		
		return control;
	}
	
	@Override
	protected void okPressed() {
//		List<CloudURL> allUrls = CloudUiUtil.getAllUrls(serverTypeId);
		for(AbstractCloudFoundryUrl url: allCloudUrls) {
			if (url.getName().equals(name)) {
				MessageDialog.openError(getParentShell(), Messages.TargetURLDialog_ERROR_DUPLICATE_TITLE, NLS.bind(Messages.TargetURLDialog_ERROR_DUPLICATE_BODY, name));
				return;
			}
		}
		
		final boolean[] shouldProceed = new boolean[] {false};
		
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			
			public void run() {
				url = replaceWildcard(cloudUrl.getUrl(), wildcard, value);
				try {
					CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(url).getCloudInfo();
					shouldProceed[0] = true;
				}
				catch (Exception e) {
					shouldProceed[0] = MessageDialog.openQuestion(getParentShell(), Messages.TargetURLDialog_ERROR_INVALID_URL_TITLE, NLS.bind(Messages.TargetURLDialog_ERROR_INVALID_URL_BODY, url));
				}
			}
		});

		if (shouldProceed[0]) {
			super.okPressed();
		}
	}
	
	private static String replaceWildcard(String url, String wildcard, String value) {
		return url.replaceAll("\\{" + wildcard + "\\}", value); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}
