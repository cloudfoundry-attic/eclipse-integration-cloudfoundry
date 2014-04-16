/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.LocalCloudService;
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

public class CloudFoundryServicePlanWizardPage extends WizardPage {

	protected DataBindingContext bindingContext;

	private final CloudFoundryServer cloudServer;

	private List<CloudServiceOffering> serviceOfferings;

	protected WritableMap map;

	private Text nameText;

	/**
	 * The data model.
	 */
	protected LocalCloudService service;

	private Composite planDetailsComposite;

	protected Group planGroup;

	private PageBook pageBook;

	private WritableValue planObservable = new WritableValue();

	private Combo typeCombo;

	private Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+");

	protected CloudFoundryServicePlanWizardPage(CloudFoundryServer cloudServer) {
		super("service");
		this.cloudServer = cloudServer;
		setTitle("Service Configuration");
		setDescription("Finish to add the service.");
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
		this.service = createService();
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
					CloudServiceOffering configuration = serviceOfferings.get(index);
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

	public LocalCloudService getService() {
		return service;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && serviceOfferings == null) {
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
		return cloudServer != null && cloudServer.hasCloudSpace();
	}

	protected void refresh() {
		if (updateConfiguration()) {
			typeCombo.removeAll();
			for (CloudServiceOffering offering : serviceOfferings) {
				String label = offering.getLabel() != null ? offering.getLabel() + " - "
						+ offering.getDescription() : offering.getDescription();
				typeCombo.add(label);
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
			CloudServiceOffering configuration = serviceOfferings.get(index);
			List<CloudServicePlan> servicePlans = getPlans(configuration);

			if (servicePlans.size() > 1) {
				pageBook.showPage(planGroup);
				planGroup.setVisible(true);

				Button defaultPlanControl = null;

				for (CloudServicePlan plan : servicePlans) {

					String planLabelText = plan.getName();

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
								CloudServicePlan plan = (CloudServicePlan) button.getData();
								setPlan(plan);

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
					CloudServicePlan plan = (CloudServicePlan) defaultPlanControl.getData();
					setPlan(plan);

				}
			}
			else if (servicePlans.size() == 1) {
				planGroup.setVisible(false);
				CloudServicePlan plan = servicePlans.get(0);
				setPlan(plan);
			}
			else {
				pageBook.setVisible(false);
			}
		}
		((Composite) getControl()).layout(true, true);

	}

	protected void setPlan(CloudServicePlan plan) {
		getService().setPlan(plan.getName());
		// re-validate
		planObservable.setValue(plan);
	}

	protected boolean updateConfiguration() {
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						serviceOfferings = cloudServer.getBehaviour().getServiceOfferings(monitor);
						Collections.sort(serviceOfferings, new Comparator<CloudServiceOffering>() {
							public int compare(CloudServiceOffering o1, CloudServiceOffering o2) {
								return o1.getDescription().compareTo(o2.getDescription());
							}
						});
						sortServicePlans(serviceOfferings);

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

	protected void sortServicePlans(List<CloudServiceOffering> configurations) {

		for (CloudServiceOffering offering : configurations) {
			Collections.sort(offering.getCloudServicePlans(), new Comparator<CloudServicePlan>() {
				public int compare(CloudServicePlan o1, CloudServicePlan o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
		}
	}

	protected List<CloudServicePlan> getPlans(CloudServiceOffering offering) {
		List<CloudServicePlan> plans = new ArrayList<CloudServicePlan>();

		List<CloudServicePlan> cloudPlans = offering.getCloudServicePlans();

		if (cloudPlans != null) {
			for (CloudServicePlan plan : cloudPlans) {
				plans.add(plan);
			}
		}
		return plans;

	}

	protected String getValidationErrorMessage() {
		return "Select a plan";
	}

	protected String getPlanLabel() {
		return "Plan";
	}

	protected void setCloudService(CloudService service, CloudServiceOffering offering) {

		service.setVersion(offering.getVersion());
		service.setLabel(offering.getLabel());
		service.setProvider(offering.getProvider());

	}

	protected LocalCloudService createService() {
		LocalCloudService service = new LocalCloudService("");
		return service;
	}

}
