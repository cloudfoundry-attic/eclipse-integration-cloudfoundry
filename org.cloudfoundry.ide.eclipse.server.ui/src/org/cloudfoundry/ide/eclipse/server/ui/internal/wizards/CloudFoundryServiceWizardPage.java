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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Logger;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.statushandlers.StatusManager;

public class CloudFoundryServiceWizardPage extends WizardPage {

	final CloudFoundryServiceWizard parent;
	
	private CloudFoundryServiceWizardPageLeftPanel leftPanel;
	private CloudFoundryServiceWizardPageRightPanel rightPanel;
	
	private CloudFoundryServer cloudServer;

	Composite topComp;
	
	public CloudFoundryServiceWizardPage(CloudFoundryServer cloudServer, CloudFoundryServiceWizard parent) {
		super(CloudFoundryServiceWizardPage.class.getName());
		
		this.parent = parent;
		this.cloudServer = cloudServer;
		
		setTitle(Messages.CloudFoundryServiceWizardPage_TTILE_SERVICE_CONFIG);
		setDescription(Messages.CloudFoundryServiceWizardPage_TEXT_FINISH);

		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
		
	}
	
	@Override
	public void setVisible(boolean isVis) {
		super.setVisible(isVis);
		
		List<AvailableService> services = updateConfiguration();
		if(services == null && parent.getParent() != null) {
			// User cancelled the service, so close the dialog.
			parent.getParent().close();
			return;
		}
		
		leftPanel.createInnerLayoutList(services, leftPanel.layoutList);
		
	}
	
	/** Returns the list of available services, or null if the user cancelled the monitor. */
	private List<AvailableService> updateConfiguration() {
		final List<AvailableService> result = new ArrayList<AvailableService>();

		try {

			final GetServiceOfferingsRunnable runnable = new GetServiceOfferingsRunnable();
			
			// Begin retrieving the service offerings
			ProgressMonitorDialog monitorDlg = new ProgressMonitorDialog(getShell());
			monitorDlg.run(true, true, runnable);
			
			// If the user cancelled service acquisition, then just return null.
			if(runnable.isUserCancelled()) {
				return null;
			}

			if (runnable.getServiceOfferingResult() != null) {
				int index = 0;
				for (CloudServiceOffering o : runnable.getServiceOfferingResult()) {
					
					result.add(new AvailableService(o.getName(), o.getDescription(), index, o));
					index++;
				}
			}
		} catch (InvocationTargetException e) {
			IStatus status = cloudServer.error(NLS.bind(Messages.CloudFoundryServiceWizardPage_ERROR_CONFIG_RETRIVE, e.getCause().getMessage()), e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
			setMessage(status.getMessage(), IMessageProvider.ERROR);
		} catch (InterruptedException e) {
	    	if (Logger.WARNING) {
	    		Logger.println(Logger.WARNING_LEVEL, this, "updateConfiguration", "Failed to load the list of available services."); //$NON-NLS-1$ //$NON-NLS-2$
	    	}
		}
		
		return result;
	}

	@Override
	public void createControl(Composite parent) {
		
		Composite main = new Composite(parent, SWT.NONE);	
		
		GridLayout gridLayout = new GridLayout(1, true);
		main.setLayout(gridLayout);
		topComp = main;
		
		GridData gd;
		
		Composite central = new Composite(main, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 1;
		gd.verticalIndent = 10;
		central.setLayoutData(gd);
		
		FillLayout fillLayout = new FillLayout();
		fillLayout.type = SWT.HORIZONTAL;
		fillLayout.marginHeight = 0;
		fillLayout.marginWidth = 0;
		fillLayout.spacing = 15;
		central.setLayout(fillLayout);
		
		leftPanel = new CloudFoundryServiceWizardPageLeftPanel(this);
		leftPanel.createMainWindowComposite(central);
		
		rightPanel = new CloudFoundryServiceWizardPageRightPanel(this);
		rightPanel.createMainWindowComposite(central);		

		setControl(main);
	}
	
	public List<CloudService> getServices() {
		
		List<CloudService> result = new ArrayList<CloudService>();
		
		List<List<ServiceInstance>> newServices = rightPanel.getServiceInstances();
		if(newServices != null) {
			
			for(List<ServiceInstance> instances : newServices) {
				
				for(ServiceInstance instance : instances) {

					result.add(instance.convertToLocalService());
				}				
			}
		}
		
		return result;
	}
	
	public CloudFoundryServer getCloudServer() {
		return cloudServer;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		if(leftPanel != null) {
			leftPanel.dispose();
		}
	}
	
	protected CloudFoundryServiceWizardPageRightPanel getRight() {
		return rightPanel;
	}

	class GetServiceOfferingsRunnable implements IRunnableWithProgress {
		
		private final List<CloudServiceOffering> serviceOfferingResult = new ArrayList<CloudServiceOffering>();
		private boolean userCancelled = false;
		
		public GetServiceOfferingsRunnable() {
		}
		
		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			try {
				GetServiceOfferingsThread thread = new GetServiceOfferingsThread(monitor);
				thread.start();
				thread.waitForResult(monitor);
				
				List<CloudServiceOffering> serviceOfferings = null;
				if(thread.isOperationComplete()) {
					if(thread.getException() != null) {
						throw thread.getException();
					} else {
						serviceOfferings = thread.getServiceOfferings();
					}
				} else {
					userCancelled = true;
					// User cancelled.
					return;
				}
				
				Collections.sort(serviceOfferings, new Comparator<CloudServiceOffering>() {
					public int compare(CloudServiceOffering o1, CloudServiceOffering o2) {
						return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
					}
				});
	
				// Pivotal Tracker [77602464] - Leaving plans in order
				// received by server keeps them in pricing order from
				// low to high
				// sortServicePlans(serviceOfferings);
				serviceOfferingResult.addAll(serviceOfferings);
				
				// Retrieve the names of existing services in the space
				List<String> existingCloudServiceNames = new ArrayList<String>();
				
				List<CloudService> allServices = cloudServer.getBehaviour().getServices(monitor);
				if(allServices != null) {
					for(CloudService existingService : allServices) {
						String name = existingService.getName().toLowerCase();
						
						if(!existingCloudServiceNames.contains(name)) {
							existingCloudServiceNames.add(name);
						}
					}
				}
				
				rightPanel.setExistingServicesNames(existingCloudServiceNames);
			}
			catch (OperationCanceledException e) {
				throw new InterruptedException();
			}
			catch (Throwable e) {
				throw new InvocationTargetException(e);
			}
			finally {
				monitor.done();
			}
		}
		
		public boolean isUserCancelled() {
			return userCancelled;
		}
		
		public List<CloudServiceOffering> getServiceOfferingResult() {
			return serviceOfferingResult;
		}
	}

	
	/** Retrieve the service offerings independently of the requesting job; call waitForResult w/ a progress monitor and
	 * get(Exception/ServiceOfferings) for result.  */
	private class GetServiceOfferingsThread extends Thread {

		private final IProgressMonitor monitor;
		
		private final Object lock = new Object();
		
		private boolean operationComplete = false; // locked by lock
		private Throwable exception; // locked by lock
		private List<CloudServiceOffering> serviceOfferings; // locked by lock
		
		public GetServiceOfferingsThread(IProgressMonitor monitor) {
			setName(GetServiceOfferingsThread.class.getName());
			setDaemon(true);
			this.monitor = monitor;
		}
		
		@Override
		public void run() {
			try {
				
				monitor.beginTask(Messages.CloudFoundryServiceWizardPage_GETTING_AVAILABLE_SERVICES, IProgressMonitor.UNKNOWN);
				
				List<CloudServiceOffering> localServiceOfferings = cloudServer.getBehaviour().getServiceOfferings(new NullProgressMonitor());
				
				synchronized(lock) {
					serviceOfferings = localServiceOfferings;
				}
			} catch (Throwable e) {
				synchronized(lock) {
					this.exception = e;
				}
			} finally {
				synchronized(lock) {
					operationComplete = true;
					lock.notify();
				}
			}
		}
		
		public void waitForResult(IProgressMonitor monitor) {
			try {
				// Wait for cancel or completion.
				while(!monitor.isCanceled()) {
					synchronized(lock) {
						lock.wait(100);
						
						if(operationComplete) {
							break;
						}
					}
					
				}
			} catch(InterruptedException ex) {
				return;
			}
		}
		
		public Throwable getException() {
			synchronized(lock) {
				return exception;
			}
		}
		
		public List<CloudServiceOffering> getServiceOfferings() {
			synchronized(lock) {
				return serviceOfferings;
			}
		}
		
		public boolean isOperationComplete() {
			synchronized(lock) {
				return operationComplete;
			}
		}
	}
}

