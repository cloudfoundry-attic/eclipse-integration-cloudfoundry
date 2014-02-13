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
package org.cloudfoundry.ide.eclipse.internal.server.ui.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudErrorUtil;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryBrandingExtensionPoint;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.ManifestParser;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.DeploymentInfoWorkingCopy;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.CloudFoundryProperties;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugCommand;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugCommandBuilder;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.DebugModeType;
import org.cloudfoundry.ide.eclipse.internal.server.core.debug.ICloudFoundryDebuggerListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudUiUtil;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.CloudFoundryEditorAction.RefreshArea;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.DebugApplicationEditorAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.RemoveServicesFromApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.ShowConsoleEditorAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.StartStopApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.UpdateApplicationMemoryAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.actions.UpdateInstanceCountAction;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.AppStatsContentProvider.InstanceStatsAndInfo;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ApplicationActionMenuControl.IButtonMenuListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.EnvVarsWizard;
import org.cloudfoundry.ide.eclipse.internal.server.ui.wizards.MappedURLsWizard;
import org.cloudfoundry.ide.eclipse.server.rse.ConfigureRemoteCloudFoundryAction;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.internal.ImageResource;

/**
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Nieraj Singh
 */
@SuppressWarnings("restriction")
public class ApplicationDetailsPart extends AbstractFormPart implements IDetailsPage {

	private boolean canUpdate;

	private final CloudFoundryServer cloudServer;

	private final CloudFoundryApplicationsEditorPage editorPage;

	private Section generalSection;

	private Section generalSectionRestartRequired;

	private Section operationsSection;

	private AppStatsContentProvider instancesContentProvider;

	private Spinner instanceSpinner;

	private Section instancesSection;

	private TableViewer instancesViewer;

	private Link mappedURIsLink;

	private IModule module;

	private ApplicationActionMenuControl restartAppButton;

	private ApplicationActionMenuControl updateRestartAppButton;

	private final CloudFoundryServerBehaviour serverBehaviour;

	private Text serverNameText;

	private TreeContentProvider servicesContentProvider;

	private ServicesViewerDropListener servicesDropListener;

	private Section servicesSection;

	private TableViewer servicesViewer;

	private Button startAppButton;

	private Button debugControl;

	private Button stopAppButton;

	private Button connectToDebugger;

	private Button saveManifest;

	private Combo memoryCombo;

	/**
	 * This must NOT be set directly. Use appropriate setter
	 */
	// private ApplicationAction currentStartDebugApplicationAction;

	private Composite buttonComposite;

	/**
	 * The toolkit used by the form part.
	 */
	private FormToolkit toolkit;

	private List<String> URIs;

	private final boolean provideServices;

	private int memory;

	private boolean isPublished = false;

	// Resize viewer tables on first refresh as to avoid extra space after the
	// last column
	private boolean initialTableResized = false;

	// Workaround as there is no restart state in the app server state,
	// and button refresh should not occur during restart mode
	protected boolean skipButtonRefreshOnRestart;

	public ApplicationDetailsPart(CloudFoundryApplicationsEditorPage editorPage, CloudFoundryServer cloudServer) {
		this.editorPage = editorPage;
		this.cloudServer = cloudServer;
		this.serverBehaviour = cloudServer.getBehaviour();
		this.provideServices = CloudFoundryBrandingExtensionPoint.getProvideServices(editorPage.getServer()
				.getServerType().getId());
	}

	public void createContents(Composite parent) {
		toolkit = getManagedForm().getToolkit();
		parent.setLayout(new GridLayout());

		createGeneralSection(parent);

		createGeneralSectionRestartRequired(parent);

		createApplicationOperationsSection(parent);

		if (provideServices) {
			createServicesSection(parent);
			servicesDropListener = new ServicesViewerDropListener(servicesViewer, serverBehaviour, editorPage);
		}

		createInstancesSection(parent);

		if (provideServices) {
			addDropSupport(generalSection);
			addDropSupport(generalSectionRestartRequired);
			addDropSupport(operationsSection);
			addDropSupport(servicesSection);
			addDropSupport(instancesSection);
		}
	}

	protected void refreshDebugButtons(CloudFoundryApplicationModule appModule) {
		int state = appModule.getState();

		if (isDebugAllowed()) {
			if (state == IServer.STATE_STOPPED || state == IServer.STATE_UNKNOWN) {
				RowData data = new RowData();
				data.exclude = false;
				debugControl.setLayoutData(data);
				debugControl.setVisible(true);

				data = new RowData();
				data.exclude = true;
				connectToDebugger.setLayoutData(data);
				connectToDebugger.setVisible(false);
			}
			else {
				RowData data = new RowData();
				data.exclude = true;
				debugControl.setLayoutData(data);
				debugControl.setVisible(false);

				// Show the connect to debugger button if the application is
				// running
				// in debug mode but no debugger is connected
				DebugModeType modeType = getDeployedAppDebugMode();
				if (modeType != null
						&& !CloudFoundryProperties.isConnectedToDebugger.testProperty(new IModule[] { module },
								cloudServer)) {
					data = new RowData();
					data.exclude = false;
					connectToDebugger.setLayoutData(data);
					connectToDebugger.setVisible(true);
				}
				else {
					data = new RowData();
					data.exclude = true;
					connectToDebugger.setLayoutData(data);
					connectToDebugger.setVisible(false);
				}
			}
			buttonComposite.layout(true, true);
		}

	}

	/**
	 * 
	 * @param appModule should not be null.
	 */
	protected void refreshApplicationDeploymentButtons(CloudFoundryApplicationModule appModule) {
		// FIXNS: Not called for CF 1.6.0 as restart and update restart buttons
		// are not made visible even when set visible.
		// Possible issue could be that restart and update restart buttons have
		// their own parent composite since for debug, they change to dropdown
		// combos
		int state = appModule.getState();

		// Don't refresh if the restart buttons were selected
		if (skipButtonRefreshOnRestart) {
			skipButtonRefreshOnRestart = false;
			return;
		}

		// Show/hide action buttons based on server state
		if (state == IServer.STATE_STOPPED || state == IServer.STATE_UNKNOWN) {

			RowData data = new RowData();
			data.exclude = false;
			startAppButton.setLayoutData(data);
			startAppButton.setVisible(true);

			data = new RowData();
			data.exclude = true;
			restartAppButton.setCompositeLayoutData(data);
			restartAppButton.setVisible(false);

			data = new RowData();
			data.exclude = true;
			stopAppButton.setLayoutData(data);
			stopAppButton.setVisible(false);

		}
		else {

			RowData data = new RowData();
			data.exclude = true;
			startAppButton.setLayoutData(data);
			startAppButton.setVisible(false);

			data = new RowData();
			data.exclude = false;
			restartAppButton.setCompositeLayoutData(data);
			restartAppButton.setVisible(true);

			data = new RowData();
			data.exclude = false;
			stopAppButton.setLayoutData(data);
			stopAppButton.setVisible(true);
		}

		// handle the update and restart button
		// Do not show the update button if there is not accessible
		// module project in the workspace, as no source update would be
		// possible within Eclipse
		if (state == IServer.STATE_STOPPED
				|| state == IServer.STATE_UNKNOWN
				|| !CloudFoundryProperties.isModuleProjectAccessible
						.testProperty(new IModule[] { module }, cloudServer)) {
			RowData data = new RowData();
			data.exclude = true;
			updateRestartAppButton.setCompositeLayoutData(data);
			updateRestartAppButton.setVisible(false);
		}
		else {
			RowData data = new RowData();
			data.exclude = false;
			updateRestartAppButton.setCompositeLayoutData(data);
			updateRestartAppButton.setVisible(true);
		}

		refreshRestartButtons();

		// FIXNS: Enable when debug is supported in v2 in the future
		// refreshDebugButtons(appModule);

		buttonComposite.layout(true, true);
	}

	protected void refreshAndReenableDeploymentButtons(CloudFoundryApplicationModule appModule) {

		if (buttonComposite == null || buttonComposite.isDisposed()) {
			return;
		}
		int state = appModule.getState();

		// Don't refresh if the restart buttons were selected
		if (skipButtonRefreshOnRestart) {
			skipButtonRefreshOnRestart = false;
			return;
		}

		// Show/hide action buttons based on server state
		if (state == IServer.STATE_STOPPED || state == IServer.STATE_UNKNOWN) {

			startAppButton.setEnabled(true);

			restartAppButton.getSelectionButton().setEnabled(false);

			stopAppButton.setEnabled(false);
		}
		else {
			startAppButton.setEnabled(false);

			restartAppButton.getSelectionButton().setEnabled(true);

			stopAppButton.setEnabled(true);
		}

		// handle the update and restart button separately.
		// Do not show the update button if there is not accessible
		// module project in the workspace, as no source update would be
		// possible within Eclipse
		if (state == IServer.STATE_STOPPED
				|| state == IServer.STATE_UNKNOWN
				|| !CloudFoundryProperties.isModuleProjectAccessible
						.testProperty(new IModule[] { module }, cloudServer)) {
			updateRestartAppButton.getSelectionButton().setEnabled(false);

		}
		else {
			updateRestartAppButton.getSelectionButton().setEnabled(true);
		}

		refreshRestartButtons();

		// FIXNS: Enable when debug is supported in v2 in the future
		// refreshDebugButtons(appModule);
	}

	private void updateServerNameDisplay(CloudFoundryApplicationModule application) {
		if (application.getApplication() == null) {
			serverNameText.setText(NLS.bind("{0} [Not Deployed]", application.getDeployedApplicationName()));
			return;
		}
		int state = application.getState();
		String debugLabel = getDebugStartStopLabel();

		switch (state) {
		case IServer.STATE_STARTED:
			String message = debugLabel != null ? "{0}  [Started in " + debugLabel + "]" : "{0}  [Started]";
			serverNameText.setText(NLS.bind(message, application.getDeployedApplicationName()));
			break;
		case IServer.STATE_STOPPED:
			serverNameText.setText(NLS.bind("{0}  [Stopped]", application.getDeployedApplicationName()));
			break;
		default:
			serverNameText.setText(application.getDeployedApplicationName());
		}
	}

	protected boolean isDebugAllowed() {
		return CloudFoundryProperties.isDebugEnabled.testProperty(new IModule[] { module }, cloudServer);
	}

	protected void connectToDebugger() {
		DebugCommand command = new DebugCommandBuilder(new IModule[] { module }, cloudServer).getDebugCommand(
				ApplicationAction.CONNECT_TO_DEBUGGER, new ApplicationDetailsDebugListener());
		new DebugApplicationEditorAction(editorPage, command).run();
	}

	/**
	 * 
	 * @param appModule may be null. If null, publish state should indicate
	 * unknown state.
	 */
	protected void refreshPublishState(CloudFoundryApplicationModule appModule) {
		isPublished = appModule != null && appModule.getState() != IServer.STATE_UNKNOWN;
	}

	public void refreshUI() {
		logError(null);
		resizeTableColumns();

		canUpdate = false;
		CloudFoundryApplicationModule appModule = null;
		try {
			appModule = getUpdatedApplication();
		}
		catch (CoreException ce) {
			logApplicationModuleFailureError("Unable to refresh editor state");
		}
		// Refresh the state of the editor regardless of whether there is a
		// module or not
		refreshPublishState(appModule);

		if (appModule == null) {
			return;
		}

		if (saveManifest != null) {
			ManifestParser parser = new ManifestParser(appModule, cloudServer);
			if (!parser.canWriteToManifest()) {
				saveManifest.setEnabled(false);
				saveManifest
						.setToolTipText("No accessible application workspace project available. Manifest file cannot be saved or created at this time.");
			}
			else {
				saveManifest.setEnabled(true);
				saveManifest
						.setToolTipText("Save application deployment properties into the manifest file. Existing manifest file will be merged with updated values.");
			}
		}

		// The rest of the refresh requires appModule to be non-null
		updateServerNameDisplay(appModule);

		int state = appModule.getState();

		setCurrentStartDebugApplicationAction();
		instanceSpinner.setSelection(appModule.getInstanceCount());

		refreshAndReenableDeploymentButtons(appModule);
		// refreshApplicationDeploymentButtons(appModule);

		mappedURIsLink.setEnabled(state == IServer.STATE_STARTED);

		CloudApplication cloudApplication = appModule.getApplication();

		instanceSpinner.setEnabled(cloudApplication != null);
		instancesViewer.getTable().setEnabled(cloudApplication != null);

		instancesViewer.setInput(null);

		memoryCombo.setEnabled(cloudApplication != null);
		if (cloudApplication != null) {
			int appMemory = appModule.getApplication().getMemory();

			int[] applicationMemoryChoices = editorPage.getApplicationMemoryChoices();
			if (applicationMemoryChoices != null && applicationMemoryChoices.length > 0) {
				boolean found = false;

				// Only clear memory combo if a new list is obtained. Otherwise
				// the memory combo shrinks
				memoryCombo.removeAll();
				for (int option : applicationMemoryChoices) {
					memoryCombo.add(option + "M");
					if (option == appMemory) {
						int index = memoryCombo.getItemCount() - 1;
						memoryCombo.select(index);
						found = true;
					}
				}

				if (!found && appMemory != 0) {
					memoryCombo.add(appMemory + "M", 0);
					memoryCombo.select(0);
				}
				memoryCombo.setEnabled(true);
				memoryCombo.redraw();

			}
		}

		List<String> currentURIs = null;
		if (cloudApplication != null) {
			currentURIs = cloudApplication.getUris();

			ApplicationStats applicationStats = appModule.getApplicationStats();
			InstancesInfo instancesInfo = appModule.getInstancesInfo();
			if (applicationStats != null) {
				List<InstanceStats> statss = applicationStats.getRecords();
				List<InstanceInfo> infos = instancesInfo != null ? instancesInfo.getInstances() : null;
				InstanceStatsAndInfo[] statsAndInfos = new InstanceStatsAndInfo[statss.size()];

				for (int i = 0; i < statss.size(); i++) {
					InstanceStats stats = statss.get(i);
					InstanceInfo info = null;
					if (infos != null && infos.size() > i) {
						info = infos.get(i);
					}

					statsAndInfos[i] = new InstanceStatsAndInfo(stats, info);
				}
				instancesViewer.setInput(statsAndInfos);
			}
		}

		if (currentURIs == null && !isPublished) {
			// At this stage, the app may not have deployed due to errors, but
			// there may already
			// be set URIs in an existing info
			currentURIs = appModule.getDeploymentInfo() != null ? appModule.getDeploymentInfo().getUris() : null;
		}

		if (currentURIs == null) {
			currentURIs = Collections.emptyList();
		}

		if (!currentURIs.equals(URIs)) {
			URIs = currentURIs;
			mappedURIsLink.setText(getURIsAsLinkText(URIs));
			generalSection.getParent().layout(true, true);
			editorPage.reflow();
		}

		refreshServices(appModule);
		instancesViewer.refresh(true);

		canUpdate = true;

		if (appModule.getErrorMessage() != null) {
			editorPage.setMessage(appModule.getErrorMessage(), IMessageProvider.ERROR);
		}
		else {
			editorPage.setMessage(null, IMessageProvider.ERROR);
		}
	}

	private void refreshServices(final CloudFoundryApplicationModule appModule) {
		if (provideServices) {
			UIJob uiJob = new UIJob("Refreshing Services") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					// servicesViewer.getTable().setEnabled(cloudApplication !=
					// null);

					// Update the mapping of bound services in the application
					List<CloudService> updatedServices = new ArrayList<CloudService>();

					DeploymentInfoWorkingCopy deploymentInfo = null;
					List<String> serviceNames = null;
					try {
						deploymentInfo = appModule.getDeploymentInfoWorkingCopy(monitor);

						serviceNames = deploymentInfo.asServiceBindingList();
					}
					catch (CoreException e) {
						logError(NLS
								.bind("Failed to resolve bound services in {0} from the application's deployment descriptor. The displayed services may not be correct.",
										appModule.getDeployedApplicationName()));
					}

					if (serviceNames == null) {
						serviceNames = Collections.emptyList();
					}

					List<CloudService> allServices = editorPage.getServices();

					// Only show bound services that actually exist
					if (allServices != null && !serviceNames.isEmpty()) {
						for (CloudService service : allServices) {
							if (serviceNames.contains(service.getName())) {
								updatedServices.add(service);
							}
						}

						// Update the bound services mapping in the application
						if (!updatedServices.isEmpty() && deploymentInfo != null) {
							deploymentInfo.setServices(updatedServices);
							deploymentInfo.save();
						}
					}
					servicesViewer.setInput(updatedServices.toArray(new CloudService[updatedServices.size()]));

					servicesDropListener.setModule(appModule);
					servicesViewer.refresh(true);
					return Status.OK_STATUS;

				}
			};
			uiJob.setSystem(true);
			uiJob.setPriority(Job.INTERACTIVE);
			uiJob.schedule();

		}
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection sel = (IStructuredSelection) selection;
		module = (IModule) sel.getFirstElement();

		refreshUI();
		editorPage.refresh(RefreshArea.DETAIL, true);
	}

	private void adaptControl(Control control) {
		control.setBackground(toolkit.getColors().getBackground());
		control.setForeground(toolkit.getColors().getForeground());
	}

	private void addDropSupport(Section section) {
		int ops = DND.DROP_COPY | DND.DROP_LINK | DND.DROP_DEFAULT;
		Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };

		DropTarget dropTarget = new DropTarget(section, ops);
		dropTarget.setTransfer(transfers);
		dropTarget.addDropListener(servicesDropListener);
	}

	protected void resizeTableColumns() {
		if (initialTableResized) {
			return;
		}

		List<TableViewer> tableViewers = new ArrayList<TableViewer>();

		if (servicesViewer != null) {
			tableViewers.add(servicesViewer);
		}

		if (instancesViewer != null) {
			tableViewers.add(instancesViewer);
		}

		for (TableViewer tableViewer : tableViewers) {
			Table table = tableViewer.getTable();
			Composite tableComposite = table.getParent();
			Rectangle tableCompositeArea = tableComposite.getClientArea();
			int tableWidth = tableCompositeArea.width;
			TableColumn[] tableColumns = table.getColumns();

			if (tableColumns.length == 0) {
				continue;
			}

			int totalColumnWidths = 0;

			// resize only if there is empty space at the end of the table
			for (TableColumn column : tableColumns) {
				totalColumnWidths += column.getWidth();
			}

			if (totalColumnWidths < tableWidth) {

				// If a successful resize, do not attempt to resize on
				// subsequent
				// refreshes.
				initialTableResized = true;

				// resize the last one column such that the last column width
				// takes up all the empty space
				TableColumn lastColumn = tableColumns[tableColumns.length - 1];
				int newWidth = (tableWidth - totalColumnWidths) + lastColumn.getWidth();
				lastColumn.setWidth(newWidth);
			}
		}
	}

	private void createGeneralSection(Composite parent) {
		generalSection = toolkit.createSection(parent, Section.TITLE_BAR);
		generalSection.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(generalSection);
		generalSection.setText("General");

		// reset spacing due to toolbar
		generalSection.clientVerticalSpacing = 0;

		Composite client = toolkit.createComposite(generalSection);
		client.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(client);
		generalSection.setClient(client);

		createLabel(client, "Name:", SWT.CENTER);
		serverNameText = createText(client, SWT.NONE);

		createLabel(client, "Mapped URLs:", SWT.TOP);

		Composite uriComposite = toolkit.createComposite(client);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).applyTo(uriComposite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(uriComposite);

		ImageHyperlink editURI = toolkit.createImageHyperlink(uriComposite, SWT.PUSH);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.TOP).applyTo(editURI);
		editURI.setImage(CloudFoundryImages.getImage(CloudFoundryImages.EDIT));
		editURI.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {

				try {
					CloudFoundryApplicationModule appModule = getExistingApplication();

					MappedURLsWizard wizard = new MappedURLsWizard(cloudServer, appModule, URIs, isPublished);
					WizardDialog dialog = new WizardDialog(editorPage.getEditorSite().getShell(), wizard);
					if (dialog.open() == Window.OK) {

						CloudApplication application = appModule.getApplication();
						if (application != null) {
							URIs = wizard.getURLs();
							mappedURIsLink.setText(getURIsAsLinkText(wizard.getURLs()));
							generalSection.getParent().layout(true, true);
							editorPage.reflow();
							application.setUris(URIs);
						}
					}
				}
				catch (CoreException ce) {
					logApplicationModuleFailureError("Unable to open Mapped URL wizard");
				}

			}
		});

		mappedURIsLink = new Link(uriComposite, SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, false).hint(250, SWT.DEFAULT).applyTo(mappedURIsLink);
		adaptControl(mappedURIsLink);

		mappedURIsLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CloudUiUtil.openUrl("http://" + e.text);
			}
		});

		createLabel(client, "Instances:", SWT.CENTER);

		instanceSpinner = new Spinner(client, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(instanceSpinner);
		instanceSpinner.setMinimum(0);
		instanceSpinner.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				if (canUpdate) {
					try {
						CloudFoundryApplicationModule appModule = getExistingApplication();
						new UpdateInstanceCountAction(editorPage, instanceSpinner, appModule).run();
					}
					catch (CoreException ce) {
						logApplicationModuleFailureError("Unable to update application instances");
					}
				}
			}
		});
		toolkit.adapt(instanceSpinner);

		// Manifest area
		createLabel(client, "Manifest:", SWT.CENTER);
		saveManifest = toolkit.createButton(client, "Save", SWT.PUSH);

		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(saveManifest);

		saveManifest.setEnabled(false);
		saveManifest.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				writeToManifest();
			}
		});

	}

	private void createGeneralSectionRestartRequired(Composite parent) {
		generalSectionRestartRequired = toolkit.createSection(parent, Section.TITLE_BAR);
		generalSectionRestartRequired.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(generalSectionRestartRequired);
		generalSectionRestartRequired.setText("General (Application Restart Required)");

		// reset spacing due to toolbar
		generalSectionRestartRequired.clientVerticalSpacing = 0;

		Composite client = toolkit.createComposite(generalSectionRestartRequired);
		client.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(client);
		generalSectionRestartRequired.setClient(client);

		createLabel(client, "Memory limit:", SWT.CENTER);

		memoryCombo = new Combo(client, SWT.BORDER);

		// Set minimum so combo doesn't shrink on refresh.
		int comboMinimumWidth = 70;
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.FILL).hint(comboMinimumWidth, SWT.DEFAULT)
				.applyTo(memoryCombo);
		memoryCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (canUpdate) {
					int selectionIndex = memoryCombo.getSelectionIndex();
					if (selectionIndex != -1) {
						memory = editorPage.getApplicationMemoryChoices()[selectionIndex];

						try {
							CloudFoundryApplicationModule appModule = getExistingApplication();
							new UpdateApplicationMemoryAction(editorPage, memory, appModule).run();
						}
						catch (CoreException ce) {
							logApplicationModuleFailureError("Unable to update application memory");
						}
					}
				}
			}

		});

		createLabel(client, "Environment Variables:", SWT.CENTER);
		Button envVarsButton = toolkit.createButton(client, "Edit...", SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(envVarsButton);

		envVarsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					final CloudFoundryApplicationModule appModule = getExistingApplication();
					if (appModule != null) {
						UIJob uiJob = new UIJob("Edit enivornment variables") {

							public IStatus runInUIThread(IProgressMonitor monitor) {
								try {
									DeploymentInfoWorkingCopy infoWorkingCopy = appModule
											.getDeploymentInfoWorkingCopy(monitor);

									EnvVarsWizard wizard = new EnvVarsWizard(cloudServer, appModule, infoWorkingCopy);
									WizardDialog dialog = new WizardDialog(editorPage.getEditorSite().getShell(),
											wizard);
									dialog.open();
									return Status.OK_STATUS;
								}
								catch (CoreException e) {
									return e.getStatus();
								}
							}

						};
						uiJob.setSystem(true);
						uiJob.setPriority(Job.INTERACTIVE);
						uiJob.schedule();

					}
				}
				catch (CoreException ce) {
					logError("Error while updating environment variables due to: " + ce.getMessage());
				}
			}
		});
	}

	private void createApplicationOperationsSection(Composite parent) {

		operationsSection = toolkit.createSection(parent, Section.TITLE_BAR);
		operationsSection.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(operationsSection);
		operationsSection.setText("Application Operations");

		// reset spacing due to toolbar
		operationsSection.clientVerticalSpacing = 0;

		Composite client = toolkit.createComposite(operationsSection);
		client.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(client);
		operationsSection.setClient(client);

		// FIXNS: Uncomment when CF client supports staging updates
		// createStandaloneCommandArea(client);

		buttonComposite = toolkit.createComposite(client);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(buttonComposite);

		RowLayout layout = RowLayoutFactory.fillDefaults().margins(0, 5).wrap(false).create();
		layout.center = true;
		buttonComposite.setLayout(layout);

		startAppButton = toolkit.createButton(buttonComposite, "Start", SWT.PUSH);
		startAppButton.setImage(ImageResource.getImage(ImageResource.IMG_CLCL_START));
		startAppButton.setEnabled(true);
		startAppButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				startStopApplication(ApplicationAction.START);
			}
		});

		stopAppButton = toolkit.createButton(buttonComposite, "Stop", SWT.PUSH);
		stopAppButton.setImage(ImageResource.getImage(ImageResource.IMG_CLCL_STOP));
		stopAppButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				startStopApplication(ApplicationAction.STOP);
			}
		});

		// Do not show drop down options for restart if debug support is not
		// allowed
		ApplicationAction[] restartActions = isDebugAllowed() ? new ApplicationAction[] { ApplicationAction.START,
				ApplicationAction.DEBUG } : null;

		restartAppButton = new ApplicationActionMenuControl(buttonComposite, restartActions, ApplicationAction.START,
				"Restart", CloudFoundryImages.getImage(CloudFoundryImages.RESTART), toolkit) {

			public void setDefaultTooltipMessage() {
				// Don't do anything as tooltip is controlled by the editor part
			}

		};
		restartAppButton.createControl();

		restartAppButton.addMenuListener(new IButtonMenuListener() {

			public void widgetSelected(ApplicationAction actionType) {
				restartApplication(ApplicationAction.RESTART, actionType);
			}
		});

		updateRestartAppButton = new ApplicationActionMenuControl(buttonComposite, restartActions,
				ApplicationAction.START, "Update and Restart", CloudFoundryImages.getImage(CloudFoundryImages.RESTART),
				toolkit) {

			public void setDefaultTooltipMessage() {
				// Don't do anything as tooltip is controlled by the editor part
			}

		};
		updateRestartAppButton.createControl();

		updateRestartAppButton.addMenuListener(new IButtonMenuListener() {

			public void widgetSelected(ApplicationAction actionType) {
				restartApplication(ApplicationAction.UPDATE_RESTART, actionType);
			}
		});

		// FIXNS: Disabled until debug support is present in v2
		// createDebugArea(buttonComposite);

	}

	protected void writeToManifest() {
		final IStatus[] errorStatus = new IStatus[1];

		try {
			final CloudFoundryApplicationModule appModule = getExistingApplication();
			if (appModule != null && saveManifest != null && !saveManifest.isDisposed()) {
				if (MessageDialog
						.openConfirm(saveManifest.getShell(), "Save to Manifest File",
								"Existing manifest file will be merged with new application deployment values. Are you sure you want to continue?")) {
					Job job = new Job("Writing manifest file for: " + appModule.getDeployedApplicationName()) {

						@Override
						protected IStatus run(IProgressMonitor monitor) {

							try {
								// Update the bound service mappings so they
								// point
								// to
								// the updated services
								serverBehaviour.refreshApplicationBoundServices(appModule, monitor);

								ManifestParser parser = new ManifestParser(appModule, cloudServer);
								parser.write(monitor, null);
							}
							catch (CoreException ce) {
								errorStatus[0] = ce.getStatus();
								return errorStatus[0];
							}
							return Status.OK_STATUS;
						}
					};
					job.schedule();
				}
			}
			else {
				errorStatus[0] = CloudFoundryPlugin
						.getErrorStatus("Unable to write manifest file. Missing module for cloud application. Try a manual refresh.");
			}
		}
		catch (CoreException ce) {
			errorStatus[0] = ce.getStatus();
		}

		if (errorStatus[0] != null && !errorStatus[0].isOK()) {
			logError("Unable to write to manifest due to: " + errorStatus[0].getMessage());
		}

	}

	protected void logApplicationModuleFailureError(String issue) {
		if (issue == null) {
			issue = "Unknown cause";
		}
		String errorMessage = "Failed to resolve a cloud application module - " + issue
				+ " - Try to manually refresh the editor.";
		logError(errorMessage);
	}

	protected void createDebugArea(Composite parent) {
		debugControl = toolkit.createButton(parent, "Debug", SWT.PUSH);
		debugControl.setImage(CloudFoundryImages.getImage(CloudFoundryImages.DEBUG));
		debugControl.setEnabled(true);
		debugControl.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				debugApplication(ApplicationAction.DEBUG);
			}
		});

		// Do not show Debug control if server does not support debug
		debugControl.setVisible(isDebugAllowed());

		connectToDebugger = toolkit.createButton(parent, "Connect to Debugger", SWT.PUSH);
		connectToDebugger.setImage(CloudFoundryImages.getImage(CloudFoundryImages.DEBUG));
		connectToDebugger.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				connectToDebugger();
			}
		});

		// If debugging is not supported, permanently hide the debug buttons
		if (!isDebugAllowed()) {
			RowData data = new RowData();
			data.exclude = true;
			debugControl.setLayoutData(data);
			debugControl.setVisible(false);

			data = new RowData();
			data.exclude = true;
			connectToDebugger.setLayoutData(data);
			connectToDebugger.setVisible(false);

			buttonComposite.layout(true, true);
		}
	}

	/**
	 * This should be the ONLY way to set the selected deploy application
	 * action.
	 * @param action
	 */
	protected void setCurrentStartDebugApplicationAction() {
		ApplicationAction currentDeployedAction = getCurrentDeploymentStateApplicationAction();
		if (restartAppButton != null) {
			restartAppButton.setSelectedAction(currentDeployedAction);
		}
		if (updateRestartAppButton != null) {
			updateRestartAppButton.setSelectedAction(currentDeployedAction);
		}
	}

	/**
	 * An application will be deployed in one of three modes, START, DEBUG
	 * SUSPEND, DEBUG NO SUSPEND.
	 * @return
	 */
	protected ApplicationAction getCurrentDeploymentStateApplicationAction() {
		DebugModeType type = getDeployedAppDebugMode();
		if (type == null) {
			return ApplicationAction.START;
		}
		else {
			return type.getApplicationAction();
		}
	}

	/**
	 * Restarts an application either in normal run mode or debug mode, based on
	 * the specified start action. The restart action is the actual restart
	 * command that was selected by the user, either "restart" or
	 * "update and restart"
	 * @param restartAction the actual button action that was selected by a user
	 */
	protected void restartApplication(ApplicationAction restartAction, ApplicationAction startAction) {
		skipButtonRefreshOnRestart = true;
		// Record the start action so that a user can invoke it again by simply
		// pressing the restart button directly

		switch (startAction) {
		case START:
			startStopApplication(restartAction);
			break;
		case DEBUG:
			debugApplication(restartAction);
			break;
		}

	}

	protected DebugModeType getDeployedAppDebugMode() {
		if (serverBehaviour == null || module == null) {
			return null;
		}
		return serverBehaviour.getDebugModeType(module, new NullProgressMonitor());

	}

	protected String getDebugStartStopLabel() {
		DebugModeType type = getDeployedAppDebugMode();

		if (type != null) {
			return type.getApplicationAction().getDisplayName().toLowerCase() + " mode";
		}
		return null;
	}

	protected void refreshRestartButtons() {
		setRestartButtonDisplayProperties(restartAppButton.getSelectionButton(), ApplicationAction.RESTART);
		setRestartButtonDisplayProperties(updateRestartAppButton.getSelectionButton(), ApplicationAction.UPDATE_RESTART);
	}

	protected void setRestartButtonDisplayProperties(Button restartButton, ApplicationAction restartButtonAction) {
		ApplicationAction currentDeployedAction = getCurrentDeploymentStateApplicationAction();

		// Set the UI for the restart buttons, including tooltip text, based on
		// the currently deployed application action.
		switch (currentDeployedAction) {
		case START:
			restartButton.setImage(CloudFoundryImages.getImage(CloudFoundryImages.RESTART));
			restartButton
					.setToolTipText(restartButtonAction == ApplicationAction.UPDATE_RESTART ? "Update and restart application"
							: "Restart application");

			break;
		case DEBUG:
			restartButton
					.setToolTipText(restartButtonAction == ApplicationAction.UPDATE_RESTART ? "Update and restart in "
							+ currentDeployedAction.getDisplayName().toLowerCase() + " mode" : "Restart in "
							+ currentDeployedAction.getDisplayName().toLowerCase() + " mode");
			restartButton.setImage(CloudFoundryImages.getImage(CloudFoundryImages.RESTART_DEBUG_MODE));
			break;
		}
	}

	private void createInstancesSection(Composite parent) {
		instancesSection = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE);
		instancesSection.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(instancesSection);
		instancesSection.setText("Instances");
		instancesSection.setExpanded(true);

		Composite client = toolkit.createComposite(instancesSection);
		client.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().applyTo(client);
		instancesSection.setClient(client);

		Composite container = toolkit.createComposite(client);
		GridLayoutFactory.fillDefaults().applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

		String[] columnNames = new String[] { "ID", "Host", "CPU", "Memory", "Disk", "Uptime" };
		String[] columnTooltips = new String[] { "ID", "Host", "CPU (Cores)", "Memory (Limit)", "Disk (Limit)",
				"Uptime"

		};

		int[] columnWidths = new int[] { 25, 105, 60, 85, 85, 75 };
		// weights new int[] { 6, 21, 14, 19, 19, 21 };

		instancesViewer = createTableViewer(container, columnNames, columnTooltips, columnWidths);

		instancesContentProvider = new AppStatsContentProvider();
		instancesViewer.setContentProvider(instancesContentProvider);
		instancesViewer.setLabelProvider(new AppStatsLabelProvider());
		instancesViewer.setSorter(new CloudFoundryViewerSorter());

		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillInstancesContextMenu(manager);
			}
		});

		Menu menu = menuManager.createContextMenu(instancesViewer.getControl());
		instancesViewer.getControl().setMenu(menu);

		if (Platform.getBundle("org.eclipse.rse.ui") != null) {
			final ConfigureRemoteCloudFoundryAction configAction = new ConfigureRemoteCloudFoundryAction(cloudServer);
			Link configLink = new Link(client, SWT.NONE);
			configLink.setText("Show deployed files in <a>Remote Systems View</a>.");
			configLink.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					configAction.run();
				}
			});
		}
	}

	private Label createLabel(Composite parent, String value, int verticalAlign) {
		Label label = toolkit.createLabel(parent, value);
		GridDataFactory.fillDefaults().align(SWT.FILL, verticalAlign).applyTo(label);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		return label;
	}

	private void createServicesSection(Composite parent) {
		servicesSection = toolkit.createSection(parent, Section.TITLE_BAR);
		servicesSection.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(servicesSection);
		servicesSection.setText("Application Services");

		Composite client = toolkit.createComposite(servicesSection);
		client.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().applyTo(client);
		servicesSection.setClient(client);

		Composite container = toolkit.createComposite(client);
		GridLayoutFactory.fillDefaults().applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

		// String[] columnNames = new String[] { "Name", "Service", "Vendor",
		// "Version", "Tier" };
		String[] columnNames = {};
		int[] columnWidths = {};
		ServicesLabelProvider labelProvider = new ServicesLabelProvider();
		ApplicationInstanceServiceColumn[] columnDescriptor = labelProvider.getServiceViewColumn();

		if (columnDescriptor != null) {
			int length = columnDescriptor.length;
			columnNames = new String[length];
			columnWidths = new int[length];
			int i = 0;
			for (ApplicationInstanceServiceColumn descriptor : columnDescriptor) {
				if (i < length) {
					columnNames[i] = descriptor.name();
					columnWidths[i] = descriptor.getWidth();
					i++;
				}
			}
		}

		// weights = new int[] { 30, 16, 12, 28, 14 };
		servicesViewer = createTableViewer(container, columnNames, null, columnWidths);

		servicesContentProvider = new TreeContentProvider();
		servicesViewer.setContentProvider(servicesContentProvider);
		servicesViewer.setLabelProvider(labelProvider);
		servicesViewer.setSorter(new CloudFoundryViewerSorter());
		servicesViewer.setInput(new CloudService[0]);

		MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillServicesContextMenu(manager);
			}
		});

		Menu menu = menuManager.createContextMenu(servicesViewer.getControl());
		servicesViewer.getControl().setMenu(menu);
		// editorPage.getSite().registerContextMenu(ID_MENU_SERVICES,
		// menuManager, servicesViewer);

		servicesSection.setVisible(CloudFoundryBrandingExtensionPoint.getProvideServices(editorPage.getServer()
				.getServerType().getId()));
	}

	private TableViewer createTableViewer(Composite parent, String[] columnNames, String[] columnTooltips,
			int[] columnWeights) {

		// Composite container = toolkit.createComposite(parent);
		// GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		// TableColumnLayout layout = new TableColumnLayout();
		// container.setLayout(layout);

		Table table = toolkit.createTable(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		table.setHeaderVisible(true);

		for (int i = 0; i < columnNames.length; i++) {
			TableColumn col = new TableColumn(table, SWT.NONE);
			col.setWidth(columnWeights[i]);
			col.setText(columnNames[i]);
			if (columnTooltips == null) {
				col.setToolTipText(null);
			}
			else {
				col.setToolTipText(columnTooltips[i]);
			}
			// layout.setColumnData(col, new
			// ColumnWeightData(columnWeights[i]));
		}

		TableViewer tableViewer = new TableViewer(table);
		tableViewer.setColumnProperties(columnNames);
		return tableViewer;
	}

	private Text createText(Composite parent, int style) {
		Text text = new Text(parent, style);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(text);
		text.setEditable(false);
		adaptControl(text);
		return text;
	}

	private void fillServicesContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) servicesViewer.getSelection();
		if (selection.isEmpty())
			return;

		try {
			CloudFoundryApplicationModule appModule = getExistingApplication();
			manager.add(new RemoveServicesFromApplicationAction(selection, appModule, serverBehaviour, editorPage));
		}
		catch (CoreException ce) {
			logApplicationModuleFailureError("Unable to determine bound services context menu actions");
		}
	}

	private void fillInstancesContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) instancesViewer.getSelection();
		if (selection.isEmpty()) {
			return;
		}

		Object instanceObject = selection.getFirstElement();

		if (instanceObject instanceof InstanceStatsAndInfo) {

			InstanceStats stats = ((InstanceStatsAndInfo) instanceObject).getStats();

			if (stats != null) {
				try {
					CloudFoundryApplicationModule appModule = getExistingApplication();
					manager.add(new ShowConsoleEditorAction(cloudServer, appModule, Integer.parseInt(stats.getId())));
				}
				catch (CoreException ce) {
					logApplicationModuleFailureError("Unable to generate application instances context menu");
				}
				catch (NumberFormatException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * @return an existing cloud application module. Should not be null during
	 * the lifecycle of the editor.
	 * @throws CoreException if application module was not resolved.
	 */
	protected CloudFoundryApplicationModule getExistingApplication() throws CoreException {
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

		if (appModule == null) {
			String errorMessage = "No Cloud Foundry application module found"
					+ (module != null ? " for: " + module.getId() : "");
			throw CloudErrorUtil.toCoreException(errorMessage);
		}

		return appModule;
	}

	/**
	 * @return an updated cloud application module. If it did not previously
	 * exist, it will attempt to create it. Never null as requesting an app
	 * module during the lifecycle of the editor should always result in
	 * non-null app.
	 * @throws CoreException if application does not exist
	 */
	protected CloudFoundryApplicationModule getUpdatedApplication() throws CoreException {
		CloudFoundryApplicationModule appModule = cloudServer.getCloudModule(module);

		if (appModule == null) {
			String errorMessage = "No Cloud Foundry application module found"
					+ (module != null ? " for: " + module.getId() : "");
			throw CloudErrorUtil.toCoreException(errorMessage);
		}
		return appModule;
	}

	protected void logError(String message) {
		if (editorPage != null && !editorPage.isDisposed()) {
			if (message != null) {
				editorPage.setErrorMessage(message);

			}
			else {
				editorPage.setErrorMessage(null);
			}
		}
	}

	/**
	 * 
	 * @param mode debug mode in which to launch the application
	 * @param restartAction update restart or restart, if that is the currently
	 * selected action, or null otherwise
	 */
	protected void debugApplication(ApplicationAction restartAction) {
		DebugCommand command = new DebugCommandBuilder(new IModule[] { module }, cloudServer).getDebugCommand(
				restartAction, new ApplicationDetailsDebugListener());
		new DebugApplicationEditorAction(editorPage, command).run();
	}

	private void startStopApplication(ApplicationAction action) {
		try {
			CloudFoundryApplicationModule appModule = getExistingApplication();
			new StartStopApplicationAction(editorPage, action, appModule, serverBehaviour, module).run();
		}
		catch (CoreException ce) {
			logApplicationModuleFailureError("Unable to perform " + action.getDisplayName());
		}
	}

	private static String getURIsAsLinkText(List<String> uris) {
		StringBuilder result = new StringBuilder();
		for (String uri : uris) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("<a href=\"");
			result.append(uri);
			result.append("\">");
			result.append(uri);
			result.append("</a>");
		}

		return result.toString();
	}

	protected class ApplicationDetailsDebugListener implements ICloudFoundryDebuggerListener {

		public void handleDebuggerTermination() {

			try {
				final CloudFoundryApplicationModule appModule = getExistingApplication();

				UIJob job = new UIJob("Debug Termination Job") {

					public IStatus runInUIThread(IProgressMonitor arg0) {

						// refreshApplicationDeploymentButtons(appModule);
						refreshAndReenableDeploymentButtons(appModule);
						return Status.OK_STATUS;
					}

				};
				job.setSystem(true);
				job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
				job.setPriority(Job.INTERACTIVE);
				job.schedule();
			}
			catch (CoreException ce) {
				logApplicationModuleFailureError("Unable to refresh debug buttons");
			}
		}
	}
}
