/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software Inc and IBM Corporation. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *     IBM Corporation - Additions to services wizard
 *******************************************************************************/

package org.eclipse.cft.server.ui.internal.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.eclipse.cft.server.core.internal.client.LocalCloudService;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;

public class CloudFoundryServiceWizardPageRightPanel {

	private List<List<ServiceInstance>> serviceInstances = new ArrayList<List<ServiceInstance>>();

	private Group group;

	private Composite layoutList;

	private ScrolledComposite scrollComp;

	private Image removeImage;

	private CFWizServicePageValidation validation;

	/** The list of the names of existing services; this prevents us from creating services w/ duplicate names. */
	private List<String> existingServicesNames = null;

	private Font boldFont;

	CloudFoundryServiceWizardPage parent;

	public CloudFoundryServiceWizardPageRightPanel(CloudFoundryServiceWizardPage parent) {
		this.parent = parent;
		validation = new CFWizServicePageValidation(this);
	}

	public Composite createMainWindowComposite(Composite parent) {
		removeImage = CloudFoundryImages.REMOVE.createImage();

		group = new Group(parent, SWT.SHADOW_IN);
		group.setText(Messages.CloudFoundryServiceWizardPageRightPanel_SERVICES_TO_CREATE);
		group.setLayout(new FillLayout());

		group.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (boldFont != null && !boldFont.isDisposed()) {
					boldFont.dispose();
				}
				if (removeImage != null && !removeImage.isDisposed()) {
					removeImage.dispose();
				}
			}
		});

		scrollComp = new ScrolledComposite(group, SWT.V_SCROLL);
		scrollComp.setAlwaysShowScrollBars(false);

		layoutList = createLayoutList(scrollComp);
		scrollComp.setContent(layoutList);

		scrollComp.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent event) {
				Control c = scrollComp.getContent();
				if (c == null) {
					return;
				}
				Rectangle r = scrollComp.getClientArea();

				r.height = c.computeSize(r.width, SWT.DEFAULT).y;

				c.setBounds(r);
			}
		});

		return group;
	}

	public List<List<ServiceInstance>> getServiceInstances() {
		return serviceInstances;
	}

	protected CloudFoundryServiceWizardPage getParent() {
		return parent;
	}

	private Composite createLayoutList(ScrolledComposite parent) {

		Composite result = null;
		if (layoutList != null) {
			// Remove existing childen from composite
			for (Control c : layoutList.getChildren()) {
				c.dispose();
			}
			result = layoutList;
		} else {
			result = new Composite(parent, SWT.NONE);
		}

		Display display = result.getDisplay();
		Font font = result.getFont();

		if (boldFont == null) {
			FontData[] fontData = font.getFontData();
			fontData[0].setStyle(SWT.BOLD);
			fontData[0].setHeight(fontData[0].getHeight());
			boldFont = new Font(display, fontData);

		}

		if (result.getLayout() == null) {
			result.setLayout(new ServiceListRightCompositeLayout(serviceInstances));
		}

		List<Control> tabList = new ArrayList<Control>();

		FocusListener fl = new FocusListener();

		for (final List<ServiceInstance> l : serviceInstances) {

			// We retrieve the service title from the first service in the list
			ServiceInstance firstInList = l.get(0);

			String serviceName = firstInList.getName();

			Label serviceNameLabel = new Label(result, SWT.NONE);
			serviceNameLabel.setFont(boldFont);
			serviceNameLabel.setText(serviceName);

			// For service instance, create entries
			for (final ServiceInstance i : l) {

				Label nameLabel = new Label(result, SWT.NONE);
				nameLabel.setText(Messages.CloudFoundryServiceWizardPageRightPanel_NAME);
				nameLabel.setFont(boldFont);

				final Text nameText = new Text(result, SWT.BORDER);
				nameText.setText(i.getUserDefinedName());
				nameText.setData(i);
				tabList.add(nameText);
				nameText.addFocusListener(fl);
				nameText.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent e) {

						String text = nameText.getText();
						if (i != null) {
							i.setUserDefinedName(text);
						}
						validation.updatePageState();
					}
				});

				Button removeButton = new Button(result, SWT.FLAT);
				removeButton.setImage(removeImage);
				removeButton.setData(i);
				removeButton.setToolTipText(Messages.CloudFoundryServiceWizardPageRightPanel_REMOVE_TOOLTIP);
				removeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						removeServiceInstance(i);
					}
				});
				tabList.add(removeButton);
				removeButton.addFocusListener(fl);

				Label planLabel = new Label(result, SWT.NONE);
				planLabel.setText(Messages.CloudFoundryServiceWizardPageRightPanel_PLAN);

				final Combo planCombo = new Combo(result, SWT.DROP_DOWN | SWT.READ_ONLY);
				planCombo.setItems(i.getPlanDisplayNames());
				planCombo.select(i.getSelectedPlan());
				planCombo.setData(i);
				planCombo.addFocusListener(fl);

				planCombo.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						int selectionIndex = planCombo.getSelectionIndex();
						if (selectionIndex >= 0) {
							i.setSelectedPlan(selectionIndex);
						}
						else {
							i.setSelectedPlan(0);
						}
					}

				});
				tabList.add(planCombo);
			}

		}

		if (serviceInstances.size() == 0) {
			Label noServicesLabel = new Label(result, SWT.WRAP);
			noServicesLabel.setText(Messages.CloudFoundryServiceWizardPageRightPanel_ADD_A_SERVICE);
		}

		// Set tab traversal order
		result.setTabList(tabList.toArray(new Control[tabList.size()]));

		// Update validation based on initial values
		validation.updatePageState();

		return result;
	}

	private void removeServiceInstance(ServiceInstance instance) {

		for (Iterator<List<ServiceInstance>> listIterator = serviceInstances.iterator(); listIterator.hasNext();) {

			List<ServiceInstance> innerList = listIterator.next();

			for (Iterator<ServiceInstance> innerIterator = innerList.iterator(); innerIterator.hasNext();) {

				ServiceInstance serviceInstance = innerIterator.next();
				if (serviceInstance.equals(instance)) {
					innerIterator.remove();
				}

			}

			if (innerList.size() == 0) {
				listIterator.remove();
			}

		}
		updateLayoutListComposite();
	}

	private static boolean utilDoesServiceListContainName(List<List<ServiceInstance>> serviceListList, String serviceName) {

		for (List<ServiceInstance> serviceList : serviceListList) {
			for (ServiceInstance instance : serviceList) {

				if (instance.getUserDefinedName().equalsIgnoreCase(serviceName)) {
					return true;
				}
			}
		}

		return false;
	}

	public void setExistingServicesNames(List<String> existingServicesNames) {
		this.existingServicesNames = existingServicesNames;
	}

	public List<String> getExistingServicesNames() {
		return existingServicesNames;
	}

	/** Add a service instance to the list; instance param must have name, plans, and user-defined names set.*/
	public void addNewServiceInstance(ServiceInstance instance) {

		// Are there any existing services of this type? If so, store the list of them ehre
		List<ServiceInstance> serviceListMatch = null;

		for (List<ServiceInstance> serviceList : serviceInstances) {

			if(serviceList.size() == 0) { continue; } // This shouldn't happen, but check.

			ServiceInstance prototypicalInstance = serviceList.get(0);

			// We are comparing service name here, not user-defined name
			if (prototypicalInstance.getName().equals(instance.getName())) {
				serviceListMatch = serviceList;
				break;
			}

		}

		// If the service list already contains the specified name (or an existing service has the name), then add an ordinal to the end (beginning at 2)
		if (utilDoesServiceListContainName(serviceInstances, instance.getUserDefinedName())
			||  ( existingServicesNames != null && existingServicesNames.contains(instance.getUserDefinedName().toLowerCase())) 
				) {

			String newName = instance.getUserDefinedName();

			int x = 2;
			while(
					utilDoesServiceListContainName(serviceInstances, newName+x) 
					||  ( existingServicesNames != null && existingServicesNames.contains(newName.toLowerCase()+x))
				) {
				x++;

				if (x > 1000) {
					// sanity check
					break;
				}
			}

			instance.setUserDefinedName(newName + x);
		}

		if (serviceListMatch != null) {
			// There already exists one or more services of the type we are adding			
			serviceListMatch.add(instance);

			// Sort the service instances by user defined name (case insensitive)
			Collections.sort(serviceListMatch, new Comparator<ServiceInstance>() {
				@Override
				public int compare(ServiceInstance o1, ServiceInstance o2) {
					final String EMPTY = "zzzzzzzz"; // Empty strings should be sorted to end of the list
					String o1Name = o1.getUserDefinedName();
					String o2Name = o2.getUserDefinedName();
					if(o1Name == null || o1Name.trim().length() == 0) { o1Name = EMPTY; }
					if(o2Name == null || o2Name.trim().length() == 0) { o2Name = EMPTY; }
					return o1Name.toLowerCase().compareTo(o2Name.toLowerCase());

				}
			});

		} else {
			// There do not yet exist services of the type we are adding
			List<ServiceInstance> list = new ArrayList<ServiceInstance>();
			list.add(instance);

			serviceInstances.add(list);

			// Since we are adding a new service type to the list, sort the existing list
			Collections.sort(serviceInstances, new Comparator<List<ServiceInstance>>() {

				@Override
				public int compare(List<ServiceInstance> o1, List<ServiceInstance> o2) {

					if (o1.size() == 0 || o2.size() == 0) {
						// Sanity check: There shouldn't be a case where either of the lists is size 0
						if(o1.size() == o2.size()) { return 0; }
						else if(o2.size() > o1.size()) { return 1; }
						else { return -1; }
					}

					String o1Name = o1.get(0).getName().toLowerCase();
					String o2Name = o2.get(0).getName().toLowerCase();

					return o1Name.compareTo(o2Name);
				}
			});
		}

		updateLayoutListComposite();
	}

	private void updateLayoutListComposite() {
		createLayoutList(scrollComp);

		// Update scroll comp size
		Rectangle r = scrollComp.getClientArea();
		r.height = layoutList.computeSize(r.width, SWT.DEFAULT).y;
		layoutList.setBounds(r);
	}

	/** When tabbing into a widget, make sure it visible in the scroll composite */
	class FocusListener extends FocusAdapter {

		@Override
		public void focusGained(FocusEvent e) {
			Control c = (Control) e.getSource();
			ServiceInstance service = (ServiceInstance) c.getData();

			Rectangle bounds = service.getAppxLocation(); // child.getBounds();
			Rectangle area = scrollComp.getClientArea();
			Point origin = scrollComp.getOrigin();

			// Our view is lower than the item
			if (origin.y > bounds.y) {
				origin.y = Math.max(0, bounds.y);
			}

			// Our view is above the item
			if (origin.y + area.height < bounds.y + bounds.height) {
				origin.y = Math.max(0, bounds.y + bounds.height - area.height);
			}

			scrollComp.setOrigin(origin);
		}
	}
}

/** Layout manager for the service widgets on the right hand side */
class ServiceListRightCompositeLayout extends Layout {

	final List<List<ServiceInstance>> serviceInstances;

	public ServiceListRightCompositeLayout(List<List<ServiceInstance>> serviceInstances) {
		this.serviceInstances = serviceInstances;
	}

	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		int[] result = layoutImpl(composite, wHint, flushCache, false);

		return new Point(result[0], result[1]);
	}

	@Override
	protected void layout(Composite composite, boolean flushCache) {
		layoutImpl(composite, -1, flushCache, true);
	}

	private static void centerControl(Control c, int maxControlHeight) {

		Rectangle b = c.getBounds();

		int controlHeight = b.height;

		b.y += (maxControlHeight / 2) - controlHeight / 2;

		c.setBounds(b);
	}

	protected int[] layoutImpl(Composite composite, int wHint, boolean flushCache, boolean apply) {

		Control[] children = composite.getChildren();

		if (children.length == 0) {
			return new int[] { 0, 0 };
		}

		int width = wHint > 0 ? wHint : composite.getClientArea().width;

		int x = 0;
		int y = 0;

		int c = 0;

		final int HINDENT = 30;
		final int VINDENT = 10;

		final int COMBO_SIZE = 100;

		final int FOUR = 4;

		if (serviceInstances.size() == 0) {
			final int X_INDENT = 10;
			final int Y_INDENT = 10;
			Label noServicesLabel = (Label) children[0];
			Point noServicesLabelPoint = noServicesLabel.computeSize(width - 30, SWT.DEFAULT, flushCache);

			if (apply) {
				noServicesLabel.setBounds(X_INDENT, Y_INDENT, noServicesLabelPoint.x, noServicesLabelPoint.y);
				composite.setSize(width, y + noServicesLabelPoint.y + Y_INDENT);
			}
			return new int[] { width, y + noServicesLabelPoint.y + Y_INDENT };

		}

		// Initial indent
		y = 10;

		// For each group of related services
		for (List<ServiceInstance> l : serviceInstances) {

			boolean firstInList = true;

			// Initial indeent for each service
			x = 10;

			Label serviceNameLabel = (Label) children[c++];
			Point p = serviceNameLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
			if (apply) {
				serviceNameLabel.setBounds(x, y, p.x, p.y);
			}
			y += p.y;

			// For each service instance
			for (ServiceInstance i : l) {

				// Line 1
				x = HINDENT;
				int lineHeight = -1;

				Label nameLabel = (Label) children[c++];
				Point namePoint = nameLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
				if (apply) {
					nameLabel.setBounds(x, y, namePoint.x, namePoint.y);
				}
				lineHeight = Math.max(lineHeight, namePoint.y);
				x += namePoint.x + FOUR;

				Text nameText = (Text) children[c++];
				Point nameTextPoint = nameText.computeSize(width - x - 80, SWT.DEFAULT, flushCache);
				if (apply) {
					nameText.setBounds(x, y, nameTextPoint.x, nameTextPoint.y);
				}
				int nameTextX = x;

				x += nameTextPoint.x + FOUR;
				lineHeight = Math.max(lineHeight, nameTextPoint.y);

				Button removeButton = (Button) children[c++];
				Point removeButtonPoint = removeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
				if (apply) {
					removeButton.setBounds(x, y, removeButtonPoint.x, removeButtonPoint.y);
				}
				lineHeight = Math.max(lineHeight, removeButtonPoint.y);

				y += lineHeight;

				if (apply) {
					centerControl(nameLabel, lineHeight);
					centerControl(nameText, lineHeight);
					centerControl(removeButton, lineHeight);
				}

				// Line 2
				x = HINDENT;
				lineHeight = -1;

				Label planLabel = (Label) children[c++];
				Point planLabelPoint = planLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
				if (apply) {
					planLabel.setBounds(x, y, planLabelPoint.x, planLabelPoint.y);
				}
				x += planLabelPoint.x + FOUR;
				lineHeight = Math.max(lineHeight, planLabelPoint.y);

				x = Math.max(x, nameTextX);

				Combo planCombo = (Combo) children[c++];
				Point planComboPoint = planCombo.computeSize(COMBO_SIZE, SWT.DEFAULT, flushCache);
				if (apply) {
					planCombo.setBounds(x, y, planComboPoint.x, planComboPoint.y);
				}
				lineHeight = Math.max(lineHeight, planComboPoint.y);

				if (apply) {
					centerControl(planLabel, lineHeight);
					centerControl(planCombo, lineHeight);

					Rectangle topLeft;

					if (firstInList) {
						topLeft = serviceNameLabel.getBounds();
					} else {
						topLeft = nameLabel.getBounds();
					}

					Rectangle bottomRight = planCombo.getBounds();

					
					Rectangle appxLocation = new Rectangle(topLeft.x, topLeft.y, (bottomRight.x+bottomRight.width)-topLeft.x , (bottomRight.y+ bottomRight.height)- topLeft.y );
					i.setAppxLocation(appxLocation);
				}

				y += lineHeight;

				y += VINDENT / 2;
			}

			y += 10;

			firstInList = false;
		}

		if (apply) {
			composite.setSize(width, y);
		}
		return new int[] { width, y };
	}
}

/** Contains a single instance of a service to be created. */
class ServiceInstance {
	private String name;

	private String userDefinedName;

	private int selectedPlan = 0;

	private Rectangle appxLocation = null;

	private final String[] planNames;

	private final String[] planDisplayNames;

	private final List<CloudServicePlan> sortedPlans;

	private final CloudServiceOffering offering;

	public ServiceInstance(String name, List<CloudServicePlan> plans, CloudServiceOffering offering) {
		this.name = name;
		this.sortedPlans = new ArrayList<CloudServicePlan>(plans);
		Collections.sort(this.sortedPlans, new Comparator<CloudServicePlan>() {

			@Override
			public int compare(CloudServicePlan p1, CloudServicePlan p2) {
				if (p1.isFree()) {
					return -1;
				}
				else if (p2.isFree()) {
					return 1;
				}
				else {
					return 0;
				}
			}
		});

		this.offering = offering;

		planNames = new String[this.sortedPlans.size()];
		planDisplayNames = new String[this.sortedPlans.size()];
		
		for (int x = 0; x < this.sortedPlans.size(); x++) {
			CloudServicePlan plan = this.sortedPlans.get(x);
			planNames[x] = plan.getName();
			planDisplayNames[x] = plan.isFree() ? NLS.bind(Messages.CloudFoundryServiceWizardPageRightPanel_FREE_PLAN,
					plan.getName()) : plan.getName();
		}
	}

	public String getName() {
		return name;
	}

	public int getSelectedPlan() {
		return selectedPlan;
	}

	public void setSelectedPlan(int selectedPlan) {
		this.selectedPlan = selectedPlan;
	}

	public void setUserDefinedName(String userDefinedName) {
		this.userDefinedName = userDefinedName;
	}

	public String getUserDefinedName() {
		return userDefinedName;
	}

	public String[] getPlanNames() {
		return planNames;
	}

	public String[] getPlanDisplayNames() {
		return planDisplayNames;
	}

	public List<CloudServicePlan> getPlans() {
		return this.sortedPlans;
	}

	public void setAppxLocation(Rectangle appxLocation) {
		this.appxLocation = appxLocation;
	}

	public Rectangle getAppxLocation() {
		return appxLocation;
	}

	/** Convert to local service, for the purpose of creation. */
	public LocalCloudService convertToLocalService() {
		final LocalCloudService localService = new LocalCloudService(""); //$NON-NLS-1$
		localService.setName(getUserDefinedName());
		localService.setVersion(offering.getVersion());
		localService.setLabel(offering.getLabel());
		localService.setProvider(offering.getProvider());
		localService.setPlan(planNames[getSelectedPlan()]);
		return localService;
	}
}

class CFWizServicePageValidation {

	private static Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+"); //$NON-NLS-1$

	CloudFoundryServiceWizardPageRightPanel wizardPage;

	public CFWizServicePageValidation(CloudFoundryServiceWizardPageRightPanel right) {
		this.wizardPage = right;
	}

	public void updatePageState() {

		boolean descriptionUpdated = false;

		if (!descriptionUpdated) {

			List<List<ServiceInstance>> list = wizardPage.getServiceInstances();
			if (list == null || list.size() == 0) {
				wizardPage.getParent().setDescription(Messages.CloudFoundryServiceWizardPage_TEXT_SELECT_SERVICE_LIST);
				wizardPage.getParent().setErrorMessage(null);
				descriptionUpdated = true;
			} else {							

				// Locate duplicate service names in the list of new services to create

				String dupeNameSeen = null;
				Map<String, Boolean> userDefinedNames = new HashMap<String, Boolean>();

				for (List<ServiceInstance> l : list) {

					for (ServiceInstance service : l) {

						if (userDefinedNames.containsKey(service.getUserDefinedName())) {
							dupeNameSeen = service.getUserDefinedName();
							break;
						} else {
							userDefinedNames.put(service.getUserDefinedName(), Boolean.TRUE);
						}
					}

					if (dupeNameSeen != null) {
						break;
					}
				}

				if (dupeNameSeen != null) {
					wizardPage.getParent().setDescription(null);
					wizardPage.getParent().setErrorMessage(Messages.CloudFoundryServiceWizardPageRightPanel_ERROR_DUPE_SERVICE_NAMES_FOUND+dupeNameSeen);
					descriptionUpdated = true;
				}
			}
		}

		if (!descriptionUpdated) {
			// Verify that all services have a name, and have valid characters

			List<List<ServiceInstance>> list = wizardPage.getServiceInstances();

			if (list != null) {
				for (List<ServiceInstance> l : list) {

					for (ServiceInstance service : l) {
						String userDefinedName = service.getUserDefinedName();

						if (service != null && userDefinedName != null) {

							if (userDefinedName.trim().length() == 0) {
								wizardPage.getParent().setDescription(Messages.CloudFoundryServiceWizardPage_TEXT_SET_SERVICE_NAME);
								wizardPage.getParent().setErrorMessage(null);
								descriptionUpdated = true;
							}

							Matcher matcher = VALID_CHARS.matcher(userDefinedName);
							if (!descriptionUpdated && !matcher.matches()) {
								wizardPage.getParent().setErrorMessage(Messages.CloudFoundryServiceWizardPage_ERROR_INVALID_CHAR);
								wizardPage.getParent().setDescription(null);
								descriptionUpdated = true;
							}
						}

						if (descriptionUpdated) {
							break;
						}
					}

					if (descriptionUpdated) {
						break;
					}
				}
			}
		}

		if (!descriptionUpdated) {
			// Flag service names that match existing services in the space

			List<String> existingServiceNames = wizardPage.getExistingServicesNames();

			List<List<ServiceInstance>> list = wizardPage.getServiceInstances();

			if (list != null) {
				String nameMatched = null;

				for (List<ServiceInstance> l : list) {

					for (ServiceInstance service : l) {

						String userDefinedName = service.getUserDefinedName().toLowerCase();

						if (existingServiceNames.contains(userDefinedName)) {
							nameMatched = service.getUserDefinedName();
						}
						if(nameMatched != null) { break; }
					}

					if(nameMatched != null) { break; }
				}

				if (nameMatched != null) {
					wizardPage.getParent().setErrorMessage(Messages.CloudFoundryServiceWizardPageRightPanel_EXISTING_SERVICE_DUPE+nameMatched);
					wizardPage.getParent().setDescription(null);
					descriptionUpdated = true;
				}
			}
		}

		if (!descriptionUpdated) {
			wizardPage.getParent().setErrorMessage(null);
			wizardPage.getParent().setDescription(Messages.CloudFoundryServiceWizardPage_TEXT_FINISH);
			wizardPage.getParent().setPageComplete(true);
		}
		else {
			wizardPage.getParent().setPageComplete(false);
		}
	}
}