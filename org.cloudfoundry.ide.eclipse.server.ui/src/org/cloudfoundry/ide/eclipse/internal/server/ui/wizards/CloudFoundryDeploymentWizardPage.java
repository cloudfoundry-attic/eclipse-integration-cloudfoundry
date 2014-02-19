/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationUrlLookupService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationURL;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.Messages;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudApplicationUrlPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.UIPart;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.wst.server.core.IModule;

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

	private ApplicationWizardDelegate wizardDelegate;

	static final int APP_NAME_CHANGE_EVENT = 10;

	static final int APP_NAME_INIT = 100;

	private static final String DEFAULT_MEMORY = CloudUtil.DEFAULT_MEMORY + "";

	public CloudFoundryDeploymentWizardPage(CloudFoundryServer server, CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor, ApplicationUrlLookupService urlLookup,
			ApplicationWizardDelegate wizardDelegate) {
		super("deployment", null, null, urlLookup);
		this.server = server;
		this.module = module;
		this.descriptor = descriptor;
		this.serverTypeId = module.getServerTypeId();
		this.wizardDelegate = wizardDelegate;
	}

	/**
	 * Perform some action like refreshing values in the UI. This is only called
	 * after the page is visible.
	 */
	protected void performWhenPageVisible() {

		refreshMemoryOptions();
		// Only refresh Domains once.
		if (!refreshedDomains) {
			refreshApplicationUrlDomains();
		}
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
		if (memory > 0) {
			descriptor.getDeploymentInfo().setMemory(memory);
			update(true, Status.OK_STATUS);
		}
		else {
			update(true, CloudFoundryPlugin.getErrorStatus(Messages.ERROR_INVALID_MEMORY));
		}
	}

	protected void setDeploymentMode(ApplicationAction deploymentMode) {
		descriptor.getDeploymentInfo().setDeploymentMode(deploymentMode);
	}

	public void createControl(Composite parent) {
		setTitle("Launch deployment");
		setDescription("Specify the deployment details");
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

		// Do not validate values yet. When controls are populated, they will
		// fire validation events accordingly
		update(false, Status.OK_STATUS);
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
		urlPart = createUrlPart(getApplicationUrlLookup());
		urlPart.createPart(parent);
		urlPart.addPartChangeListener(this);
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

		String startLabelText = "Start application on deployment";

		regularStartOnDeploymentButton = new Button(parent, SWT.CHECK);
		regularStartOnDeploymentButton.setText(startLabelText);
		ApplicationAction deploymentMode = descriptor.getDeploymentInfo().getDeploymentMode();

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

				deploymentMode = start ? ApplicationAction.START : null;

				setDeploymentMode(deploymentMode);

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
		return CloudFoundryProperties.isDebugEnabled.testProperty(new IModule[] { module }, server);
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
	 * org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.PartsWizardPage
	 * #handleChange
	 * (org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent)
	 */
	public void handleChange(PartChangeEvent event) {
		String value = event.getData() instanceof String ? (String) event.getData() : null;

		// If the event originated from the URL UI, just update the URL in the
		// descriptor. No other UI needs to be updated.
		if (event.getSource() == urlPart) {
			setUrlInDescriptor(value);
		}
		// If the app name changed, then update both the descriptor and the UI
		else if (event.getType() == APP_NAME_CHANGE_EVENT || event.getType() == APP_NAME_INIT) {
			updateApplicationNameInDescriptor(value);

			// If the list of domains has been refreshed, update the URL in the
			// UI right away. Otherwise
			// wait for the refresh to finish and invoke the call back that then
			// updates the UI (see the postDomainRefreshOperation callback).

			if (event.getType() == APP_NAME_CHANGE_EVENT) {
				updateDescriptorURLwithAppName(value);
			}
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

	protected void updateDescriptorURLwithAppName(String appName) {
		// When the app name changes, the URL also changes, but only for
		// application types that require a URL. By default, it
		// is assumed that the app needs a URL, unless otherwise specified by
		// the app's delegate
		if (wizardDelegate == null || wizardDelegate.getApplicationDelegate() == null
				|| wizardDelegate.getApplicationDelegate().requiresURL()) {

			String url = null;
			if (appName != null) {
				// First see if there is a selected Domain.

				String selectedDomain = urlPart != null ? urlPart.getCurrentDomain() : null;

				if (selectedDomain == null) {
					// use a default URL
					CloudApplicationURL appURL = null;
					try {
						appURL = getApplicationUrlLookup().getDefaultApplicationURL(appName);
					}
					catch (CoreException e) {
						// Do not disable the wizard. Users can still enter a
						// domain manually.
						update(false,
								CloudFoundryPlugin.getStatus(
										NLS.bind(
												"Unable to resolve a domain due to {0} - Enter a domain manually to continue deploying the application",
												e.getMessage()), IStatus.WARNING));
					}

					if (appURL != null) {
						url = appURL.getUrl();
					}
				}

				// If url was not yet resolved, manually construct it
				if (url == null && selectedDomain != null) {
					url = appName + '.' + selectedDomain;
				}
			}

			setUrlInDescriptor(url);
			if (urlPart != null) {
				urlPart.updateFullUrl(url);
			}
		}
	}

	class MemoryPart extends UIPart {

		protected Text memory;

		@Override
		public Control createPart(Composite parent) {
			Label label = new Label(parent, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			label.setText(org.cloudfoundry.ide.eclipse.internal.server.ui.Messages.LABEL_MEMORY_LIMIT);

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
					memory.setText(currentMemory + "");
				}
			}

		}
	}

	@Override
	protected void postDomainsRefreshedOperation() {
		urlPart.refreshDomains();
		if (urlPart == null) {
			return;
		}

		// If the app already has a URL, use that in the part. Otherwise, set a
		// url based on the app's deployment name
		List<String> urls = descriptor.getDeploymentInfo().getUris();
		String url = urls != null && !urls.isEmpty() ? urls.get(0) : null;

		if (url != null) {
			urlPart.updateFullUrl(url);
		}
		else {
			String appName = descriptor.getDeploymentInfo().getDeploymentName();

			if (appName != null) {
				urlPart.updateUrlSubdomain(appName);
			}
		}

	}

}
