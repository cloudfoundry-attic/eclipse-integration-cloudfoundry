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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.ServiceConfiguration;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.map.WritableMap;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.statushandlers.StatusManager;

public abstract class AbstractCloudFoundryServiceWizardPage extends WizardPage {

	protected DataBindingContext bindingContext;

	private final CloudFoundryServer cloudServer;

	private List<ServiceConfiguration> configurations;

	protected WritableMap map;

	private Text nameText;

	/**
	 * The data model.
	 */
	protected CloudService service;

	private Composite planDetailsComposite;

	protected Group planGroup;

	private PageBook pageBook;

	private WritableValue planObservable = new WritableValue();

	private Combo typeCombo;

	private Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+");

	protected AbstractCloudFoundryServiceWizardPage(CloudFoundryServer cloudServer) {
		super("service");
		this.cloudServer = cloudServer;
		setTitle("Service Configuration");
		setDescription("Finish to add the service.");
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
		this.service = new CloudService();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, LayoutConstants.getSpacing().y).applyTo(composite);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Name:");

		nameText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameText);
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				service.setName(nameText.getText());
			}
		});

		bindingContext = new DataBindingContext();
		map = new WritableMap();

		WizardPageSupport.create(this, bindingContext);

		bindingContext.bindValue(SWTObservables.observeText(nameText, SWT.Modify),
				Observables.observeMapEntry(map, "name"),
				new UpdateValueStrategy().setAfterConvertValidator(new StringValidator()), null);

		label = new Label(composite, SWT.NONE);
		label.setText("Type:");

		typeCombo = new Combo(composite, SWT.READ_ONLY | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(typeCombo);
		typeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				int index = typeCombo.getSelectionIndex();
				if (index != -1) {
					ServiceConfiguration configuration = configurations.get(index);
					setCloudService(service, configuration);
				}
				refreshPlan();
			}
		});

		bindingContext.bindValue(SWTObservables.observeSelection(typeCombo), Observables.observeMapEntry(map, "type"),
				new UpdateValueStrategy().setAfterConvertValidator(new ComboValidator("Select a type")), null);

		pageBook = new PageBook(composite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(pageBook);

		planGroup = new Group(pageBook, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(planGroup);
		planGroup.setLayout(new GridLayout());
		planGroup.setVisible(false);
		planGroup.setText(getPlanLabel());

		MultiValidator validator = new MultiValidator() {
			protected IStatus validate() {
				// access plan value to bind validator
				if (planObservable.getValue() == null) {
					return ValidationStatus.cancel(getValidationErrorMessage());
				}
				return ValidationStatus.ok();
			}
		};
		bindingContext.addValidationStatusProvider(validator);

		Dialog.applyDialogFont(composite);
		setControl(composite);
	}

	public CloudService getService() {
		return service;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && configurations == null) {
			// delay until dialog is actually visible
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if (!getControl().isDisposed()) {
						refresh();
					}
				}
			});
		}
	}

	protected boolean supportsSpaces() {
		return cloudServer != null && cloudServer.supportsCloudSpaces();
	}

	protected void refresh() {
		if (updateConfiguration()) {
			typeCombo.removeAll();
			for (ServiceConfiguration configuration : configurations) {
				typeCombo.add(configuration.getDescription());
			}
			refreshPlan();
		}
	}

	protected void refreshPlan() {
		int index = typeCombo.getSelectionIndex();
		if (index == -1) {
			pageBook.setVisible(false);
			planGroup.setVisible(false);

			// re-validate
			planObservable.setValue(null);
		}
		else {
			pageBook.setVisible(true);

			for (Control control : planGroup.getChildren()) {
				control.dispose();
			}
			ServiceConfiguration configuration = configurations.get(index);
			List<ServicePlan> servicePlans = getPlans(configuration);

			if (servicePlans.size() > 1) {
				pageBook.showPage(planGroup);
				planGroup.setVisible(true);

				Button defaultPlanControl = null;

				for (ServicePlan plan : servicePlans) {

					String planLabelText = plan.getDisplayValue();

					Button planButton = new Button(planGroup, SWT.RADIO);

					if (defaultPlanControl == null) {
						defaultPlanControl = planButton;
					}

					planButton.setText(planLabelText);
					planButton.setData(plan);
					planButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent event) {
							Button button = (Button) event.widget;
							if (button.getSelection()) {
								ServicePlan plan = (ServicePlan) button.getData();
								setPlan(plan);
								refreshPlanDetails(plan, planDetailsComposite);
							}
						}
					});
				}

				planDetailsComposite = new Composite(planGroup, SWT.NONE);
				GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 10, 0).numColumns(2)
						.applyTo(planDetailsComposite);

				// Set a default plan, if one exists
				if (defaultPlanControl != null) {
					defaultPlanControl.setSelection(true);
					ServicePlan plan = (ServicePlan) defaultPlanControl.getData();
					setPlan(plan);
					refreshPlanDetails(plan, planDetailsComposite);
				}
			}
			else if (servicePlans.size() == 1) {
				planGroup.setVisible(false);
				ServicePlan plan = servicePlans.get(0);
				setPlan(plan);
			}
			else {
				pageBook.setVisible(false);
			}
		}
		((Composite) getControl()).layout(true, true);

	}

	protected void setPlan(ServicePlan plan) {
		plan.updateInService();
		service.getOptions().clear();
		// re-validate
		planObservable.setValue(plan);
	}

	protected boolean updateConfiguration() {
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						configurations = cloudServer.getBehaviour().getServiceConfigurations(monitor);
						Collections.sort(configurations, new Comparator<ServiceConfiguration>() {
							public int compare(ServiceConfiguration o1, ServiceConfiguration o2) {
								return o1.getDescription().compareTo(o2.getDescription());
							}
						});
						sortServicePlans(configurations);

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
			IStatus status = cloudServer.error(
					NLS.bind("Configuration retrieval failed: {0}", e.getCause().getMessage()), e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
			setMessage(status.getMessage(), IMessageProvider.ERROR);
		}
		catch (InterruptedException e) {
			// ignore
		}
		return false;
	}

	protected class ComboValidator implements IValidator {

		private final String message;

		public ComboValidator(String message) {
			this.message = message;
		}

		public IStatus validate(Object value) {
			if (value instanceof String && ((String) value).length() > 0) {
				return Status.OK_STATUS;
			}
			return ValidationStatus.cancel(message);
		}

	}

	protected class StringValidator implements IValidator {

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

	abstract protected void refreshPlanDetails(ServicePlan plan, Composite refreshDetailsComposite);

	abstract protected void sortServicePlans(List<ServiceConfiguration> configurations);

	abstract protected List<ServicePlan> getPlans(ServiceConfiguration configuration);

	abstract protected String getValidationErrorMessage();

	abstract protected String getPlanLabel();

	abstract protected void setCloudService(CloudService service, ServiceConfiguration configuration);

	abstract static class ServicePlan {

		private final CloudService service;

		public ServicePlan(CloudService service) {
			this.service = service;
		}

		public CloudService getService() {
			return service;
		}

		abstract public void updateInService();

		abstract protected String getDisplayValue();

	}

}
