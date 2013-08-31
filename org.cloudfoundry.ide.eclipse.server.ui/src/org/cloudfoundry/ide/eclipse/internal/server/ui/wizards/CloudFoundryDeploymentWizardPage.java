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

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudApplicationUrlLookup;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConfiguration;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudHostDomainUrlPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ICoreRunnable;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.UIPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDescriptor.DescriptorChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.ApplicationWizardDescriptor.DescriptorProperty;
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
public class CloudFoundryDeploymentWizardPage extends AbstractURLWizardPage implements DescriptorChangeListener {

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

	private boolean requiresUrlUpdate = false;

	public CloudFoundryDeploymentWizardPage(CloudFoundryServer server, CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor, CloudApplicationUrlLookup urlLookup) {
		super("deployment", null, null, urlLookup);
		this.server = server;
		this.module = module;
		this.descriptor = descriptor;
		this.serverTypeId = module.getServerTypeId();

		descriptor.addListener(this, DescriptorProperty.ApplicationInfo);

	}

	/**
	 * Perform some action like refreshing values in the UI. This is only called
	 * after the page is visible.
	 */
	protected void performWhenPageVisible() {

		fetchDeploymentConfiguration();
		// Note that there is a delay in refreshing list of domains. As a
		// consequence, the URL host cannot be updated right away. Allow the
		// callback that gets invoked post-domain Refresh to set the URL host.
		// Only set the URL host if the domains have been refreshed
		if (!refreshedDomains) {
			refreshApplicationUrlDomains();
		}
		else if (requiresUrlUpdate) {
			requiresUrlUpdate = false;
			updateUrlHost();
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

		// Do not validate values yet
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
	public void updateUrlHost() {

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
	protected void setURL(String url) {
		if (url != null) {
			List<String> urls = new ArrayList<String>();
			urls.add(url);
			descriptor.getDeploymentInfo().setUris(urls);
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
		if (event.getSource() == urlPart) {
			String url = event.getData() instanceof String ? (String) event.getData() : null;
			setURL(url);
		}

		super.handleChange(event);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.
	 * ApplicationWizardDescriptor
	 * .DescriptorChangeListener#valueChanged(java.lang.Object)
	 */
	public void valueChanged(Object value) {
		// Delay the URL update until the page is visible
		requiresUrlUpdate = true;
	}

	@Override
	protected void postDomainsRefreshedOperation() {
		urlPart.refreshDomains();
		updateUrlHost();
	}
}
