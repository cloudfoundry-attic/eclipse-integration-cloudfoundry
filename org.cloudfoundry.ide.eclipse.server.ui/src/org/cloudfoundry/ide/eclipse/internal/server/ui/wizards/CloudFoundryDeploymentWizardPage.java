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

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationURL;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationUrlLookup;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConfiguration;
import org.cloudfoundry.ide.eclipse.internal.server.core.ValueValidationUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudHostDomainUrlPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ICoreRunnable;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.UIPart;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.progress.UIJob;
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

	protected DeploymentConfiguration deploymentConfiguration;

	protected Combo memoryCombo;

	protected Composite runDebugOptions;

	protected Button regularStartOnDeploymentButton;

	protected CloudFoundryApplicationWizard wizard;

	protected final CloudFoundryApplicationModule module;

	protected final ApplicationWizardDescriptor descriptor;

	private CloudHostDomainUrlPart urlPart;

	private MemoryPart memoryPart;

	private ApplicationWizardDelegate wizardDelegate;

	static final int APP_NAME_CHANGE_EVENT = 10;

	public CloudFoundryDeploymentWizardPage(CloudFoundryServer server, CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor, CloudApplicationUrlLookup urlLookup,
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

		fetchDeploymentConfiguration();
		// Only refresh Domains once.
		if (!refreshedDomains) {
			refreshApplicationUrlDomains();
		}
	}

	protected void fetchDeploymentConfiguration() {
		// Only get a deployment if it doesn't yet exist.
		if (deploymentConfiguration != null) {
			return;
		}
		final String jobLabel = "Fetching list of memory options.";
		UIJob job = new UIJob(jobLabel) {

			@Override
			public IStatus runInUIThread(IProgressMonitor arg0) {
				try {
					CloudUiUtil.runForked(new ICoreRunnable() {
						public void run(IProgressMonitor monitor) throws CoreException {

							SubMonitor subProgress = SubMonitor.convert(monitor, jobLabel, 100);

							try {
								deploymentConfiguration = server.getBehaviour().getDeploymentConfiguration(subProgress);
							}

							catch (OperationCanceledException e) {
								throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
							}
							finally {
								subProgress.done();
							}

						}
					}, getWizard().getContainer());
					memoryPart.refreshMemoryOptions();

				}

				catch (CoreException ce) {
					update(true, ce.getStatus());
				}
				return Status.OK_STATUS;
			}

		};
		job.setSystem(true);
		job.schedule();

	}

	protected Point getRunDebugControlIndentation() {
		return new Point(15, 5);
	}

	protected void setMemory(int memory) {
		descriptor.getDeploymentInfo().setMemory(memory);
	}

	protected void setDeploymentMode(ApplicationAction deploymentMode) {
		descriptor.setStartDeploymentMode(deploymentMode);
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

	protected CloudHostDomainUrlPart createUrlPart(CloudApplicationUrlLookup urlLookup) {
		return new CloudHostDomainUrlPart(urlLookup);
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
		ApplicationAction deploymentMode = descriptor.getStartDeploymentMode();

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
	 * Update the application URL in case there have been changes to the
	 * application name, as the application name is used as the URL's host
	 * segment.
	 */
	public void updateUrlInUI() {

		if (urlPart == null) {
			return;
		}

		ApplicationInfo appInfo = descriptor.getApplicationInfo();
		if (appInfo != null) {
			String appName = appInfo.getAppName();

			if (appName != null) {
				urlPart.updateUrlHost(appName);
			}
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
		else if (event.getType() == APP_NAME_CHANGE_EVENT) {
			updateApplicationNameInDescriptor(value);

			// If the list of domains has been refreshed, update the URL in the
			// UI right away. Otherwise
			// wait for the refresh to finish and invoke the call back that then
			// updates the UI (see the postDomainRefreshOperation callback).
			if (refreshedDomains) {
				updateUrlInUI();
			}
		}

		super.handleChange(event);
	}

	protected void updateApplicationNameInDescriptor(String appName) {

		// Do not set empty Strings
		if (ValueValidationUtil.isEmpty(appName)) {
			appName = null;
		}

		if (appName != null) {
			ApplicationInfo appInfo = new ApplicationInfo(appName);
			descriptor.setApplicationInfo(appInfo);
		}
		else {
			descriptor.setApplicationInfo(null);
		}

		DeploymentInfo depInfo = descriptor.getDeploymentInfo();
		if (depInfo == null) {
			depInfo = new DeploymentInfo();
			descriptor.setDeploymentInfo(depInfo);
		}

		depInfo.setDeploymentName(appName);

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
					CloudApplicationURL appURL = getApplicationUrlLookup().getDefaultApplicationURL(appName);

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
		}
	}

	class MemoryPart extends UIPart {
		@Override
		public Control createPart(Composite parent) {
			Label label = new Label(parent, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			label.setText("Memory Reservation:");

			memoryCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
			memoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			memoryCombo.setEnabled(false);
			memoryCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					// should always parse correctly
					int selectionIndex = memoryCombo.getSelectionIndex();
					if (selectionIndex != -1) {
						int memory = deploymentConfiguration.getMemoryOptions()[selectionIndex];
						setMemory(memory);
					}
				}
			});
			return parent;
		}

		public void refreshMemoryOptions() {

			// Select the default memory first
			if (deploymentConfiguration == null || deploymentConfiguration.getMemoryOptions() == null
					|| deploymentConfiguration.getMemoryOptions().length == 0) {
				notifyStatusChange(CloudFoundryPlugin
						.getErrorStatus("Unable to retrieve list of memory options from the server. Please check connection or account settings."));
			}
			else {
				memoryCombo.removeAll();
				int memory = 0;
				for (int option : deploymentConfiguration.getMemoryOptions()) {
					memoryCombo.add(option + "M");
					if (option == deploymentConfiguration.getDefaultMemory()) {
						int index = memoryCombo.getItemCount() - 1;
						memoryCombo.setItem(index, option + "M (Default)");
						memoryCombo.select(index);
						memory = option;
					}
				}
				// If no default memory is found, select the first memory option
				if (memory == 0 && deploymentConfiguration.getMemoryOptions().length > 0) {
					memoryCombo.select(0);
					memory = deploymentConfiguration.getMemoryOptions()[0];
				}
				memoryCombo.setEnabled(true);
				setMemory(memory);
				notifyStatusChange(Status.OK_STATUS);
			}
		}
	}

	@Override
	protected void postDomainsRefreshedOperation() {
		urlPart.refreshDomains();
		updateUrlInUI();
	}
}
