/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationAction;
import org.cloudfoundry.ide.eclipse.server.core.internal.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ApplicationRegistry;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.debug.DebugProvider;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudApplicationUrlPart;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.IEventSource;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.server.ui.internal.UIPart;
import org.cloudfoundry.ide.eclipse.server.ui.internal.WizardPartChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Nieraj Singh
 */
public class CloudFoundryDeploymentWizardPage extends AbstractURLWizardPage {

	protected final String serverTypeId;

	protected final CloudFoundryServer server;

	protected Composite runDebugOptions;

	protected Button regularStartOnDeploymentButton;

	protected CloudFoundryApplicationWizard wizard;

	protected final CloudFoundryApplicationModule module;

	protected final ApplicationWizardDescriptor descriptor;

	protected CloudApplicationUrlPart urlPart;

	private MemoryPart memoryPart;

	private static final String DEFAULT_MEMORY = CloudUtil.DEFAULT_MEMORY + ""; //$NON-NLS-1$

	private ApplicationWizardDelegate wizardDelegate;

	public CloudFoundryDeploymentWizardPage(CloudFoundryServer server, CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor, ApplicationUrlLookupService urlLookup,
			ApplicationWizardDelegate wizardDelegate) {
		super(Messages.CloudFoundryDeploymentWizardPage_TEXT_DEPLOYMENT, null, null);
		this.server = server;
		this.module = module;
		this.descriptor = descriptor;
		this.serverTypeId = module.getServerTypeId();
		// Create the part before area is created as it be invoked by the page's
		// event handler before the page is visible.
		urlPart = createUrlPart(urlLookup);
		urlPart.addPartChangeListener(this);
		this.wizardDelegate = wizardDelegate;
	}

	/**
	 * Perform some action like refreshing values in the UI. This is only called
	 * after the page is visible.
	 */
	protected void performWhenPageVisible() {

		refreshMemoryOptions();
	}

	protected void refreshMemoryOptions() {
		memoryPart.refreshMemoryOptions();
	}

	protected Point getRunDebugControlIndentation() {
		return new Point(15, 5);
	}

	protected void setMemory(String memoryVal) {

		int memory = -1;
		try {
			memory = Integer.parseInt(memoryVal);
		}
		catch (NumberFormatException e) {
			// ignore. error is handled below
		}
		IStatus status = Status.OK_STATUS;
		if (memory > 0) {
			descriptor.getDeploymentInfo().setMemory(memory);
		}
		else {
			// Set an invalid memory so next time page opens, it restores a
			// valid value
			descriptor.getDeploymentInfo().setMemory(-1);
			status = CloudFoundryPlugin.getErrorStatus(Messages.ERROR_INVALID_MEMORY);
		}
		handleChange(new PartChangeEvent(memoryVal, status, CloudUIEvent.MEMORY));
	}

	public void createControl(Composite parent) {
		setTitle(Messages.CloudFoundryDeploymentWizardPage_TITLE_LAUNCH_DEPLOY);
		setDescription(Messages.CloudFoundryDeploymentWizardPage_TEXT_DEPLOY_DETAIL);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
		if (banner != null) {
			setImageDescriptor(banner);
		}

		this.wizard = (CloudFoundryApplicationWizard) getWizard();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createAreas(composite);

		setControl(composite);
	}

	protected void createAreas(Composite parent) {

		Composite topComposite = new Composite(parent, SWT.NONE);
		GridLayout topLayout = new GridLayout(2, false);
		topComposite.setLayout(topLayout);
		topComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createURLArea(topComposite);

		createMemoryArea(topComposite);

		createStartOrDebugOptions(parent);
	}

	protected void createURLArea(Composite parent) {
		urlPart.createPart(parent);
		urlPart.refreshDomains();
		updateApplicationURL();
	}

	protected CloudApplicationUrlPart createUrlPart(ApplicationUrlLookupService urlLookup) {
		return new CloudApplicationUrlPart(urlLookup);
	}

	protected void createMemoryArea(Composite parent) {
		memoryPart = new MemoryPart();
		memoryPart.addPartChangeListener(this);
		memoryPart.createPart(parent);
	}

	protected void createStartOrDebugOptions(Composite parent) {

		String startLabelText = Messages.CloudFoundryDeploymentWizardPage_LABEL_START_APP;

		regularStartOnDeploymentButton = new Button(parent, SWT.CHECK);
		regularStartOnDeploymentButton.setText(startLabelText);
		ApplicationAction deploymentMode = descriptor.getApplicationStartMode();

		regularStartOnDeploymentButton.setSelection(deploymentMode == ApplicationAction.START);

		GridData buttonData = new GridData(SWT.FILL, SWT.FILL, false, false);

		if (!isServerDebugModeAllowed()) {
			buttonData.horizontalSpan = 2;
			buttonData.verticalIndent = 10;
		}

		regularStartOnDeploymentButton.setLayoutData(buttonData);

		regularStartOnDeploymentButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				boolean start = regularStartOnDeploymentButton.getSelection();
				ApplicationAction deploymentMode = null;

				// TODO: Uncomment when debug support is available once again
				// (post CF
				// 1.5.0)
				// if (isServerDebugModeAllowed()) {
				// // delegate to the run or debug controls to decide which
				// // mode to select
				// makeStartDeploymentControlsVisible(start);
				// if (!start) {
				// deploymentMode = null;
				// }
				// }
				// else {
				// deploymentMode = start ? ApplicationAction.START : null;
				// }

				deploymentMode = start ? ApplicationAction.START : ApplicationAction.STOP;

				descriptor.setApplicationStartMode(deploymentMode);
			}
		});
		// TODO: Uncomment when debug support is available once again (post CF
		// 1.5.0)
		// if (isServerDebugModeAllowed()) {
		// runDebugOptions = new Composite(parent, SWT.NONE);
		//
		// GridLayoutFactory.fillDefaults().margins(getRunDebugControlIndentation()).numColumns(1)
		// .applyTo(runDebugOptions);
		// GridDataFactory.fillDefaults().grab(false,
		// false).applyTo(runDebugOptions);
		//
		// final Button runRadioButton = new Button(runDebugOptions, SWT.RADIO);
		// runRadioButton.setText("Run");
		// runRadioButton.setToolTipText("Run application after deployment");
		// runRadioButton.setSelection(deploymentMode ==
		// ApplicationAction.START);
		//
		// runRadioButton.addSelectionListener(new SelectionAdapter() {
		//
		// public void widgetSelected(SelectionEvent e) {
		// setDeploymentMode(ApplicationAction.START);
		// }
		// });
		//
		// final Button debugRadioButton = new Button(runDebugOptions,
		// SWT.RADIO);
		// debugRadioButton.setText("Debug");
		// debugRadioButton.setToolTipText("Debug application after deployment");
		// debugRadioButton.setSelection(deploymentMode ==
		// ApplicationAction.DEBUG);
		//
		// debugRadioButton.addSelectionListener(new SelectionAdapter() {
		//
		// public void widgetSelected(SelectionEvent e) {
		// setDeploymentMode(ApplicationAction.DEBUG);
		// }
		// });
		//
		// // Hide run or debug selection controls if there is no server
		// // support
		// makeStartDeploymentControlsVisible(true);
		// }

	}

	protected boolean isServerDebugModeAllowed() {
		return DebugProvider.getCurrent(module, server).isCloudSpaceDebugEnabled();
	}

	protected void makeStartDeploymentControlsVisible(boolean makeVisible) {
		if (runDebugOptions != null && !runDebugOptions.isDisposed()) {
			GridData data = (GridData) runDebugOptions.getLayoutData();

			// If hiding, exclude from layout as to not take up space when it is
			// made invisible
			GridDataFactory.createFrom(data).exclude(!makeVisible).applyTo(runDebugOptions);

			runDebugOptions.setVisible(makeVisible);

			// Recalculate layout if run debug options are excluded
			runDebugOptions.getParent().layout(true, true);

		}
	}

	/**
	 * Sets the application URL in the deployment descriptor
	 */
	protected void setUrlInDescriptor(String url) {
		if (url != null) {
			List<String> urls = new ArrayList<String>();
			urls.add(url);
			descriptor.getDeploymentInfo().setUris(urls);
		}
		else {
			descriptor.getDeploymentInfo().setUris(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudfoundry.ide.eclipse.server.ui.internal.wizards.PartsWizardPage
	 * #handleChange
	 * (org.cloudfoundry.ide.eclipse.server.ui.internal.PartChangeEvent)
	 */
	public void handleChange(PartChangeEvent event) {
		Object eventData = event.getData();
		IEventSource<?> source = event.getSource();

		// If the event originated from the URL UI, just update the URL in
		// the
		// descriptor. No other UI needs to be updated.
		if (event.getSource() == CloudUIEvent.APPLICATION_URL_CHANGED) {
			String urlVal = eventData instanceof String ? (String) eventData : null;
			setUrlInDescriptor(urlVal);

			IStatus status = event.getStatus();
			// Don't show the error if the application does not require a URL
			// and the URL is empty
			if (ValueValidationUtil.isEmpty(urlVal) && !requiresUrl()) {
				status = Status.OK_STATUS;
			}
			event = new WizardPartChangeEvent(eventData, status, event.getSource(), true);

		}
		else if (source == CloudUIEvent.APP_NAME_CHANGE_EVENT) {
			String value = (String) event.getData();
			updateApplicationNameInDescriptor(value);
			// Set the application URL based on the app name.
			updateApplicationURLFromAppName();
		}

		super.handleChange(event);
	}

	protected void updateApplicationNameInDescriptor(String appName) {

		// Do not set empty Strings
		if (ValueValidationUtil.isEmpty(appName)) {
			appName = null;
		}

		descriptor.getDeploymentInfo().setDeploymentName(appName);
	}

	protected void updateApplicationURL() {

		List<String> urls = descriptor.getDeploymentInfo().getUris();
		String url = urls != null && !urls.isEmpty() ? urls.get(0) : null;

		// Existing URLs have higher priority than URLs generated from the
		// application name
		if (url != null) {
			urlPart.setUrl(url);
		}
		else {
			updateApplicationURLFromAppName();
		}
	}

	protected void updateApplicationURLFromAppName() {
		if (shouldSetDefaultUrl()) {
			// When the app name changes, the URL also changes, but only for
			// application types that require a URL.
			String appName = descriptor.getDeploymentInfo().getDeploymentName();

			urlPart.setSubdomain(appName);
		}
	}

	protected boolean requiresUrl() {
		// By default, applications require a URL, unless specified by the
		// delegate
		return wizardDelegate == null || wizardDelegate.getApplicationDelegate() == null
				|| wizardDelegate.getApplicationDelegate().requiresURL();

	}

	protected boolean shouldSetDefaultUrl() {
		return wizardDelegate == null
				|| ApplicationRegistry.shouldSetDefaultUrl(wizardDelegate.getApplicationDelegate(), module);
	}

	class MemoryPart extends UIPart {

		protected Text memory;

		@Override
		public Control createPart(Composite parent) {
			Label label = new Label(parent, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			label.setText(Messages.LABEL_MEMORY_LIMIT);

			memory = new Text(parent, SWT.BORDER);
			memory.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			memory.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					setMemory(memory.getText());
				}
			});
			return parent;
		}

		public void refreshMemoryOptions() {
			if (memory != null && !memory.isDisposed()) {
				int currentMemory = descriptor.getDeploymentInfo().getMemory();
				if (currentMemory <= 0) {
					memory.setText(DEFAULT_MEMORY);
				}
				else {
					memory.setText(currentMemory + ""); //$NON-NLS-1$
				}
			}
		}
	}

	@Override
	protected void domainsRefreshed() {
		urlPart.refreshDomains();
		updateApplicationURL();
	}

}
