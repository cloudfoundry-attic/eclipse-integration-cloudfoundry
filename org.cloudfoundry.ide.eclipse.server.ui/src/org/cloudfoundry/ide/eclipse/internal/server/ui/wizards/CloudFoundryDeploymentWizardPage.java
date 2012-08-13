/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.DeploymentInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentConfiguration;
import org.cloudfoundry.ide.eclipse.internal.server.core.DeploymentInfoValidator;
import org.cloudfoundry.ide.eclipse.internal.server.core.URLNameValidation;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.standalone.StandaloneStartCommandPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.standalone.StartCommandPartFactory.IStartCommandChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.standalone.StartCommandPartFactory.StartCommandEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
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
public class CloudFoundryDeploymentWizardPage extends WizardPage implements IStartCommandChangeListener {

	private boolean canFinish;

	private final String serverTypeId;

	private Text urlText;

	protected final String deploymentName;

	private int memory;

	private final CloudFoundryServer server;

	protected DeploymentConfiguration deploymentConfiguration;

	private Combo memoryCombo;

	private Composite runDebugOptions;

	private Button regularStartOnDeploymentButton;

	private final CloudFoundryApplicationWizard wizard;

	private final ApplicationModule module;

	private String deploymentUrl;

	private ApplicationAction deploymentMode;

	private StandaloneStartCommandPart standalonePart;

	public CloudFoundryDeploymentWizardPage(CloudFoundryServer server, ApplicationModule module,
			CloudFoundryApplicationWizard wizard) {
		super("deployment");
		this.server = server;
		this.module = module;
		this.wizard = wizard;
		this.serverTypeId = module.getServerTypeId();

		DeploymentInfo deploymentInfo = null;
		if (module != null) {
			deploymentInfo = module.getLastDeploymentInfo();
		}

		if (deploymentInfo != null) {
			this.deploymentName = deploymentInfo.getDeploymentName();
		}
		else {
			this.deploymentName = getDeploymentNameFromModule(module);
		}

		this.memory = CloudUtil.DEFAULT_MEMORY;

		// Default should be to start in regular mode upon deployment
		deploymentMode = ApplicationAction.START;
	}

	private String getDeploymentNameFromModule(ApplicationModule module) {
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
			for (int option : deploymentConfiguration.getMemoryOptions()) {
				memoryCombo.add(option + "M");
				if (option == deploymentConfiguration.getDefaultMemory()) {
					int index = memoryCombo.getItemCount() - 1;
					memoryCombo.setItem(index, option + "M (Default)");
					memoryCombo.select(index);
					memory = option;
				}
			}
			if (memory == 0 && deploymentConfiguration.getMemoryOptions().length > 0) {
				memoryCombo.select(0);
				memory = deploymentConfiguration.getMemoryOptions()[0];
			}
			memoryCombo.setEnabled(true);
		}
	}

	protected Point getRunDebugControlIndentation() {
		return new Point(15, 5);
	}

	protected boolean updateConfiguration() {
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						deploymentConfiguration = server.getBehaviour().getDeploymentConfiguration(
								CloudUtil.DEFAULT_FRAMEWORK, monitor);
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

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite topComposite = new Composite(composite, SWT.NONE);
		GridLayout topLayout = new GridLayout(2, false);
		topComposite.setLayout(topLayout);
		topComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label label = new Label(topComposite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		label.setText("Deployed URL:");
		label.setFocus();

		urlText = new Text(topComposite, SWT.BORDER);
		urlText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		urlText.setEditable(true);
		updateUrl();

		urlText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				deploymentUrl = urlText.getText();
				update();
			}
		});

		label = new Label(topComposite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		label.setText("Memory Reservation:");

		memoryCombo = new Combo(topComposite, SWT.BORDER | SWT.READ_ONLY);
		memoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		memoryCombo.setEnabled(false);
		memoryCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// should always parse correctly
				int selectionIndex = memoryCombo.getSelectionIndex();
				if (selectionIndex != -1) {
					memory = deploymentConfiguration.getMemoryOptions()[selectionIndex];
				}
			}
		});

		if (wizard.isStandaloneApplication()) {
			IProject project = module.getProject();
			if (project == null) {
				project = module.getLocalModule().getProject();
			}
			standalonePart = new StandaloneStartCommandPart(wizard.getStandaloneHandler().getStartCommand(), this,
					project);
			standalonePart.createPart(topComposite);
		}

		createStartOrDebugOptions(composite);

		setControl(composite);

		update(false);
	}

	public String getStandaloneStartCommand() {
		if (standalonePart != null) {
			return standalonePart.getStandaloneStartCommand();
		}
		return null;
	}

	protected void createStartOrDebugOptions(Composite parent) {
		regularStartOnDeploymentButton = new Button(parent, SWT.CHECK);
		regularStartOnDeploymentButton.setText("Start application on deployment");
		regularStartOnDeploymentButton.setSelection(deploymentMode == ApplicationAction.START);
		GridData buttonData = new GridData(SWT.FILL, SWT.FILL, true, false);
		buttonData.horizontalSpan = 2;
		buttonData.verticalIndent = 10;
		regularStartOnDeploymentButton.setLayoutData(buttonData);

		runDebugOptions = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(getRunDebugControlIndentation()).numColumns(1)
				.applyTo(runDebugOptions);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(runDebugOptions);

		final Button runRadioButton = new Button(runDebugOptions, SWT.RADIO);
		runRadioButton.setText("Run");
		runRadioButton.setToolTipText("Run application after deployment");
		runRadioButton.setSelection(deploymentMode == ApplicationAction.START);

		regularStartOnDeploymentButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				boolean start = regularStartOnDeploymentButton.getSelection();
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

			}
		});

		runRadioButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				deploymentMode = ApplicationAction.START;

			}
		});

		final Button debugRadioButton = new Button(runDebugOptions, SWT.RADIO);
		debugRadioButton.setText("Debug");
		debugRadioButton.setToolTipText("Debug application after deployment");
		debugRadioButton.setSelection(deploymentMode == ApplicationAction.DEBUG);

		debugRadioButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				deploymentMode = ApplicationAction.DEBUG;

			}
		});

		// Hide run or debug selection controls if there is no server support
		if (!isServerDebugModeAllowed() || deploymentMode == null) {
			makeStartDeploymentControlsVisible(false);

		}

	}

	protected boolean isServerDebugModeAllowed() {
		return CloudFoundryProperties.isDebugEnabled.testProperty(new IModule[] { module }, server);
	}

	protected void makeStartDeploymentControlsVisible(boolean makeVisible) {
		GridData data = (GridData) runDebugOptions.getLayoutData();

		// If hiding, exclude from layout as to not take up space when it is
		// made invisible
		GridDataFactory.createFrom(data).exclude(!makeVisible).applyTo(runDebugOptions);

		runDebugOptions.setVisible(makeVisible);

		// Recalculate layout if run debug options are excluded
		runDebugOptions.getParent().layout(true, true);
	}

	public DeploymentInfo getDeploymentInfo() {
		DeploymentInfo info = new DeploymentInfo();
		info.setDeploymentName(deploymentName);
		info.setMemory(memory);
		List<String> uris = new ArrayList<String>();

		// Be sure not to add an empty URL if it is a standalone application
		if (!wizard.isStandaloneApplication() || !URLNameValidation.isEmpty(deploymentUrl)) {
			uris.add(deploymentUrl);
		}

		info.setUris(uris);

		return info;
	}

	public ApplicationAction getDeploymentMode() {
		return deploymentMode;
	}

	@Override
	public boolean isPageComplete() {
		return canFinish;
	}

	private void update() {
		update(true);
	}

	private void update(boolean updateButtons) {
		canFinish = true;

		DeploymentInfoValidator validator = new DeploymentInfoValidator(urlText.getText(), getStandaloneStartCommand(),
				wizard.isStandaloneApplication());

		IStatus status = validator.isValid();
		canFinish = status.getSeverity() == IStatus.OK;

		if (canFinish) {
			if (wizard.isStandaloneApplication() && standalonePart != null) {
				canFinish = standalonePart.isStartCommandValid();
			}
			if (!canFinish) {
				setErrorMessage("Invalid start command entered.");
			}
			else {
				setErrorMessage(null);
			}
		}
		else {
			setErrorMessage(status.getMessage() != null ? status.getMessage() : "Invalid value entered.");
		}

		if (updateButtons) {
			getWizard().getContainer().updateButtons();
		}
	}

	public void updateUrl() {
		String appName = wizard.getApplicationInfo().getAppName();
		if (appName != null) {
			deploymentUrl = module.getLaunchUrl(appName);
		}
		else {
			deploymentUrl = module.getDefaultLaunchUrl();
		}

		if (urlText != null) {
			urlText.setText(deploymentUrl);
		}
	}

	public void handleEvent(StartCommandEvent event) {
		if (event.equals(StartCommandEvent.UPDATE)) {
			update(true);
		}
	}

}
