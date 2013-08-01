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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConfiguration;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentInfoValidator;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IModule;

/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Nieraj Singh
 */
@SuppressWarnings("restriction")
public class CloudFoundryDeploymentWizardPage extends WizardPage {

	protected boolean canFinish;

	protected final String serverTypeId;

	protected Text urlText;

	protected final CloudFoundryServer server;

	protected DeploymentConfiguration deploymentConfiguration;

	protected Combo memoryCombo;

	protected Composite runDebugOptions;

	protected Button regularStartOnDeploymentButton;

	protected CloudFoundryApplicationWizard wizard;

	protected final CloudFoundryApplicationModule module;

	protected final ApplicationWizardDescriptor descriptor;

	public CloudFoundryDeploymentWizardPage(CloudFoundryServer server, CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor) {
		super("deployment");
		this.server = server;
		this.module = module;
		this.descriptor = descriptor;
		this.serverTypeId = module.getServerTypeId();

		initDeploymentInfoInDescriptor();

	}

	protected void initDeploymentInfoInDescriptor() {
		// Create a new deployment info, and populate it from existing or
		// default values, as well as user input
		DeploymentInfo info = new DeploymentInfo();
		descriptor.setDeploymentInfo(info);

		DeploymentInfo lastInfo = (module != null) ? module.getLastDeploymentInfo() : null;

		String deploymentName = (lastInfo != null) ? lastInfo.getDeploymentName() : getDeploymentNameFromModule(module);

		info.setDeploymentName(deploymentName);

		int memory = CloudUtil.DEFAULT_MEMORY;
		info.setMemory(memory);

		// Default should be to start in regular mode upon deployment
		ApplicationAction deploymentMode = ApplicationAction.START;

		setDeploymentMode(deploymentMode);
	}

	private String getDeploymentNameFromModule(CloudFoundryApplicationModule module) {
		if (module != null) {
			CloudApplication app = module.getApplication();
			if (app != null && app.getName() != null) {
				return app.getName();
			}
			return module.getName();
		}
		return "";
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && deploymentConfiguration == null) {
			if (getPreviousPage() == null) {
				// delay until dialog is actually visible
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						if (!getControl().isDisposed()) {
							refresh();
						}
					}
				});
			}
			else {
				refresh();
			}
		}
	}

	protected void refresh() {
		if (updateConfiguration()) {
			memoryCombo.removeAll();
			int memory = 0;
			// Select the default memory first
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
		}
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

	protected void setURL() {
		String url = urlText != null && !urlText.isDisposed() ? urlText.getText() : null;

		if (url != null) {
			List<String> urls = new ArrayList<String>();
			urls.add(url);
			descriptor.getDeploymentInfo().setUris(urls);
		}
	}

	protected boolean updateConfiguration() {
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						deploymentConfiguration = server.getBehaviour().getDeploymentConfiguration(monitor);
					}
					catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
					catch (OperationCanceledException e) {
						throw new InterruptedException();
					}
					finally {
						monitor.done();
					}
				}
			});
			return true;
		}
		catch (InvocationTargetException e) {
			IStatus status = server
					.error(NLS.bind("Configuration retrieval failed: {0}", e.getCause().getMessage()), e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
			setMessage(status.getMessage(), IMessageProvider.ERROR);
		}
		catch (InterruptedException e) {
			// ignore
		}
		return false;
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

		update(false);
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
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		label.setText("Deployed URL:");
		label.setFocus();

		urlText = new Text(parent, SWT.BORDER);
		urlText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		urlText.setEditable(true);
		updateUrl();

		urlText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setURL();
				update();
			}
		});
	}

	protected void createMemoryArea(Composite parent) {
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
				if (isServerDebugModeAllowed()) {
					// delegate to the run or debug controls to decide which
					// mode to select
					makeStartDeploymentControlsVisible(start);
					if (!start) {
						deploymentMode = null;
					}
				}
				else {
					deploymentMode = start ? ApplicationAction.START : null;
				}
				setDeploymentMode(deploymentMode);

			}
		});

		if (isServerDebugModeAllowed()) {
			runDebugOptions = new Composite(parent, SWT.NONE);

			GridLayoutFactory.fillDefaults().margins(getRunDebugControlIndentation()).numColumns(1)
					.applyTo(runDebugOptions);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(runDebugOptions);

			final Button runRadioButton = new Button(runDebugOptions, SWT.RADIO);
			runRadioButton.setText("Run");
			runRadioButton.setToolTipText("Run application after deployment");
			runRadioButton.setSelection(deploymentMode == ApplicationAction.START);

			runRadioButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					setDeploymentMode(ApplicationAction.START);
				}
			});

			final Button debugRadioButton = new Button(runDebugOptions, SWT.RADIO);
			debugRadioButton.setText("Debug");
			debugRadioButton.setToolTipText("Debug application after deployment");
			debugRadioButton.setSelection(deploymentMode == ApplicationAction.DEBUG);

			debugRadioButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					setDeploymentMode(ApplicationAction.DEBUG);
				}
			});

			// Hide run or debug selection controls if there is no server
			// support
			makeStartDeploymentControlsVisible(true);
		}

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

	@Override
	public boolean isPageComplete() {
		return canFinish;
	}

	private void update() {
		update(true);
	}

	protected void update(boolean updateButtons) {
		canFinish = true;

		DeploymentInfoValidator validator = new DeploymentInfoValidator(urlText.getText(), null, false);

		IStatus status = validator.isValid();
		canFinish = status.getSeverity() == IStatus.OK;

		if (canFinish) {
			setErrorMessage(null);
		}
		else {
			setErrorMessage(status.getMessage() != null ? status.getMessage() : "Invalid value entered.");
		}

		if (updateButtons) {
			getWizard().getContainer().updateButtons();
		}
	}

	public void updateUrl() {

		ApplicationInfo appInfo = descriptor.getApplicationInfo();
		if (appInfo != null) {
			String appName = appInfo.getAppName();

			try {
				String deploymentUrl = (appName != null) ? module.getLaunchUrl(appName) : module.getDefaultLaunchUrl();

				if (urlText != null) {
					urlText.setText(deploymentUrl);
					setURL();
				}
			}
			catch (CoreException ce) {
				String errorMessage = ce.getMessage() != null ? ce.getMessage()
						: "Unable to resolve an application launch URL.";
				errorMessage += " Please manually enter a URL.";
				setMessage(errorMessage, DialogPage.WARNING);
			}
		}

	}
}
