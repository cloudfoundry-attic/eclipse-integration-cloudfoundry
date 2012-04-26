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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.CloudService;
import org.cloudfoundry.client.lib.ServiceConfiguration;
import org.cloudfoundry.client.lib.ServiceConfiguration.Option;
import org.cloudfoundry.client.lib.ServiceConfiguration.Tier;
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

/**
 * @author Steffen Pingel
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class CloudFoundryServiceWizardPage extends WizardPage {

	private DataBindingContext bindingContext;

	private final CloudFoundryServer cloudServer;

	private List<ServiceConfiguration> configurations;

	private WritableMap map;

	private Text nameText;

	/**
	 * The data model.
	 */
	private CloudService service;

	private Composite tierDetailsComposite;

	private Group tierGroup;

	private PageBook pageBook;

	private WritableValue tierObservable = new WritableValue();

	private Combo typeCombo;

	private Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+");

	public CloudFoundryServiceWizardPage(CloudFoundryServer cloudServer) {
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
					service.setType(configuration.getType());
					service.setVendor(configuration.getVendor());
					service.setVersion(configuration.getVersion());
				}
				refreshTier();
			}
		});

		bindingContext.bindValue(SWTObservables.observeSelection(typeCombo), Observables.observeMapEntry(map, "type"),
				new UpdateValueStrategy().setAfterConvertValidator(new ComboValidator("Select a type")), null);

		pageBook = new PageBook(composite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(pageBook);

		tierGroup = new Group(pageBook, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tierGroup);
		tierGroup.setLayout(new GridLayout());
		tierGroup.setVisible(false);
		tierGroup.setText("Tier");

		MultiValidator validator = new MultiValidator() {
			protected IStatus validate() {
				// access tier value to bind validator
				if (tierObservable.getValue() == null) {
					return ValidationStatus.cancel("Select a tier");
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

	protected void refresh() {
		if (updateConfiguration()) {
			typeCombo.removeAll();
			for (ServiceConfiguration configuration : configurations) {
				typeCombo.add(configuration.getDescription());
			}
			refreshTier();
		}
	}

	protected void refreshTier() {
		int index = typeCombo.getSelectionIndex();
		if (index == -1) {
			pageBook.setVisible(false);
			tierGroup.setVisible(false);

			// re-validate
			tierObservable.setValue(null);
		}
		else {
			pageBook.setVisible(true);

			for (Control control : tierGroup.getChildren()) {
				control.dispose();
			}
			ServiceConfiguration configuration = configurations.get(index);
			List<Tier> tiers = configuration.getTiers();

			if (tiers.size() > 1) {
				pageBook.showPage(tierGroup);
				tierGroup.setVisible(true);

				Button defaultTierControl = null;

				for (Tier tier : tiers) {

					String tierLabelText = tier.getDescription() != null ? tier.getDescription() : tier.getType();

					Button tierButton = new Button(tierGroup, SWT.RADIO);

					if (defaultTierControl == null) {
						defaultTierControl = tierButton;
					}

					tierButton.setText(tierLabelText);
					tierButton.setData(tier);
					tierButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent event) {
							Button button = (Button) event.widget;
							if (button.getSelection()) {
								Tier tier = (Tier) button.getData();
								setTier(tier);
								refreshTierDetails(tier);
							}
						}
					});
				}

				tierDetailsComposite = new Composite(tierGroup, SWT.NONE);
				GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 10, 0).numColumns(2)
						.applyTo(tierDetailsComposite);

				// Set a default tier, if one exists
				if (defaultTierControl != null) {
					defaultTierControl.setSelection(true);
					Tier tier = (Tier) defaultTierControl.getData();
					setTier(tier);
					refreshTierDetails(tier);
				}
			}
			else if (tiers.size() == 1) {
				tierGroup.setVisible(false);
				Tier tier = tiers.get(0);
				setTier(tier);
			}
			else {
				pageBook.setVisible(false);
			}
		}
		((Composite) getControl()).layout(true, true);

	}

	protected void setTier(Tier tier) {
		service.setTier(tier.getType());
		service.getOptions().clear();
		// re-validate
		tierObservable.setValue(tier);
	}

	protected void refreshTierDetails(Tier tier) {
		for (Control control : tierDetailsComposite.getChildren()) {
			control.dispose();
		}
		for (final Option option : tier.getOptions()) {
			Label label = new Label(tierDetailsComposite, SWT.NONE);
			label.setText(option.getDescription());

			final Combo combo = new Combo(tierDetailsComposite, SWT.READ_ONLY);
			final List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(option.getPriceByValue()
					.entrySet());
			for (Entry<String, Integer> entry : list) {
				if (entry.getValue() != null) {
					combo.add(NLS.bind("{0} (${1}/{2})",
							new Object[] { entry.getKey(), entry.getValue(), tier.getPricingPeriod() }));
				}
				else {
					combo.add(entry.getKey());
				}
			}
			combo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					service.getOptions().put(option.getName(), list.get(combo.getSelectionIndex()).getKey());
				}
			});

			bindingContext.bindValue(SWTObservables.observeSelection(combo), Observables.observeMapEntry(map,
					option.getName()), new UpdateValueStrategy().setAfterConvertValidator(new ComboValidator(NLS.bind(
					"Select a {0}", option.getName()))), null);
		}

		tierGroup.layout(true, true);
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
						for (ServiceConfiguration configuration : configurations) {
							Collections.sort(configuration.getTiers(), new Comparator<Tier>() {
								public int compare(Tier o1, Tier o2) {
									return o1.getOrder() - o2.getOrder();
								}
							});
						}
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

	private class ComboValidator implements IValidator {

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

	private class StringValidator implements IValidator {

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
