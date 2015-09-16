/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.ui.internal.wizards.CloudUIEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 * Allows users to edit or add an application URL based on a list of existing
 * URL Cloud domains.
 * <p/>
 * Application URL is defined by subdomain and domain segments. The UP part may
 * keep track of a raw, unparsed URL , whether invalid or not, as the user has
 * option to edit the full URL manually without necessarily specifying either a
 * subdomain or domain.
 * <p/>
 * Any changes to the raw URL will be parsed into subdomain and domain segments
 * if possible, based on a list of available domains for the account. If
 * successfully parsed, any subdomain and domain controls will be updated as
 * well. If not successfully parsed, at the very minimum any full URL controls
 * will be kept up to date with the invalid URL.
 * <p/>
 * Any registered listeners are notified when there are changes to the URL, or
 * any errors have occurred during validation or parsing of the URL.
 */
public class CloudApplicationUrlPart extends UIPart {

	protected final ApplicationUrlLookupService lookupService;

	/**
	 * The current URL being edited, in raw form. Note that since URL value in
	 * the UI needs to always be up to date but the URL itself may be invalid
	 * and even not par
	 */
	private String currentUrl;

	private Control validationSource;

	private Text subDomainText;

	private Text fullURLText;

	private Combo domainCombo;

	public CloudApplicationUrlPart(ApplicationUrlLookupService lookupService) {
		this.lookupService = lookupService;
	}

	/**
	 * Note that the part will adapt the given parent into a 2 column parent
	 * that does not grab vertically. This is to allow other two column controls
	 * in the same parent to have the same column widths as the controls for the
	 * URL part. Callers are responsible for creating an appropriate parent that
	 * only contains the URL part, if they do not wish for other controls
	 * outside this part to be affected.
	 */
	public Composite createPart(Composite parent) {

		Composite subDomainComp = parent;
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(subDomainComp);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(subDomainComp);

		Label label = new Label(subDomainComp, SWT.NONE);

		label.setText(Messages.CloudApplicationUrlPart_TEXT_SUBDOMAIN_LABEL);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.CENTER).applyTo(label);

		subDomainText = new Text(subDomainComp, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(subDomainText);

		subDomainText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent arg0) {
				resolveUrlFromSubdomain(subDomainText);
			}
		});

		label = new Label(subDomainComp, SWT.NONE);
		label.setText(Messages.COMMONTXT_DOMAIN);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.CENTER).applyTo(label);

		domainCombo = new Combo(subDomainComp, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(domainCombo);
		domainCombo.setEnabled(true);
		domainCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				resolveUrlFromSubdomain(domainCombo);
			}
		});

		label = new Label(subDomainComp, SWT.NONE);

		label.setText(Messages.CloudApplicationUrlPart_TEXT_DEPLOYURL_LABEL);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.CENTER).applyTo(label);

		fullURLText = new Text(subDomainComp, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fullURLText);

		fullURLText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent arg0) {
				currentUrl = fullURLText.getText();
				validate(null, fullURLText);
			}
		});

		return subDomainComp;

	}
	
	public String getCurrentDomain() {
		if (isActive(domainCombo)) {
			int selectionIndex = domainCombo.getSelectionIndex();
			String[] domains = domainCombo.getItems();
			if (selectionIndex >= 0 && selectionIndex < domains.length) {
				return domains[selectionIndex];
			}
		}
		return null;
	}

	public void refreshDomains() {
		if (isActive(domainCombo)) {
			String existingSelection = getCurrentDomain();
			List<String> domains = getDomains();
			domainCombo.setItems(domains.toArray(new String[0]));
			updateDomainSelection(existingSelection);
		}
	}

	public void setUrl(String url) {
		// Update the current URL, whether valid or not
		currentUrl = url;

		setTextValue(fullURLText, currentUrl);
	}

	public void setSubdomain(String subdomain) {
		setTextValue(subDomainText, subdomain);
	}

	/**
	 * Sets value in the given text control if the control is active , and there
	 * is a change in the text control value. A null value will be set as an
	 * empty String in the control (this serves as a way to "clear" the
	 * control).
	 * @param textControl where text needs to be set
	 * @param value. If null, empty string will be set to clear the control
	 */
	protected void setTextValue(Text textControl, String value) {
		if (value == null) {
			value = ""; //$NON-NLS-1$
		}
		// Only set value if change occurred to avoid unnecessary validation
		if (isActive(textControl) && !textControl.getText().equals(value)) {
			// Setting value will notify Control listener which then
			// triggers validation
			textControl.setText(value);
		}
	}

	protected void resolveUrlFromSubdomain(Control source) {
		String subdomain = subDomainText.getText();
		String domain = getCurrentDomain();

		CloudApplicationURL suggestedUrl = new CloudApplicationURL(subdomain, domain);

		validate(suggestedUrl, source);
	}

	protected boolean isActive(Control control) {
		return control != null && !control.isDisposed();
	}

	/**
	 * Validate a given application URL. If no application URL is specified, a
	 * raw URL will be validated instead, if available.
	 * 
	 * <p/>
	 * If the URL is valid, meaning it has valid subdomain and domain segments:
	 * 
	 * <p/>
	 * 1. UI controls will be updated ONLY if there have been changes to the
	 * control values
	 * <p/>
	 * 2. If there are changes to values, or error occurred during validation,
	 * an event will be fired to notify any registered listeners
	 * <p/>
	 * @param appUrl
	 * @param status
	 */
	protected void validate(CloudApplicationURL appUrl, Control source) {
		// Validation has already been requested by a control, therefore don't
		// start another
		// one to avoid recursive validations
		if (this.validationSource != null) {
			return;
		}

		this.validationSource = source;

		// If no Application URL is given with subdomain and domain values set,
		// attempt to parse the existing
		// current URL as it may have changed manually
		IStatus status = Status.OK_STATUS;
		if (appUrl == null) {

			if (currentUrl != null) {
				try {
					appUrl = lookupService.getCloudApplicationURL(currentUrl);
				}
				catch (CoreException ce) {
					status = ce.getStatus();
				}
			}
		}
		else {
			try {
				appUrl = lookupService.validateCloudApplicationUrl(appUrl);
			}
			catch (CoreException ce) {
				status = ce.getStatus();
			}
		}

		// Update the current URL regardless of whether the Application URL is
		// valid or not, to make sure UI controls are up-to-date
		if (appUrl != null) {
			currentUrl = appUrl.getUrl();
		}

		if (this.validationSource != fullURLText) {
			setTextValue(fullURLText, currentUrl);
		}

		if (this.validationSource != domainCombo) {
			String domain = appUrl != null ? appUrl.getDomain() : null;
			updateDomainSelection(domain);
		}

		if (this.validationSource != subDomainText) {
			String subDomain = appUrl != null ? appUrl.getSubdomain() : null;
			setTextValue(subDomainText, subDomain);
		}

		validationSource = null;

		notifyChange(new PartChangeEvent(currentUrl, status, CloudUIEvent.APPLICATION_URL_CHANGED));
	}

	/**
	 * 
	 * @return non-null list of Domains.
	 */
	protected List<String> getDomains() {
		List<String> domains = new ArrayList<String>();
		List<CloudDomain> cloudDomains = lookupService.getDomains();
		if (cloudDomains != null) {
			for (CloudDomain cldm : cloudDomains) {
				domains.add(cldm.getName());
			}
		}
		return domains;
	}

	protected void updateDomainSelection(String domain) {

		if (isActive(domainCombo)) {

			// If no domain is to be set, or it no longer exists in list of
			// domains (possibly because list of domains has been changed),
			// select
			// a default one
			if (getSelectionIndex(domain) < 0 && getCurrentDomain() == null) {
				List<String> domains = getDomains();

				if (!domains.isEmpty()) {
					domain = domains.get(0);
				}
			}

			if (domain != null) {
				int selectionIndex = getSelectionIndex(domain);

				if (selectionIndex > -1) {
					domainCombo.select(selectionIndex);
				}
			}
		}
	}

	protected int getSelectionIndex(String domain) {
		int selectionIndex = -1;
		String[] domains = domainCombo.getItems();
		if (domains != null) {
			for (int i = 0; i < domains.length; i++) {
				if (domains[i].equals(domain)) {
					selectionIndex = i;
					break;
				}
			}
		}
		return selectionIndex;
	}
}
