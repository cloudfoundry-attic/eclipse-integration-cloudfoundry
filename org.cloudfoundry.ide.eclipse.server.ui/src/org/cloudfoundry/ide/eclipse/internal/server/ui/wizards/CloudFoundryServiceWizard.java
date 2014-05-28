/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software Inc. and others 
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
 *     IBM Corporation - Provide hooks to new service addition wizard 
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.LocalCloudService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Terry Denney
 */
public class CloudFoundryServiceWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private CloudFoundryServiceWizardPage1 page;

	private List<CloudService> createdServices = null;

	/**
	 * Set true if service should not be added during wizard completion.
	 */
	private final boolean deferServiceAddition;

	/**
	 * Use this constructor if service should be added automatically upon wizard
	 * completion.
	 * @param cloudServer
	 */
	public CloudFoundryServiceWizard(CloudFoundryServer cloudServer) {
		this(cloudServer, false);
	}

	/**
	 * User this constructor if caller decides whether service should be added
	 * automatically upon wizard completion
	 * @param cloudServer
	 * @param deferServiceAddition
	 */
	public CloudFoundryServiceWizard(CloudFoundryServer cloudServer, boolean deferServiceAddition) {
		this.cloudServer = cloudServer;
		setWindowTitle("Add Service");
		setNeedsProgressMonitor(true);
		this.deferServiceAddition = deferServiceAddition;
	}
	

	@Override
	public void addPages() {
		
		CloudFoundryServiceWizardPage2 page2;
		
		page = new CloudFoundryServiceWizardPage1(cloudServer);
		addPage(page);
		page2 = new CloudFoundryServiceWizardPage2(cloudServer, page);
		addPage(page2);
		page.setSecondPage(page2);
	}

	@Override	
	public boolean performFinish() {
		
		if (!deferServiceAddition && page.getSelectedList() != null) {			

			try {
				getContainer().run(true, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							List<CFServiceWizUI> services = page.getSelectedList();
							
							for(CFServiceWizUI serviceUI : services) {
								
								LocalCloudService localService = serviceUI.convertToLocalCloudService(); 
							
								cloudServer.getBehaviour().createService(new CloudService[] { localService }, monitor);
								// Get the actual Service
								List<CloudService> allServices = cloudServer.getBehaviour().getServices(monitor);
								if (allServices != null) {
									for (CloudService existingService : allServices) {
										if (existingService.getName().equals(localService.getName())) {
											if(createdServices == null) {
												createdServices = new ArrayList<CloudService>();
											}
											createdServices.add(existingService);
											break;
										}
									}
								}
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
				}); // end run
				return true;
			}
			catch (InvocationTargetException e) {
				if (e.getCause() != null) {
					Status status = new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
							"Adding of service failed for {0}: {1}", cloudServer.getServer().getName(), e.getCause()
									.getMessage() != null ? e.getCause().getMessage() : e.getCause().toString()), e);
					StatusManager.getManager().handle(status,
							StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
				}
			}
			catch (InterruptedException e) {
				// ignore
			}				
				
			
			return false;
		
		} // end if
		
		return true;

	}

	/**
	 * Returns the services added by this wizard, or possibly null if wizard
	 * hasn't completed yet or was cancelled.
	 * @return added services or null if nothing added at the time of the call
	 */
	public List<CloudService> getServices() {
		
		List<CloudService> result = null;
		
		if (createdServices != null) {
			result = createdServices;
			
		} else 	if(page != null) {
			// Convert the UI selections to CloudService entries
			List<CFServiceWizUI> selectedServices = page.getSelectedList();
			if(selectedServices != null && selectedServices.size() > 0) {
				result = new ArrayList<CloudService>();
				for(CFServiceWizUI product : selectedServices) {
					result.add(product.convertToLocalCloudService());
				}
			}
						
		}
		return result;
	}

}
