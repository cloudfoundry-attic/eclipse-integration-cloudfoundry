/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software Inc and IBM Corporation. 
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.LocalCloudService;
import org.cloudfoundry.ide.eclipse.server.ui.ICloudFoundryServiceWizardIconProvider;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IRuntime;

public class CloudFoundryServiceWizardPage1 extends WizardPage {
	protected static final String PAGE_NAME = CloudFoundryServiceWizardPage1.class.getName();

	protected ScrolledComposite scrollComp;

	protected String filter;

	protected Font boldFont;

	protected Color selEdgeColor;

	protected Color selFillColor;

	protected Text nameText;

	protected boolean updating = false;

	private List<CloudServiceOffering> serviceOfferings;

	private List<CFServiceWizUI> allServicesList = new ArrayList<CFServiceWizUI>();

	private List<CFServiceWizUI> selectedServicesList = new ArrayList<CFServiceWizUI>();

	CFWizServicePage1Validation servicePageValidation;

	CloudFoundryServer cloudServer;

	protected Group serviceInfoComposite;
	
	public LocalCloudService getService() {
		
		List<CFServiceWizUI> selectedServices = getSelectedList();
		if(selectedServices != null && selectedServices.size() == 1) {
			return selectedServices.get(0).convertToLocalCloudService();
		}
		
		return null;
	}
	

	public static final String INSTALL_SERVICE_TEXT = "Select a service to include in this installation";
	public static final String DEFAULT_FILTER_TEXT = "type filter text";
	public static final String REMOVE_SERVICE_TEXT = "Remove";
	public static final String ADD_SERVICE_TEXT = "Select";
	public static final String DESCRIPTION_FINAL = "Click finish to add the service.";
	public static final String FILTER_TEXT = "Filter:";

	/** Optional field -- used to provide icons in service wizard if available*/
	CFServiceWizardDynamicIconLoader loader;
	
	
    private FontMetrics fontMetrics;	
	
	public CloudFoundryServiceWizardPage1(CloudFoundryServer cloudServer) {
		super(PAGE_NAME);
		this.cloudServer = cloudServer;
		setTitle("Service Configuration");
		setDescription(DESCRIPTION_FINAL);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
		this.cloudServer = cloudServer;

		// Is there an icon provide for the service, if so, use it
		IRuntime runtime = cloudServer.getServer().getRuntime();
		String runtimeTypeId = runtime.getRuntimeType().getId();
		
		ICloudFoundryServiceWizardIconProvider provider = CloudFoundryServiceWizardIconProviderRegistry.getInstance().getIconProvider(runtimeTypeId);		
		if(provider != null) {
			loader = new CFServiceWizardDynamicIconLoader(provider, this.cloudServer);
			loader.start();
		}
		
		servicePageValidation = new CFWizServicePage1Validation(this);
		setPageComplete(false);

	}

	private static void sortServicePlans(List<CloudServiceOffering> configurations) {

		for (CloudServiceOffering offering : configurations) {
			Collections.sort(offering.getCloudServicePlans(), new Comparator<CloudServicePlan>() {
				public int compare(CloudServicePlan o1, CloudServicePlan o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
		}
	}

	private boolean updateConfiguration() {
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

			if (serviceOfferings != null) {
				for (CloudServiceOffering o : serviceOfferings) {
					if (!allServicesList.contains(new CFServiceWizUI(o))) {
						allServicesList.add(new CFServiceWizUI(o));
					}
				}
			}

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
	
	Composite topComp;

	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);

		topComp = comp;
		
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(comp, 4);
		layout.verticalSpacing = convertVerticalDLUsToPixels(comp, 4);
		layout.numColumns = 3;
		comp.setLayout(layout);

		GridData data;

		Composite filterComp = new Composite(comp, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(comp, 4);
		layout.verticalSpacing = convertHorizontalDLUsToPixels(comp, 4);
		filterComp.setLayout(layout);
		data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		data.horizontalSpan = 3;
		data.horizontalIndent = 0;
		data.verticalIndent = 0;
		filterComp.setLayoutData(data);

		final Label filterLabel = new Label(filterComp, SWT.NONE);
		filterLabel.setText(FILTER_TEXT);
		data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
		filterLabel.setLayoutData(data);

		final Text filterText = new Text(filterComp, SWT.SEARCH);
		filterText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));
		filterText.setText(DEFAULT_FILTER_TEXT);

		
		
		scrollComp = new ScrolledComposite(comp, SWT.BORDER | SWT.V_SCROLL);
		data = new GridData(GridData.FILL, GridData.FILL, true, true, 3, 1);
		data.heightHint = 300;
		scrollComp.setLayoutData(data);
		scrollComp.setAlwaysShowScrollBars(true);
		scrollComp.setBackground(scrollComp.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		int lineHeight = filterText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		scrollComp.getVerticalBar().setPageIncrement(lineHeight * 3);
		scrollComp.getVerticalBar().setIncrement(lineHeight);

		scrollComp.addControlListener(new ControlAdapter() {
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

		Font font = comp.getFont();
		if (boldFont == null) {
			Display display = getShell().getDisplay();
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
		}

		comp.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent event) {
				if (boldFont != null) {
					boldFont.dispose();
				}
				if (selEdgeColor != null) {
					selEdgeColor.dispose();
				}
				if (selFillColor != null) {
					selFillColor.dispose();
				}
			}
		});

		filterText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent event) {
				if (updating) {
					return;
				}

				String text = filterText.getText();
				if (DEFAULT_FILTER_TEXT.equals(text)) {
					filter = null;
				}
				else {
					filter = filterText.getText();
				}
				updateServicesTable();
			}
		});

		filterText.addFocusListener(new FocusListener() {

			public void focusLost(FocusEvent e) {
				if (filterText.getText().isEmpty()) {
					updating = true;
					filterText.setText(DEFAULT_FILTER_TEXT);
					updating = false;
				}
			}

			public void focusGained(FocusEvent e) {
				if (DEFAULT_FILTER_TEXT.equals(filterText.getText())) {
					updating = true;
					filterText.setText("");
					updating = false;
				}
			}
		});
		
		
		
		
		serviceInfoComposite = new Group(comp, SWT.NONE);
		serviceInfoComposite.setText("Specify a service name and service plan");
		data = new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1);
		data.verticalIndent = 5;
		serviceInfoComposite.setLayoutData(data);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		serviceInfoComposite.setLayout(layout);		

		serviceInfoComposite.setVisible(false);		
		
		servicePageValidation.updatePageState();
		Dialog.applyDialogFont(comp);
		setControl(comp);

		MouseWheelListener mwl = new MouseWheelListener() {

			public void mouseScrolled(MouseEvent event) {
				scrollComp.setFocus();
			}
		};

		filterText.addMouseWheelListener(mwl);
		// nameText.addMouseWheelListener(mwl);

	}

	@Override
	public void setVisible(boolean isVis) {
		super.setVisible(isVis);

		if (isVis) {
			// Set focus on scrolled composite whenever we become visible to
			// prevent missing mouse wheel events
			scrollComp.setFocus();

			updateConfiguration();
			updateServicesTable();

		}
	}

	protected void updateServicesTable() {
		
		Control content = scrollComp.getContent();
		if (content != null) {
			content.dispose();
		}

		if (allServicesList.isEmpty()) {
			Composite labelComp = new Composite(scrollComp, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.horizontalSpacing = 8;
			layout.verticalSpacing = 12;
			labelComp.setLayout(layout);
			labelComp.setBackground(scrollComp.getBackground());

			Label descriptionLabel = new Label(labelComp, SWT.NONE);
			descriptionLabel.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
			descriptionLabel.setBackground(scrollComp.getBackground());
			descriptionLabel.setText("No available services.");
			scrollComp.setContent(labelComp);
			labelComp.setSize(labelComp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			return;
		}

		final Composite comp = new Composite(scrollComp, SWT.NONE);
		comp.setBackground(scrollComp.getBackground());
		
		comp.setLayout(new CFServiceWizardLayout(this));

		comp.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent event) {
				List<CFServiceWizUI> selected = selectedServicesList;
				
				if(selected != null && selected.size() == 1) {
					
					Rectangle r = selected.get(0).getAppxLocation();
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
		});

		for (final CFServiceWizUI item : allServicesList) {
			Color c = null;

			boolean areWeInSelectedList = selectedServicesList.contains(item);

			if (areWeInSelectedList) {
				c = selFillColor;
			}
			else {
				c = comp.getBackground();
			}

			if (filter == null || filter.isEmpty() || matches(item, filter)) {
				final Label imgLabel = new Label(comp, SWT.NONE);
				imgLabel.setBackground(c);
				imgLabel.setData(item);
				if(loader != null) {
//					// Uncomment out these lines for dynamic icon acquisition
//					imgLabel.addPaintListener(new PaintListener() {
//						
//						public void paintControl(PaintEvent e) {
//							
//							if(imgLabel.getImage() == null) {
//								loader.addIconToRetrieveList(item.getOffering(), imgLabel);
//							}
//						}
//					});
					
					loader.addIconToRetrieveList(item.getOffering(), imgLabel);
				}

				final Label nameLabel = new Label(comp, SWT.WRAP);
				nameLabel.setFont(boldFont);
				nameLabel.setBackground(c);
				nameLabel.setText(item.getName());

				nameLabel.setData(item);

				final Label descLabel = new Label(comp, SWT.WRAP);
				descLabel.setBackground(c);
				String desc = item.getDescription();
				if (desc == null) {
					desc = "";
				}
				descLabel.setText(trimTextAtBound(desc, 200));

				descLabel.setToolTipText(wrapAndTrimTextAtBound(desc, 100));
				descLabel.setData(item);

				String hyperLink = null;
				if (hyperLink == null) {
					hyperLink = item.getOffering().getUrl();
				}
				if (hyperLink == null) {
					hyperLink = item.getOffering().getInfoUrl();
				}
				if (hyperLink == null) {
					hyperLink = item.getOffering().getDocumentationUrl();
				}

				// final String hyperLinkFinal = hyperLink;

				final Hyperlink hyperLinkLabel = new Hyperlink(comp, SWT.WRAP);
				hyperLinkLabel.setBackground(c);
				hyperLinkLabel.setText("");
				hyperLinkLabel.setVisible(false);
				// if(hyperLink != null) {
				// hyperLinkLabel.setText("(link)");
				// hyperLinkLabel.setForeground(comp.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
				// hyperLinkLabel.setUnderlined(true);
				// hyperLinkLabel.addHyperlinkListener(new HyperlinkAdapter() {
				// public void linkActivated(HyperlinkEvent e) {
				// if(hyperLinkFinal != null) {
				// org.eclipse.swt.program.Program.launch(hyperLinkFinal);
				// }
				// }
				// });
				// }

				CFWizSelectItemListener itemListener = new CFWizSelectItemListener(item);
				
				imgLabel.addMouseListener(itemListener);
				nameLabel.addMouseListener(itemListener);
				descLabel.addMouseListener(itemListener);

			} else {
				// Filtered out items have no location
				item.setAppxLocation(null);
			}
 		}

		comp.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent e) { 
				
				for (final CFServiceWizUI item : allServicesList) {
					if(item.getAppxLocation().contains(e.x, e.y)) {
						selectItem(item);
						break;
					}
					
				}
				
			}

			public void mouseDoubleClick(MouseEvent arg0) {
				// Ignore
			}

			public void mouseUp(MouseEvent arg0) {
				// Ignore
			}
			
		});
		
		scrollComp.setContent(comp);

		Rectangle r = scrollComp.getClientArea();
		r.height = comp.computeSize(r.width, SWT.DEFAULT).y;
		comp.setBounds(r);

				
		for(Control c: serviceInfoComposite.getChildren()) {
			c.dispose();
		}
	
		createServiceInfoComposite();
		
	
		serviceInfoComposite.layout();
		topComp.layout();
		
		servicePageValidation.updatePageState();

	}
	
	private void createServiceInfoComposite() {
		
		String name = "";
		
		final CFServiceWizUI selectedItemFinal;
		if(selectedServicesList.size() == 1) {
			 selectedItemFinal = selectedServicesList.get(0);
			 name = selectedItemFinal.getName();
		} else {
			selectedItemFinal = null;
		}
		
		
		serviceInfoComposite.setEnabled(selectedItemFinal != null);
		
		final Label serviceNameLabel = new Label(serviceInfoComposite, SWT.NONE);
		serviceNameLabel.setText("Name:");
		GridData data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
		serviceNameLabel.setLayoutData(data);
		serviceNameLabel.setEnabled(selectedItemFinal != null);

		final Text serviceNameText = new Text(serviceInfoComposite, SWT.SEARCH);
		serviceNameText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 1, 1));
		serviceNameText.setText(utilRemoveSpaces(name));
		serviceNameText.setEnabled(selectedItemFinal != null);
		
		if(selectedItemFinal != null) {
			selectedItemFinal.setUserDefinedName(utilRemoveSpaces(name));
		}
		serviceNameText.addModifyListener(new ModifyListener() { 
			
			public void modifyText(ModifyEvent e) {
				
				String text = serviceNameText.getText();
				if(selectedItemFinal != null) {
					selectedItemFinal.setUserDefinedName(text);
				}
				servicePageValidation.updatePageState();
			}
			
		});
		
		final Label servicePlanLabel = new Label(serviceInfoComposite, SWT.NONE);
		servicePlanLabel.setText("Plan:");
		servicePlanLabel.setEnabled(selectedItemFinal != null);
		data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
		servicePlanLabel.setLayoutData(data);
		@SuppressWarnings("unchecked")
		final List<CloudServicePlan> plans = selectedItemFinal != null ? selectedItemFinal.getOffering().getCloudServicePlans() : Collections.EMPTY_LIST;
		
		String[] items = new String[plans.size()];
		for(int x = 0; x < items.length; x++) {
			items[x] = plans.get(x).getName();
		}
		
		final Combo typeCombo = new Combo(serviceInfoComposite, SWT.READ_ONLY | SWT.BORDER);
		typeCombo.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 1, 1));
		typeCombo.setItems(items);
		typeCombo.select(0);
		typeCombo.setEnabled(selectedItemFinal != null);
		
		
		typeCombo.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				if(selectedItemFinal != null) {
					selectedItemFinal.setPlan(plans.get(typeCombo.getSelectionIndex()));
				}
			}

			
			public void widgetDefaultSelected(SelectionEvent e) {
				// Ignore.
			}
		});
		
		serviceInfoComposite.setVisible(true);

	}
	
	
	private static String utilRemoveSpaces(String str) {
		if(str == null) {
			return null;
		} else {
			return str.replace(" ", "");
		}
	}

	private void selectItem(CFServiceWizUI item) {

		if (selectedServicesList.contains(item)) {
			selectedServicesList.remove(item);
			
		} else  {
			
			selectedServicesList.clear();
			
			selectedServicesList.add(item);
	
			// If the plan has not been set, use the first
			if (item.getPlan() == null) {
				List<CloudServicePlan> plans = item.getOffering().getCloudServicePlans();
				if (plans != null && plans.size() > 0) {
					item.setPlan(plans.get(0));
				}
			}

		}

		servicePageValidation.updatePageState();

		Point p = scrollComp.getOrigin();

		updateServicesTable();

		scrollComp.setFocus();

		scrollComp.setOrigin(p);			

	}

	class CFWizSelectItemListener implements SelectionListener, MouseListener {

		CFServiceWizUI service;

		public CFWizSelectItemListener(CFServiceWizUI item2) {
			this.service = item2;
		}

		public void widgetSelected(SelectionEvent event2) {
			selectItem(service);

		}

		public void mouseDoubleClick(MouseEvent arg0) {
			// Ignore.
		}

		public void mouseDown(MouseEvent arg0) {
			selectItem(service);

		}

		public void mouseUp(MouseEvent arg0) {
			// Ignore.
		}

		public void widgetDefaultSelected(SelectionEvent arg0) {
			// Ignore.
		}

	}

	protected boolean matches(CFServiceWizUI item, String filter) {
		return item.getName().toLowerCase().contains(filter.toLowerCase())
				|| item.getDescription().toLowerCase().contains(filter.toLowerCase());
	}

	public String getServiceName() {
		return nameText.getText();
	}

	public List<CFServiceWizUI> getSelectedList() {
		return selectedServicesList;
	}
	
    protected void initializeDialogUnits(Control testControl) {
        // Compute and store a font metric
        GC gc = new GC(testControl);
        gc.setFont(JFaceResources.getDialogFont());
        fontMetrics = gc.getFontMetrics();
        gc.dispose();
    }

    
    public int convertHorizontalDLUsToPixels(Composite comp, int x) {
        if (fontMetrics == null) {
            initializeDialogUnits(comp);
        }
        return Dialog.convertHorizontalDLUsToPixels(fontMetrics, x);
    }

    public int convertVerticalDLUsToPixels(Composite comp, int y) {
        if (fontMetrics == null) {
            initializeDialogUnits(comp);
        }
        return Dialog.convertVerticalDLUsToPixels(fontMetrics, y);
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
	
	protected boolean isIconProviderAvailable() {
		return loader != null;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if(loader != null) {
			this.loader.dispose();
		}
	}
}

class CFServiceWizUI {

	private CloudServiceOffering offering;

	private Button button;

	private Rectangle appxLocation;

	private String userDefinedName = "";

	private CloudServicePlan plan = null;

	public CFServiceWizUI(CloudServiceOffering offering) {
		this.offering = offering;
	}

	public String getName() {
		return offering.getName();
	}

	public String getDescription() {
		return offering.getDescription();
	}

	public Button getButton() {
		return button;
	}

	public void setButton(Button button) {
		this.button = button;
	}

	public Rectangle getAppxLocation() {
		return appxLocation;
	}

	public void setAppxLocation(Rectangle appxLocation) {
		this.appxLocation = appxLocation;
	}

	public String getUserDefinedName() {
		return userDefinedName;
	}

	public void setUserDefinedName(String userDefinedName) {
		this.userDefinedName = userDefinedName;
	}

	public CloudServiceOffering getOffering() {
		return offering;
	}

	public CloudServicePlan getPlan() {
		return plan;
	}

	public void setPlan(CloudServicePlan plan) {
		this.plan = plan;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CFServiceWizUI)) {
			return false;
		}
		CloudServiceOffering offering = ((CFServiceWizUI) o).getOffering();
		String name1 = "" + getOffering().getName();
		String name2 = "" + offering.getName();

		return name1.equals(name2);
	}

	public LocalCloudService convertToLocalCloudService() {
		final LocalCloudService localService = new LocalCloudService("");
		localService.setName(getUserDefinedName());
		localService.setVersion(getOffering().getVersion());
		localService.setLabel(getOffering().getLabel());
		localService.setProvider(getOffering().getProvider());
		localService.setPlan(getPlan().getName());
		return localService;
	}

}

class CFServiceWizardLayout extends Layout {
	private static final int BORDER = 5;

	private static int IMG_DEFAULT = 32;

	private static final int GAP = 3;

	private static final int V_SPACE = 3;

	protected int colWidth = -1;


	CloudFoundryServiceWizardPage1 parent;
	
	public CFServiceWizardLayout(CloudFoundryServiceWizardPage1 parent) {
		this.parent = parent;
	}
	
	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
		int[] result = layoutImpl(composite, 0, flushCache, false);
		
		return new Point(result[0], result[1]);
	}

	@Override
	protected void layout(Composite composite, boolean flushCache) {
		int[] result = layoutImpl(composite, 0, flushCache, true);
		
		// Adjust the height to match the elments inside
		Rectangle r = composite.getBounds();
		r.height = result[1];
		composite.setBounds(r);
	}


	protected int[] layoutImpl(Composite composite, int wHint, boolean flushCache, boolean apply) {
		Rectangle r = composite.getClientArea();
		int areaW = r.width > 0 ? r.width : wHint;
		int i = 0;
		int x = r.x + BORDER;
		int y = r.y + BORDER;
		int w = 0;
		if (areaW == 0) {
			w = composite.getParent().getBounds().width - composite.getParent().getBorderWidth() * 2;
		} else {
			w = areaW - BORDER * 2;
		}
		w = Math.max(400, w);

		int bottomMostY = -1;
		int rightMostX = -1;
		
		boolean iconsAvailable = parent.isIconProviderAvailable();

		int IMG;
		if(iconsAvailable) {
			IMG = IMG_DEFAULT;
		} else {
			IMG = 0;
		}
		
		// children are: image, name, description, size
		Control[] children = composite.getChildren();

		final int initialX = x;

		while (i < children.length) {
			
			x = initialX;
			
			if (i > 0) {
				y += V_SPACE;
			}

			
			Label localIconLabel = (Label)children[i]; 
			
			Label localNameLabel = (Label)children[i + 1];
			
			Rectangle br = new Rectangle(x, y, w, 5);
			CFServiceWizUI product = (CFServiceWizUI) localNameLabel.getData();

			
			
			// image
			if (apply) {
				localIconLabel.setBounds(x, y, IMG, IMG);
			}
			
			if (iconsAvailable) {
				x += 3;
			}
			

			// label
			 
			Point p = localNameLabel.computeSize(w - IMG - GAP, SWT.DEFAULT, flushCache);
			if (apply) {
				localNameLabel.setBounds(x + IMG + GAP, y, p.x, p.y);
			}

			y += p.y + GAP;

			// hyperlink
			Hyperlink localLink = (Hyperlink)children[i+3];
			p = localLink.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
			if (apply) {
				localLink.setBounds(x + w - p.x - 2, y, p.x, p.y);
			}

			// description
			Label localDescr = (Label)children[i+2];
			p = localDescr.computeSize(w - IMG - GAP * 2, SWT.DEFAULT, flushCache);
			if (apply) {
				localDescr.setBounds(x + IMG + GAP, y, p.x, p.y);
			}
			
			y = y + p.y;

			Rectangle imgLabelRect = localIconLabel.getBounds();
			int topLeftX = imgLabelRect.x;
			int topLeftY = imgLabelRect.y;

			Rectangle descLabelRect = localDescr.getBounds();
			int botRightX = descLabelRect.x + descLabelRect.width;
			int botRightY = descLabelRect.y + descLabelRect.height;

			bottomMostY = botRightY;
			rightMostX = botRightX;
			
			Rectangle rowPos = new Rectangle(topLeftX, topLeftY, botRightX - topLeftX, botRightY - topLeftY);
			if(apply) {
				product.setAppxLocation(rowPos);
			}

			y += BORDER;
			i += 4;
			
		}
		
		bottomMostY += BORDER;
		
		return new int[] {rightMostX, bottomMostY};
	}
}

class CFWizServicePage1Validation {

	private static Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+");
	
	CloudFoundryServiceWizardPage1 wizardPage;

	public CFWizServicePage1Validation(CloudFoundryServiceWizardPage1 wizardPage) {
		this.wizardPage = wizardPage;
	}

	public void updatePageState() {

		boolean descriptionUpdated = false;

		if (!descriptionUpdated) {

			List<CFServiceWizUI> list = wizardPage.getSelectedList();
			if (list == null || list.size() == 0) {
				wizardPage.setDescription("Select a service from the service list.");
				wizardPage.setErrorMessage(null);
				descriptionUpdated = true;
			}

		}
		
		
		if(!descriptionUpdated) {
			List<CFServiceWizUI> list = wizardPage.getSelectedList();
			if (list != null && list.size() == 1) {
				CFServiceWizUI service = list.get(0);
				
				String userDefinedName = service.getUserDefinedName();
				
				if(service != null && userDefinedName != null ) {

					if(userDefinedName.trim().length() == 0) {
						wizardPage.setDescription("Specify a service name");
						wizardPage.setErrorMessage(null);
						descriptionUpdated = true;						
					}

					
					Matcher matcher = VALID_CHARS.matcher(userDefinedName);
					if (!descriptionUpdated && !matcher.matches()) {
						wizardPage.setErrorMessage("The service name contains invalid characters.");
						wizardPage.setDescription(null);
						descriptionUpdated = true;
					}
					
				}
	
			}
			
		}
		

		if (!descriptionUpdated) {
			wizardPage.setErrorMessage(null);
			wizardPage.setDescription(CloudFoundryServiceWizardPage1.DESCRIPTION_FINAL);
			wizardPage.setPageComplete(true);
		}
		else {
			wizardPage.setPageComplete(false);
		}

	}

}


class CFServiceWizardDynamicIconLoader extends Thread {
	
	private final Object lock = new Object();
	
	// List synchronized on lock
	private List<ServiceWizardMapEntry> iconsToRetrieve = new ArrayList<ServiceWizardMapEntry>();
	
	private CloudFoundryServer server;
	
	private ICloudFoundryServiceWizardIconProvider iconProvider;
	
	private boolean isRunning = true;
	
	// Synchronize on imageMap when accessing it
	private Map<String /*service id + provider*/, Image> imageMap = new HashMap<String /*offering id*/, Image>();
	
	public CFServiceWizardDynamicIconLoader(ICloudFoundryServiceWizardIconProvider iconProvider, CloudFoundryServer server) {
		super(CFServiceWizardDynamicIconLoader.class.getName());
		setDaemon(true);
		this.iconProvider = iconProvider;
		this.server = server;
	}

	
	/** Add icon to the front of the list, for icons the user is currently viewing */
	public void addIconToFrontOfRetrieveList(CloudServiceOffering offering, Label imageLabel) {
		synchronized(lock) {

			// Add to front (this may create a duplicate; dupes are checked in IconRetrieveRunnable)
			ServiceWizardMapEntry me = new ServiceWizardMapEntry(offering, imageLabel);
			iconsToRetrieve.add(0, me);
			lock.notifyAll();
		}
	}


	/** Add icon to end of list */
	public void addIconToRetrieveList(CloudServiceOffering offering, Label imageLabel) {
		ServiceWizardMapEntry me = new ServiceWizardMapEntry(offering, imageLabel);
		synchronized(lock) {
			iconsToRetrieve.add(me);
			lock.notifyAll();
		}
	}
	
	
	public void run() {
		// Have up to 5 URL requests running at a time
		ExecutorService es = Executors.newFixedThreadPool(5);
		
		while(isRunning) {
			
			try {
				
				List<ServiceWizardMapEntry> localIconsToRetrieve = null;
				
				synchronized(lock) {
					lock.wait(1000);

					if(isRunning && iconsToRetrieve.size() > 0) {
						
						localIconsToRetrieve = new ArrayList<ServiceWizardMapEntry>();
						localIconsToRetrieve.addAll(iconsToRetrieve);
						iconsToRetrieve.clear();						
					}
					
				}
				
				if(isRunning && localIconsToRetrieve != null) {
					// Process icon requests outside the lock
					
					for(ServiceWizardMapEntry e : localIconsToRetrieve) {
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
			// Wait at most 10 seconds for the remaining tasks to finish
			es.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			// ignore.
		}

		synchronized (imageMap) {
			// Dispose of old images
			Set<Entry<String, Image>> s = imageMap.entrySet();
			for(Entry<String, Image> e : s) {
				if(e.getValue().isDisposed()) {
					e.getValue().dispose();
				}
			}			
		}

		
	}
	
	public void dispose() {
		synchronized(lock) {
			isRunning = false;
		}
	}
	
	
	private class IconRetrieveRunnable implements Runnable {
				
		ServiceWizardMapEntry entry;

		public IconRetrieveRunnable(ServiceWizardMapEntry entry) {
			this.entry = entry;
		}
		
		public void run() {
			
			CloudServiceOffering cso = entry.getOffering();
			final String mapId = ""+cso.getName()+"-"+cso.getProvider(); 
			
			Image result = null;
			
			// Check the cache for the image
			synchronized(imageMap) {
				Image i = imageMap.get(mapId);
				if(i != null) {
					result = i;
				}
			}
			
			// Grab the image from the provider, if needed
			if(result == null) {
				ImageDescriptor imageDesc = iconProvider.getServiceIcon(entry.getOffering(), server);
				
				if(imageDesc != null) {
					Image img = imageDesc.createImage();
					final Image resizeImg = resizeImage(img, 32, 32);
					result = resizeImg;
					synchronized(imageMap) {
						imageMap.put(mapId, resizeImg);
					}
				}
			}
			
			final Image labelImage = result;
			
			if(labelImage == null) return;
						
			// Replace the image label
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					Label l = entry.getImageLabel();
					
					if(!l.isDisposed()) {
						l.setImage(labelImage);						
					}
				}
			});
				
		}
		
		private Image resizeImage(Image oldImage, int newWidth, int newHeight) {
			Image newImage = new Image(Display.getDefault(), newWidth, newHeight);
			GC gc = new GC(newImage);
			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.HIGH);
			gc.drawImage(oldImage, 0, 0, oldImage.getBounds().width, oldImage.getBounds().height, 0, 0, newWidth, newHeight);
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