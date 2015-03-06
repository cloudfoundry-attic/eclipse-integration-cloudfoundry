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

package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.ui.ICloudFoundryServiceWizardIconProvider;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IRuntime;

public class CloudFoundryServiceWizardPageLeftPanel {
	
	/** Services that are available for the user to click */
	List<AvailableService> availableServices = new ArrayList<AvailableService>();
	
	/** One or more services the user has already clicked. */
	List<AvailableService> selectedServices = new ArrayList<AvailableService>();
	
	protected Color selEdgeColor;

	protected Color selFillColor;

	ScrolledComposite scrollComp;
	Composite layoutList;

	private Button clearButton;	
	private Button addButton;
	
	private Text filterText;
	
	CloudFoundryServiceWizardPage parent;
	
	// Keyboard traversal listener; see description on class for details.
	private ServiceListTraverseListener traverseListener;

	/** Optional field -- used to provide icons in service wizard if available */
	CFServiceWizardDynamicIconLoader loader;
	
	private Font boldFont;

	private String filterTerm = null;

	public CloudFoundryServiceWizardPageLeftPanel(CloudFoundryServiceWizardPage parent) {
		traverseListener = new ServiceListTraverseListener();
		this.parent = parent;
	}
	
	private Composite createFilterComp(Composite parent) {
		
		Composite filterComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
//		layout.horizontalSpacing = convertHorizontalDLUsToPixels(comp, 4);
//		layout.verticalSpacing = convertHorizontalDLUsToPixels(comp, 4);
		filterComp.setLayout(layout);

		final Label filterLabel = new Label(filterComp, SWT.NONE);
		filterLabel.setText(Messages.CloudFoundryServiceWizardPageLeftPanel_FILTER_TITLE);
		GridData data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
		filterLabel.setLayoutData(data);

		filterText = new Text(filterComp, SWT.SEARCH);
		filterText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));
		filterText.setMessage(Messages.CloudFoundryServiceWizardPageLeftPanel_DEFAULT_FILTER_TEXT);

		filterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				
				String text = filterText.getText();
				if (Messages.CloudFoundryServiceWizardPageLeftPanel_DEFAULT_FILTER_TEXT.equals(text)) {
					filterTerm = null;
				}
				else {
					filterTerm = filterText.getText().trim();
				}
				updateServiceList();
			}
		});
		
		MouseWheelListener mwl = new MouseWheelListener() {

			public void mouseScrolled(MouseEvent event) {
				scrollComp.setFocus();
			}
		};

		filterText.addMouseWheelListener(mwl);		
		filterText.addTraverseListener(traverseListener);
		
		return filterComp;
		
	}
	
	public Group createMainWindowComposite(Composite parent) {
		
		IRuntime runtime = this.parent.getCloudServer().getServer().getRuntime();
		String runtimeTypeId = runtime.getRuntimeType().getId();

		// Is there an icon provider for the service, if so, use it
		ICloudFoundryServiceWizardIconProvider provider = CloudFoundryServiceWizardIconProviderRegistry.getInstance().getIconProvider(runtimeTypeId);
		if (provider != null) {
			loader = new CFServiceWizardDynamicIconLoader(provider, this.parent.getCloudServer());
			loader.start();
		}
		
		GridLayout gridLayout = new GridLayout(3, false);		
		
		Group group = new Group(parent, SWT.SHADOW_IN);
		group.setText(Messages.CloudFoundryServiceWizardPageLeftPanel_AVAILABLE_SERVICES);
		group.setLayout(gridLayout);

		Composite filterComp = createFilterComp(group);
		GridData gd = new GridData (SWT.FILL, SWT.DEFAULT, true, false, 3, 1);
		filterComp.setLayoutData(gd);
				
		scrollComp = new ScrolledComposite(group,  SWT.V_SCROLL);
		gd = new GridData (SWT.FILL, SWT.FILL, true, true, 3, 1);

		scrollComp.setLayoutData(gd);
		scrollComp.setAlwaysShowScrollBars(false);
		scrollComp.setBackground(scrollComp.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		
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
		
		layoutList = createLayoutList(scrollComp);
		scrollComp.setContent(layoutList);

		Label emptyLabel = new Label(group, SWT.NONE);
		emptyLabel.setText("");
		gd = new GridData (SWT.FILL, SWT.CENTER, true, false, 1, 1);
		emptyLabel.setLayoutData(gd);
		
		
		clearButton = new Button(group, SWT.PUSH);
		clearButton.setText(Messages.CloudFoundryServiceWizardPageLeftPanel_CLEAR);
		gd = new GridData (SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		clearButton.setLayoutData(gd);
		clearButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deselectServices(selectedServices);
				selectedServices.clear();
				
				addButton.setEnabled(false);
				clearButton.setEnabled(false);
				layoutList.redraw();
			}
		});
		clearButton.setEnabled(false);
		
		
		addButton = new Button(group, SWT.PUSH);
		addButton.setText(Messages.CloudFoundryServiceWizardPageLeftPanel_ADD);
		gd = new GridData (SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		addButton.setLayoutData(gd);
		addButton.setEnabled(false);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Only add selected services that are visible.
				List<AvailableService> visibleSelectedServices = utilGetVisibleServices(selectedServices);
				
				if(visibleSelectedServices.size() > 0) {
					createNewServiceInstances(visibleSelectedServices.toArray(new AvailableService[visibleSelectedServices.size()]));
					
				}
			}
		});
		
		// Shift+tab on Add button should not reset the selected items in the list 
		clearButton.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
					if(selectedServices.size() > 0 ) {
						selectedServices.get(0).getNameLabel().forceFocus();
						e.doit = false;
					}
					
				}
			}
		});
		
		return group;
	}
	
	private Composite createLayoutList(Composite parent) {

		Composite result = new Composite(parent, SWT.NONE);
		
		result.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		
		Display display = result.getDisplay();	
		Font font = result.getFont();
		
		if (boldFont == null) {
			FontData[] fontData = font.getFontData();
			fontData[0].setStyle(SWT.BOLD);
			fontData[0].setHeight(fontData[0].getHeight());
			boldFont = new Font(display, fontData);

			Color c1 = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
			Color c2 = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
			
			selEdgeColor = new Color(display, (c1.getRed() + c2.getRed() * 3) / 4,
					(c1.getGreen() + c2.getGreen() * 3) / 4, (c1.getBlue() + c2.getBlue() * 3) / 4);
			selFillColor = new Color(display, (c1.getRed() + c2.getRed() * 8) / 9,
					(c1.getGreen() + c2.getGreen() * 8) / 9, (c1.getBlue() + c2.getBlue() * 8) / 9);
			
			result.addDisposeListener(new DisposeListener() {

				public void widgetDisposed(DisposeEvent event) {
					if (boldFont != null && !boldFont.isDisposed()) {
						boldFont.dispose();
					}
					if (selEdgeColor != null && !selEdgeColor.isDisposed()) {
						selEdgeColor.dispose();
					}
					if (selFillColor != null && !selFillColor.isDisposed()) {
						selFillColor.dispose();
					}
				}
			});
			
		}

		result.setLayout(new TextCompositeLayout(this.loader != null));

		// Paint behind the service list, based on what services are selected. 
		result.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent event) {

				for(AvailableService service : selectedServices) {
					if(service.getNameLabel().isVisible()) {
						Rectangle r = service.getAppxLocation();
						if(r != null) {
							Rectangle r2 = new Rectangle(r.x, r.y, r.width, r.height);
							
							r2.x -= 3;
							r2.width += 6;
							
							r2.y -= 3;
							r2.height += 6;
							
							event.gc.setBackground(selFillColor);
							event.gc.fillRoundRectangle(r2.x, r2.y, r2.width, r2.height, 7, 7);
		
							event.gc.setForeground(selEdgeColor);
							event.gc.drawRoundRectangle(r2.x, r2.y, r2.width, r2.height, 7, 7);
						}
					}
				}
			}
		});

		return result;
	}
	
	/** Called by CloudFoundryServiceWizardPage.setVisible() once the page becomes visible. */
	public void createInnerLayoutList(final List<AvailableService> availableServices, Composite result) {
		
		this.availableServices = availableServices;
		
		Color color = result.getBackground();
		
		Control[] tabList = new Control[availableServices.size()];
		
		for(int x = 0; x < availableServices.size(); x++) {

			AvailableService service = availableServices.get(x);
			
			final Label imgLabel = new Label(result, SWT.NONE);
			imgLabel.setBackground(color);
			imgLabel.setData(service);
			
			final Label nameLabel = new Label(result, SWT.WRAP);
			nameLabel.setData(service);
			nameLabel.setFont(boldFont);
			nameLabel.setBackground(color);
			if (loader != null) {
				loader.addIconToRetrieveList(service.getOffering(), imgLabel);
			}

			// When a service is selected by the keyboard, it is the nameLabel which is selected behind the scenes.
			// Here we ensure that ENTER Key adds a service, and page up/down custom logic works
			nameLabel.setText(service.getName());
			nameLabel.addKeyListener(new KeyAdapter() { 
				@Override
				public void keyPressed(KeyEvent e) {
					if(e.keyCode == '\n' || e.keyCode == '\r') {
						AvailableService currService = (AvailableService) (((Label)e.getSource()).getData());
						
						for(AvailableService as : selectedServices) {
							if(as != currService) {
								createNewServiceInstances(new AvailableService[] { as } );
							}
						}
						
						createNewServiceInstances(new AvailableService[] { currService } );
						e.doit = false;
						
					}
					if(e.keyCode == SWT.PAGE_UP || e.keyCode == SWT.PAGE_DOWN) {
						AvailableService currService = (AvailableService) (((Label)e.getSource()).getData());
						
						int indexOfService = -1; // index of service in new visible service list
						
						// Add only visibles services to a new list
						List<AvailableService> visibleServices = new ArrayList<AvailableService>();
						for(AvailableService as : availableServices) {
							if(as.getNameLabel() != null && as.getNameLabel().isVisible()) {
								visibleServices.add(as);
							}
						}

						// Locate the index of the selected service inside the visible service list
						for(int x = 0; x < visibleServices.size(); x++) {
							AvailableService as = visibleServices.get(x);
							if(as == currService) {
								indexOfService = x; 
								break;
							}
						}
						
						// For page/up down, move up or down 5 _visible services_
						if(indexOfService != -1 && visibleServices.size() > 0) {
							if(e.keyCode == SWT.PAGE_UP) {
								indexOfService = Math.max(0, indexOfService-5);
							}  else {
								indexOfService = Math.min(visibleServices.size()-1, indexOfService+5);
							}
																			
							selectService(visibleServices.get(indexOfService), true, true);
						}
						
						e.doit = false;
					}
				}} 
			);
			
			tabList[x] = nameLabel;

			final Label descLabel = new Label(result, SWT.WRAP);
			descLabel.setData(service);
			descLabel.setBackground(color);
			String desc = service.getDesc();
			descLabel.setText(trimTextAtBound(desc, 200));

			service.setDescLabel(descLabel);
			service.setImageLabel(imgLabel);
			service.setNameLabel(nameLabel);
			
			imgLabel.addMouseListener(new CFAvailableServiceListener(service));
			nameLabel.addMouseListener(new CFAvailableServiceListener(service));
			descLabel.addMouseListener(new CFAvailableServiceListener(service));
			
			descLabel.setToolTipText(wrapAndTrimTextAtBound(desc, 100));
		
		} // end for
	
		// We need to handle mouse clicks on the panel behind the name/icon/description, as there is a small whitespace gap
		// between them.  We locate which service they correspond to base on known service location.
		result.addMouseListener(new MouseAdapter() {
	
			/** Locate the service at the position of the mouse click  */
			private AvailableService getServiceAtClickPos(MouseEvent e, List<AvailableService> availableServices) {
				AvailableService result = null;
				
				boolean foundExactMatch = false;
				for (final AvailableService service : availableServices) {
					if(service.getAppxLocation() != null && service.getAppxLocation().contains(e.x,  e.y)) {
						result = service;
						
						foundExactMatch = true;
						break;
					}
				}
				
				if(!foundExactMatch) {
					// Try again, but use the service that directly precedes it
					for (final AvailableService service : availableServices) {
						int newY = Math.max(0, e.y-10);
						if(service.getAppxLocation() != null && service.getAppxLocation().contains(e.x, newY)) {
							result = service;
							foundExactMatch = true;
							break;
						}
					}
				}
				
				if(foundExactMatch) {
					return result;
				} else {
					return null;
				}
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				boolean isMultiSelect = (e.stateMask & SWT.CTRL) != 0;
				
				AvailableService service = getServiceAtClickPos(e, availableServices);
				if(service != null && !isMultiSelect) {
					createNewServiceInstances(new AvailableService[] { service } );
				}
			}
			
			@Override
			public void mouseDown(MouseEvent e) { 
				boolean isMultiSelect = (e.stateMask & SWT.CTRL) != 0;
				
				AvailableService service = getServiceAtClickPos(e, availableServices);
				if(service != null/* && !isMultiSelect*/) {
					selectService(service, false, !isMultiSelect);
				}
			}
		}); // end mouse adapter

		result.setTabList(tabList);
		
		for(Control c : tabList) {
			c.addTraverseListener(traverseListener);
		}
	}
	
	private static List<AvailableService> utilGetVisibleServices(List<AvailableService> inputServices) {
		List<AvailableService> result = new ArrayList<AvailableService>();
		if(inputServices != null) {
			for(AvailableService s : inputServices) {
				if(s.getNameLabel() != null && s.getNameLabel().isVisible()) {
					result.add(s);
				}
			}		
		}
		
		return result;
	}
	
	private int getNumberOfVisibleServices() {
		return utilGetVisibleServices(availableServices).size();
	}
	
	/** Update service visibility, then layout.*/
	private void updateServiceList() {
		boolean emptyFilter = false;
		
		if(filterTerm == null || filterTerm.trim().length() == 0) {
			emptyFilter = true;
		}
		
		for(AvailableService service : availableServices) {
			boolean isVisible = emptyFilter || service.getName().toLowerCase().contains(filterTerm.toLowerCase()) || service.getDesc().toLowerCase().contains(filterTerm.toLowerCase()); 
			
			service.getImageLabel().setVisible(isVisible);
			service.getNameLabel().setVisible(isVisible);
			service.getDescLabel().setVisible(isVisible);
		}
		
		layoutList.layout();
	}
	
	/** Create one or more services instances, and adds them the right hand side of the wizard */
	private void createNewServiceInstances(AvailableService[] services) {
		for(AvailableService service : services) {
			
			List<CloudServicePlan> plans = service.getOffering().getCloudServicePlans();
			
			ServiceInstance si = new ServiceInstance(service.getName(), plans, service.getOffering());
			si.setUserDefinedName(stripBadCharsFromServiceName(service.getName()));
			
			CloudFoundryServiceWizardPageLeftPanel.this.parent.getRight().addNewServiceInstance(si);
		}
	}

	private static String stripBadCharsFromServiceName(String str) {
		if(str == null) { return null; }
		
		StringBuilder result = new StringBuilder();
		for(int x = 0; x < str.length(); x++) {
			
			char ch = str.charAt(x);
			
			if( Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == '$') {
				result.append(ch);
			}
		}
		
		return result.toString();
	}
	
	private static String trimTextAtBound(String str, int bound) {
		if (str.length() > bound) {
			/** Use the existing word wrap function to wrap at the spot we want. */
			String result = wrapAndTrimTextAtBound(str, bound);

			int index = result.indexOf("\n");
			if (index == -1) {
				return str;
			}
			else {
				return str.substring(0, index) + "...";
			}
		}
		else {
			return str;
		}

	}

	/** Simple text wrapper */
	private static String wrapAndTrimTextAtBound(String text, int bound) {
		StringBuilder result = new StringBuilder();
		String remainingString = text;

		// Last seen in the text
		int lastSpace = 0;

		for (int x = 0; x < remainingString.length(); x++) {
			if (remainingString.charAt(x) == ' ') {
				lastSpace = x;
			}
			if (x == bound) {
				// If there is no way to possible wrap it (e.g no spaces, or the
				// bound is too small) then don't bother
				if (lastSpace == 0) {
					return text;
				}

				String nextLine = remainingString.substring(0, lastSpace);
				remainingString = remainingString.substring(lastSpace).trim();
				result.append(nextLine);
				result.append("\n");
				x = 0;
				lastSpace = 0;
			}

		}

		result.append(remainingString.trim());

		return result.toString();

	}
	
	private void deselectServices(List<AvailableService> services) {
		for(AvailableService service : services) {
			service.getDescLabel().setBackground(layoutList.getBackground());
			service.getImageLabel().setBackground(layoutList.getBackground());
			service.getNameLabel().setBackground(layoutList.getBackground());
		}
	}
	
	public void dispose() {
		if (loader != null) {
			this.loader.dispose();
		}
	}
	
	/**
	 * Update the selection in the composite for the service (the behaviour differs on a number of factors, including whether or not the selected service was already selected)
	 * @param service The service to select
	 * @param scrollTo Whether or not to scroll to the position of the service in the scroll composite
	 * @param clearOtherSelections Whether or not to clear the other selections before selecting the service
	 */
	private void selectService(AvailableService service, boolean scrollTo, boolean clearOtherSelections) {
		
		if(!selectedServices.contains(service)) {
			// If the service was not previously selected...
			
			if(clearOtherSelections) {
				deselectServices(selectedServices);
				selectedServices.clear();
			}
			selectedServices.add(service);

		} else {
			// .. else the service _was_ previously selected
			if(clearOtherSelections) {
				deselectServices(selectedServices);
				selectedServices.clear();
				selectedServices.add(service);
				
			} else {
				selectedServices.remove(service);
				
				addButton.setEnabled(selectedServices.size() > 0);
				clearButton.setEnabled(selectedServices.size() > 0);
				
				service.getDescLabel().setBackground(layoutList.getBackground());
				service.getImageLabel().setBackground(layoutList.getBackground());
				service.getNameLabel().setBackground(layoutList.getBackground());
				service.getNameLabel().forceFocus();
				
				layoutList.redraw();
				return;
			}
			
		}
		
		addButton.setEnabled(selectedServices.size() > 0);
		clearButton.setEnabled(selectedServices.size() > 0);		

		service.getDescLabel().setBackground(selFillColor);
		service.getImageLabel().setBackground(selFillColor);
		service.getNameLabel().setBackground(selFillColor);
		
		service.getNameLabel().forceFocus();
		
		layoutList.redraw();
		
		if(service.getAppxLocation() != null && scrollTo) {
			
	        Rectangle bounds = service.getAppxLocation(); //child.getBounds();
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


	/** Clicking one of the service's widgets should select, or create it (depending on click type) */
	private class CFAvailableServiceListener extends MouseAdapter implements SelectionListener {

		AvailableService service;

		public CFAvailableServiceListener(AvailableService item) {
			this.service = item;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			boolean isMultiSelect = (e.stateMask & SWT.CTRL) != 0;
			
			selectService(service, false, !isMultiSelect);
		}
		
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			boolean isMultiSelect = (e.stateMask & SWT.CTRL) != 0;
			
			if(!isMultiSelect) {
				createNewServiceInstances(new AvailableService[] { service } );
			}
			
		}

		@Override
		public void mouseUp(MouseEvent e) {
			boolean isMultiSelect = (e.stateMask & SWT.CTRL) != 0;
			
			selectService(service, false, !isMultiSelect);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// Ignore.
		}		
	}


	/** Get the next visible service in the list, beginning at startPos (inclusive); returns null if none are visible */
	private static AvailableService utilGetNextVisibleService(List<AvailableService> services, int startPos) {
		AvailableService firstVisible = null;
		
		for(int x = startPos; x < services.size() && x >= 0 ; x++) {
			AvailableService as = services.get(x);
			if(as.getNameLabel() != null && as.getNameLabel().isVisible()) {
				firstVisible = as;
				break;
			}
		}
		
		return firstVisible;
	
	}

	
	/** Get previous visible service in the list, beginning at startPos (inclusive); returns null if none are visible, or startPos is out-of-bounds. */
	private static AvailableService utilGetPreviousVisibleService(List<AvailableService> services, int startPos) {
		AvailableService prevVisible = null;
		
		for(int x = startPos; x >= 0 && x < services.size(); x--) {
			AvailableService as = services.get(x);
			if(as.getNameLabel() != null && as.getNameLabel().isVisible()) {
				prevVisible = as;
				break;
			}
		}
		
		return prevVisible;
	}

	
	
	
	/** Implement fully custom tab-traversal behaviour, such that arrow keys are used to move the selection inside
	 * the service list, and the tab/shift-tab key will always exit the list (without changing the selection).
	 * 
	 * This ensures that:
	 * - If inside the service list:
	 * 		o Tab/Shift-Tab will always exit the list
	 * 		o Arrow keys can be used to move the selected service
	 * 		o Arrow keys will never exit the list
	 * 
	 * - IF outside the service list (filter button/field or add button):
	 * 		o Arrow keys cannot be used to move into the service list
	 * 		o Tab/Shift-Tab will move keyboard focus into the service list (where appropriate)
	 * 		o Moving keyboard focus into the service list will never reset the list selection index
	 *  */
	private class ServiceListTraverseListener implements TraverseListener {
				
		@Override
		public void keyTraversed(TraverseEvent e) {

			if(e.detail == SWT.TRAVERSE_ESCAPE) {
				e.doit = true;
				return;
			}
			
			if(e.detail == SWT.TRAVERSE_RETURN) {
				// Enter/return is used to add a service, so prevent the wizard from closing.
				e.doit = false;
				return;
			}
			
			// Arrow keys should only move up and down in the service list, and should not move the user out of the service list (this is tab's job)
			if(e.detail == SWT.TRAVERSE_ARROW_NEXT || e.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {
				e.doit = false;
				
				if(e.getSource() instanceof Label && e.getSource() != null && ((Label)e.getSource() ).getData() instanceof AvailableService ) {
					
					Label source = (Label)e.getSource();						
					AvailableService service = (AvailableService)source.getData();
					
					AvailableService visibleServiceAbove = utilGetPreviousVisibleService(availableServices, service.getListPosition()-1);
					AvailableService visibleServiceBelow = utilGetNextVisibleService(availableServices, service.getListPosition()+1);
					
					if( (visibleServiceAbove == null && e.detail == SWT.TRAVERSE_ARROW_PREVIOUS) 
							|| (visibleServiceBelow == null && e.detail == SWT.TRAVERSE_ARROW_NEXT)) {
						// If we are in the first or last position in the list, then ignore the traverse event. This prevents the user from using arrow keys to exit the service list.
						return;
					}
				}
			}
			
			// If the traversal was from filter text, theres a few special cases to handle
			if(e.getSource() == filterText) {
				
				if(getNumberOfVisibleServices() == 0) {
					// Special case: if the service list is empty (perhaps from filtering) then don't ignore tab traversal
					e.doit = true;
					return;
				}
				
				if(e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
					// ignore
				} else {
					AvailableService firstVisible = null;
					for(AvailableService as : availableServices) {
						if(as.getNameLabel() != null && as.getNameLabel().isVisible()) {
							firstVisible = as;
							break;
						}
					}
					
					if(selectedServices.size() == 0) {
						if(firstVisible != null) {
							selectService(firstVisible, true, true);
						}
					} else {
						if(firstVisible != null) {
							firstVisible.getNameLabel().forceFocus();
							selectService(firstVisible, true, true);
						}
						e.doit = false;
					}
				}
				
				return;
			}
			
			
			// Sanity check on our mouse event
			if(	! ( e.getSource() instanceof Label && e.getSource() != null && ((Label)e.getSource() ).getData() instanceof AvailableService)  ) {
				return;
			}
			
			// The service we are moving _from_
			AvailableService selectedService = (AvailableService)((Label)e.getSource() ).getData();
			
			if(e.detail == SWT.TRAVERSE_ARROW_NEXT || e.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {
			
				AvailableService service = null;
				
				int servicePos = -1;
				for(int x = 0; x < availableServices.size(); x++) {
					if(selectedService == availableServices.get(x)) {
						servicePos = x;
						break;
					}
				}
				
				if(servicePos != -1) {

					if(e.detail == SWT.TRAVERSE_ARROW_NEXT) {
					
//						// Find the next service in the list that is visible
						service = utilGetNextVisibleService(availableServices, servicePos+1);
						
					} else if(e.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {

						// Find the previous service in the list that is visible
						service = utilGetPreviousVisibleService(availableServices, servicePos-1);
					}
				}
				
				if(service != null) {
					// Move to the next/previous service in the list
					selectService(service, true, true);					
				}
			} else {
				// A TAB next/prev from one of the items in our service list

				e.doit = false;
				if(e.detail == SWT.TRAVERSE_TAB_NEXT) {
					clearButton.setFocus();
				} else if(e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
					filterText.setFocus();
				}
			}
		}
	}
}

/** Main layout manager for the service list composite */
class TextCompositeLayout extends Layout {
	
	/** Whether or not there we should make room for icons: only if an icon provider is available */
	final boolean isIconProviderAvailableForServerType;
	
	public TextCompositeLayout(boolean isIconProviderAvailableForServerType) {
		this.isIconProviderAvailableForServerType = isIconProviderAvailableForServerType;
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
	
	protected int[] layoutImpl(Composite composite, int wHint, boolean flushCache, boolean apply) {
			
		Control[] children = composite.getChildren();
		
		final int width = wHint > 0 ? wHint : composite.getParent().getClientArea().width;

		int x = 0;
		int y = 10;
		
		for(int c = 0; c < children.length; c+=3) {
			// initial index of 10
			x = 10;

			// We determine if the line is visible based on whether the title is visible
			boolean isVisible = children[c+1].getVisible();
			
			
			AvailableService service = (AvailableService)children[c].getData();

			// Skip filtered services
			if(!isVisible) {
				service.setAppxLocation(null);
				continue;
			}
			
			Point p;
			
			// Image label -- only used if an icon provider is set
			Label imageLabel = (Label)children[c];
			if(isIconProviderAvailableForServerType) {
				p = imageLabel.computeSize(32, 32, flushCache);
				if(apply) {
					imageLabel.setBounds(x, y+3, p.x, p.y);
				}

				x += 32 + 8; 
			}
			
			// Title
			Label titleLabel = (Label)children[c+1];
			p = titleLabel.computeSize(width-x-20, SWT.DEFAULT, flushCache);
			if(apply) {
				titleLabel.setBounds(x, y, p.x, p.y);
			}
			
			y+= p.y;
			
			// Description
			Label descLabel = (Label)children[c+2];
			p = descLabel.computeSize(width-x-20, SWT.DEFAULT, flushCache);
			if(apply) {
				descLabel.setBounds(x, y, p.x, p.y);
			}

			if(apply) {
				Rectangle topBounds = isIconProviderAvailableForServerType ? imageLabel.getBounds() : titleLabel.getBounds();
				Rectangle descLabelBounds = descLabel.getBounds();
				
				int rowHeight = (descLabelBounds.y + descLabelBounds.height) - topBounds.y;
				
				service.setAppxLocation(new Rectangle(topBounds.x, topBounds.y, width-20, rowHeight));				
				
			}
			y+= p.y;
			
			y+= 10;
		}		
		
		
		if(apply) {
			// This is necessary for cases where the user has changed the filter term, which will cause some widgets to no longer be visible;
			// in this scenario, our layout manager does not size invisible elements, thus the y value will be much smaller, and needs to be
			// updated here.
			composite.setSize(width, y);
		}
		
		return new int[]{ width, y };
	}
}

/** Store all services that are available to be created */
class AvailableService {
	private final String name;
	private final String desc;
	private Rectangle appxLocation;
	
	private Label nameLabel;
	private Label descLabel;
	private Label imageLabel;
	
	final private CloudServiceOffering offering;

	/** Position of the service in the unfiltered available services list */
	private int listPosition = -1;
	
	public AvailableService(String name, String desc, int listPosition, CloudServiceOffering offering) {
		super();
		this.name = name;
		this.desc = desc;
		this.listPosition = listPosition;
		this.offering = offering;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public Rectangle getAppxLocation() {
		return appxLocation;
	}
	
	public void setAppxLocation(Rectangle appxLocation) {
		this.appxLocation = appxLocation;
	}

	public Label getNameLabel() {
		return nameLabel;
	}

	public void setNameLabel(Label nameLabel) {
		this.nameLabel = nameLabel;
	}

	public Label getDescLabel() {
		return descLabel;
	}

	public void setDescLabel(Label descLabel) {
		this.descLabel = descLabel;
	}

	public Label getImageLabel() {
		return imageLabel;
	}

	public void setImageLabel(Label imageLabel) {
		this.imageLabel = imageLabel;
	}
	
	
	public int getListPosition() {
		return listPosition;
	}
	
	public CloudServiceOffering getOffering() {
		return offering;
	}
	
}

/** If an icon provider is specified, this thread will retrieve the service icons and set the label images as needed. */
class CFServiceWizardDynamicIconLoader extends Thread {

	private final Object lock = new Object();

	// The wizard page lets us know which icons we should be retrieving; we store them here for processing. List synchronized on lock
	private final List<ServiceWizardMapEntry> iconsToRetrieve = new ArrayList<ServiceWizardMapEntry>();

	private final CloudFoundryServer server;

	private final ICloudFoundryServiceWizardIconProvider iconProvider;

	private boolean isRunning = true;

	// Synchronize on imageMap when accessing it
	private Map<String /* service id + provider */, Image> imageMap = new HashMap<String, Image>();

	public CFServiceWizardDynamicIconLoader(ICloudFoundryServiceWizardIconProvider iconProvider, CloudFoundryServer server) {
		super(CFServiceWizardDynamicIconLoader.class.getName());
		setDaemon(true);
		this.iconProvider = iconProvider;
		this.server = server;
	}

	/**
	 * Add icon to the front of the list, for icons the user is currently viewing
	 */
	public void addIconToFrontOfRetrieveList(CloudServiceOffering offering, Label imageLabel) {
		synchronized (lock) {

			// Add to front (this may create a duplicate; dupes are checked in IconRetrieveRunnable)
			ServiceWizardMapEntry me = new ServiceWizardMapEntry(offering, imageLabel);
			iconsToRetrieve.add(0, me);
			lock.notifyAll();
		}
	}

	/** Add icon to end of list */
	public void addIconToRetrieveList(CloudServiceOffering offering, Label imageLabel) {
		ServiceWizardMapEntry me = new ServiceWizardMapEntry(offering, imageLabel);
		synchronized (lock) {
			iconsToRetrieve.add(me);
			lock.notifyAll();
		}
	}

	public void run() {
		// Have up to 10 URL requests running at a time
		ExecutorService es = Executors.newFixedThreadPool(10);

		while (isRunning) {

			try {

				List<ServiceWizardMapEntry> localIconsToRetrieve = null;

				synchronized (lock) {
					lock.wait(1000);

					if (isRunning && iconsToRetrieve.size() > 0) {

						localIconsToRetrieve = new ArrayList<ServiceWizardMapEntry>();
						localIconsToRetrieve.addAll(iconsToRetrieve);
						iconsToRetrieve.clear();
					}
				}

				if (isRunning && localIconsToRetrieve != null) {
					// Process icon requests outside the lock

					for (ServiceWizardMapEntry e : localIconsToRetrieve) {
						IconRetrieveRunnable r = new IconRetrieveRunnable(e);
						es.execute(r);
					}

					localIconsToRetrieve.clear();
				}

			}
			catch (InterruptedException e) {
				isRunning = false;
			}
		}

		es.shutdownNow();

		try {
			// Wait at most 30 seconds for the remaining tasks to finish
			es.awaitTermination(30, TimeUnit.SECONDS);
		}
		catch (InterruptedException e1) {
			// ignore.
		}

		synchronized (imageMap) {
			// Dispose of old images
			Set<Entry<String, Image>> s = imageMap.entrySet();
			for (Entry<String, Image> e : s) {
				if (e.getValue().isDisposed()) {
					e.getValue().dispose();
				}
			}
		}

	}

	public void dispose() {
		synchronized (lock) {
			isRunning = false;
		}
	}

	/** This runnable is run on many threads at a time; it retrieves the result from the icon provider and sets the icon image label */
	private class IconRetrieveRunnable implements Runnable {

		private final ServiceWizardMapEntry entry;

		public IconRetrieveRunnable(ServiceWizardMapEntry entry) {
			this.entry = entry;
		}

		public void run() {

			CloudServiceOffering cso = entry.getOffering();
			final String mapId = "" + cso.getName() + "-" + cso.getProvider(); //$NON-NLS-1$ //$NON-NLS-2$

			Image result = null;

			// Check the cache for the image
			synchronized (imageMap) {
				Image i = imageMap.get(mapId);
				if (i != null) {
					result = i;
				}
			}

			// Grab the image from the provider, if needed
			if (result == null) {
				Image img = null;
				ImageDescriptor imageDesc = iconProvider.getServiceIcon(entry.getOffering(), server);
				
				if(imageDesc != null) {
					img = imageDesc.createImage(false);
				}
				
				if(img == null) {
					// An error occurred while trying to create the image (for example, bad URL), 
					// OR the getServiceIcon(...) call returned null, so request a replacement icon.
					imageDesc = iconProvider.getDefaultServiceIcon(entry.getOffering(), server);
					if (imageDesc != null) {
						img = imageDesc.createImage();
					}
				}
					
				if(img != null) {
					final Image resizeImg = resizeImage(img, 32, 32);
					result = resizeImg;
					synchronized (imageMap) {
						imageMap.put(mapId, resizeImg);
					}
				}
			}

			final Image labelImage = result;

			if (labelImage == null)
				return;

			// Replace the image label
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					Label l = entry.getImageLabel();

					if (!l.isDisposed()) {
						l.setImage(labelImage);
					}
				}
			});
		}

		@SuppressWarnings("cast")
		private Image resizeImage(Image oldImage, int newWidth, int newHeight) {
			
			Rectangle oldImageBounds = oldImage.getBounds();
						
			if(oldImage.getBounds().width > oldImage.getBounds().height) {
				// width > height
				double scaleRatio = ((double)newWidth/(double)oldImageBounds.width);
				newHeight = (int)  (	scaleRatio * (double)oldImageBounds.height);
				
			} else {
				// heigh > width, or equal
				double scaleRatio = ((double)newHeight/(double)oldImageBounds.height);
				newWidth = (int)  (	scaleRatio * (double)oldImageBounds.width );				
			}
			
			Image newImage = new Image(Display.getDefault(), newWidth, newHeight);
			GC gc = new GC(newImage);
			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.HIGH);
			gc.drawImage(oldImage, 0, 0, oldImageBounds.width, oldImageBounds.height, 0, 0, newWidth, newHeight);
			gc.dispose();
			oldImage.dispose();
			return newImage;
		}

	}

	private static class ServiceWizardMapEntry {
		private CloudServiceOffering offering;

		private Label imageLabel;

		public ServiceWizardMapEntry(CloudServiceOffering offering, Label imageLabel) {
			super();
			this.offering = offering;
			this.imageLabel = imageLabel;
		}

		public CloudServiceOffering getOffering() {
			return offering;
		}

		public Label getImageLabel() {
			return imageLabel;
		}

	}

}