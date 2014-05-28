/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation. - initial implementation
 *******************************************************************************/

package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.TreeContentProvider;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellNavigationStrategy;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationListener;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerEditorDeactivationEvent;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class CloudFoundryServiceWizardPage2 extends WizardPage {

	CloudFoundryServer cloudServer;

	private TableViewer servicesViewer;

	CloudFoundryServiceWizardPage1 firstPage;

	CFWizServicePage2Validation validation;

	public CloudFoundryServiceWizardPage2(CloudFoundryServer cloudServer, CloudFoundryServiceWizardPage1 firstPage) {
		super(CloudFoundryServiceWizardPage2.class.getName());
		setTitle("Service Configuration");
		setDescription(CFWizServicePage2Validation.SPECIFY_NAME);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}

		this.cloudServer = cloudServer;
		this.firstPage = firstPage;
		this.validation = new CFWizServicePage2Validation(this);

		setPageComplete(false);
	}

	private void createColumns(TableViewer viewer) {
		TableViewerColumn serviceName = new TableViewerColumn(viewer, SWT.NONE);
		serviceName.getColumn().setWidth(200);
		serviceName.getColumn().setText("Service");

		TableViewerColumn name = new TableViewerColumn(viewer, SWT.NONE);
		name.getColumn().setWidth(200);
		name.getColumn().setText("Name");

		name.setEditingSupport(new CFWizNameEditingSupport(this, viewer));

		TableViewerColumn plan = new TableViewerColumn(viewer, SWT.NONE);
		plan.getColumn().setWidth(200);
		plan.getColumn().setText("Service Plan");
		plan.setEditingSupport(new CFWizCloudPlanEditingSupport(this, viewer));

	}

	public void createControl(Composite parent) {

		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}

		Composite tableArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);

		Composite toolBarArea = new Composite(tableArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(toolBarArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolBarArea);

		Label label = new Label(toolBarArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
		label.setText("List of services to be added:");

		final Table table = new Table(tableArea, SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		servicesViewer = new TableViewer(table);

		createColumns(servicesViewer);

		servicesViewer.setContentProvider(new TreeContentProvider());
		servicesViewer.setLabelProvider(new CFWizServicesTreeLabelProvider());

		setControl(tableArea);

		addTabTraversalToEditor();

		setBoundServiceSelectionInUI();

	}

	private void addTabTraversalToEditor() {
		CellNavigationStrategy strategy = new CellNavigationStrategy() {

			@Override
			public ViewerCell findSelectedCell(ColumnViewer viewer, ViewerCell currentSelectedCell, Event event) {
				ViewerCell cell = super.findSelectedCell(viewer, currentSelectedCell, event);
				if (cell != null) {
					TableColumn t = servicesViewer.getTable().getColumn(cell.getColumnIndex());
					servicesViewer.getTable().showColumn(t);
				}
				return cell;
			}
		};

		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(servicesViewer,
				new FocusCellOwnerDrawHighlighter(servicesViewer), strategy);

		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(servicesViewer) {

			protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {

				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION
						|| ((event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED))
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};

		TableViewerEditor.create(servicesViewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL
				| ColumnViewerEditor.TABBING_CYCLE_IN_ROW | ColumnViewerEditor.TABBING_VERTICAL
				| ColumnViewerEditor.KEYBOARD_ACTIVATION);

		servicesViewer.getColumnViewerEditor().addEditorActivationListener(new ColumnViewerEditorActivationListener() {

			@Override
			public void afterEditorActivated(ColumnViewerEditorActivationEvent event) {
				/** Ignore */
			}

			@Override
			public void afterEditorDeactivated(ColumnViewerEditorDeactivationEvent event) {
				/** Ignore */
			}

			@Override
			public void beforeEditorActivated(ColumnViewerEditorActivationEvent event) {
				ViewerCell cell = (ViewerCell) event.getSource();
				servicesViewer.getTable().showColumn(servicesViewer.getTable().getColumn(cell.getColumnIndex()));
			}

			@Override
			public void beforeEditorDeactivated(ColumnViewerEditorDeactivationEvent event) {
				/** Ignore */
			}

		});

	}

	protected void setBoundServiceSelectionInUI() {

		servicesViewer.setInput(firstPage.getSelectedList().toArray(new CFServiceWizUI[] {}));

		validatePageState();
	}

	public List<CFServiceWizUI> getProducts() {
		return firstPage.getSelectedList();
	}

	protected void validatePageState() {
		validation.updatePageState();
	}

}

class CFWizServicesTreeLabelProvider extends LabelProvider implements ITableLabelProvider {

	public CFWizServicesTreeLabelProvider() {
	}

	public Image getColumnImage(Object element, int columnIndex) {

		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		CFServiceWizUI service = (CFServiceWizUI) element;

		switch (columnIndex) {
		case 0:
			return "" + service.getName();
		case 1:
			return "" + service.getUserDefinedName();
		case 2:
			return "" + (service.getPlan() != null ? service.getPlan().getName() : "");
		default:
			return "";
		}
	}

}

class CFWizServicePage2Validation {

	CloudFoundryServiceWizardPage2 wizardPage;

	StringValidator stringValidator = new StringValidator();

//	public static final String DESCRIPTION_FINAL = "Finish to add the services.";

	public static final String SPECIFY_NAME = "Specify service names and select service plans for each of the services.";

	public static final String INVALID_SERVICE_NAME = "Service name contains invalid characters: ";

	public CFWizServicePage2Validation(CloudFoundryServiceWizardPage2 wizardPage) {
		this.wizardPage = wizardPage;
	}

	public void updatePageState() {

		boolean descriptionUpdated = false;

		List<CFServiceWizUI> list = wizardPage.getProducts();

		for (CFServiceWizUI s : list) {

			if (descriptionUpdated) {
				break;
			}

			String userDefName = s.getUserDefinedName();

			if (userDefName != null && userDefName.trim().length() > 0) {
				// name is > 0 length
				IStatus vs = stringValidator.validate(s.getUserDefinedName());
				if (!vs.isOK()) {
					wizardPage.setDescription(null);
					wizardPage.setErrorMessage(INVALID_SERVICE_NAME + " " + s.getUserDefinedName());
					descriptionUpdated = true;
				}

			}
			else {
				wizardPage.setDescription(SPECIFY_NAME);
				wizardPage.setErrorMessage(null);
				descriptionUpdated = true;
			}

		}

		if (!descriptionUpdated && list.size() > 0) {
			wizardPage.setErrorMessage(null);
			wizardPage.setDescription(SPECIFY_NAME);
			wizardPage.setPageComplete(true);
		}
		else {
			wizardPage.setPageComplete(false);
		}

	}

	protected static class StringValidator implements IValidator {
		protected static Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+");

		public IStatus validate(Object value) {
			if (value instanceof String) {
				if (((String) value).length() == 0) {
					return ValidationStatus.cancel("Enter a name");
				}
				Matcher matcher = VALID_CHARS.matcher((String) value);
				if (!matcher.matches()) {
					return ValidationStatus.error("The entered name contains invalid characters.");
				}
			}
			return Status.OK_STATUS;
		}

	}

}

class CFWizNameEditingSupport extends EditingSupport {

	private final TableViewer viewer;

	private final CellEditor editor;

	private CloudFoundryServiceWizardPage2 parent;

	public CFWizNameEditingSupport(CloudFoundryServiceWizardPage2 parent, TableViewer viewer) {
		super(viewer);
		this.viewer = viewer;
		this.editor = new TextCellEditor(viewer.getTable());
		this.parent = parent;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return editor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		CFServiceWizUI product = (CFServiceWizUI) element;
		if (product.getUserDefinedName() != null) {
			return product.getUserDefinedName();
		}
		else {
			return "";
		}

	}

	@Override
	protected void setValue(Object element, Object userInputValue) {
		CFServiceWizUI product = (CFServiceWizUI) element;
		product.setUserDefinedName((String) userInputValue);
		parent.validatePageState();
		viewer.update(element, null);
	}
}

class CFWizCloudPlanEditingSupport extends EditingSupport {

	private final TableViewer viewer;

	private CloudFoundryServiceWizardPage2 parent;

	public CFWizCloudPlanEditingSupport(CloudFoundryServiceWizardPage2 parent, TableViewer viewer) {
		super(viewer);
		this.viewer = viewer;
		this.parent = parent;

	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		List<CloudServicePlan> csp = null;

		CFServiceWizUI product = (CFServiceWizUI) element;
		csp = product.getOffering().getCloudServicePlans();

		String[] items = new String[csp.size()];
		for (int x = 0; x < items.length; x++) {
			items[x] = csp.get(x).getName();
		}

		return new ComboBoxCellEditor(viewer.getTable(), items, SWT.READ_ONLY);
	}

	@Override
	protected boolean canEdit(Object element) {
		CFServiceWizUI product = (CFServiceWizUI) element;

		if (product.getOffering() != null && product.getOffering().getCloudServicePlans() != null) {
			List<CloudServicePlan> csp = product.getOffering().getCloudServicePlans();
			return csp.size() > 0;
		}

		return false;
	}

	@Override
	protected Object getValue(Object element) {
		CFServiceWizUI product = (CFServiceWizUI) element;

		if (product.getPlan() != null) {
			List<CloudServicePlan> csp = product.getOffering().getCloudServicePlans();
			if (csp != null && csp.size() > 0) {
				for (int x = 0; x < csp.size(); x++) {
					if (csp.get(x) == product.getPlan()) {
						return x;
					}
				}

			}

		}

		return 0;

	}

	@Override
	protected void setValue(Object element, Object value) {
		CFServiceWizUI product = (CFServiceWizUI) element;
		List<CloudServicePlan> csp = product.getOffering().getCloudServicePlans();

		product.setPlan(csp.get((Integer) value));

		parent.validatePageState();

		viewer.update(element, null);
	}
}