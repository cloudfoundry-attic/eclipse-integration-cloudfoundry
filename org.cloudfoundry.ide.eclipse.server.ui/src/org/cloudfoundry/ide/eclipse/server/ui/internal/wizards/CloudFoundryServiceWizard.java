/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. and IBM Corporation
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

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Jonathan West
 */
public class CloudFoundryServiceWizard extends Wizard {
	
	private WizardDialog parent;
	
	private final CloudFoundryServer cloudServer;

	private CloudFoundryServiceWizardPage page;

	private List<CloudService> resultServices = null;
	
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
	 * Use this constructor if caller decides whether service should be added
	 * automatically upon wizard completion
	 * @param cloudServer
	 * @param deferServiceAddition
	 */
	public CloudFoundryServiceWizard(CloudFoundryServer cloudServer, boolean deferServiceAddition) {
		this.cloudServer = cloudServer;
		setWindowTitle(Messages.COMMONTXT_ADD_SERVICE);
		setNeedsProgressMonitor(true);
		this.deferServiceAddition = deferServiceAddition;
		
	}
	

	@Override
	public void addPages() {
		page = new CloudFoundryServiceWizardPage(cloudServer, this);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		
		List<CloudService> localServices = page.getServices();
		
		if (!deferServiceAddition && localServices != null) {
			ServiceCreationJob job = new ServiceCreationJob(localServices, cloudServer);
			job.setUser(true);
			job.setPriority(Job.SHORT);
			job.schedule();
			
		}
				
		return true;
	}

	/**
	 * Returns the service added by this wizard, or possibly null if wizard
	 * hasn't completed yet or was cancelled.
	 * 
	 * These services have not necessarily been created it, depending on the
	 * result of deferAddition.
	 * 
	 * @return added service or null if nothing added at the time of the call
	 */
	public List<CloudService> getServices() {
		if (resultServices != null) {
			return resultServices;
		}

		return page != null ? page.getServices() : null;
	}

	
	/** Create the specified services and confirms their creation */
	private static class ServiceCreationJob extends Job {
		
		final CloudFoundryServer cloudServer;
		final List<CloudService> servicesToCreate;
		
		public ServiceCreationJob(List<CloudService> servicesToCreate, CloudFoundryServer cloudServer) {
			super(Messages.CloudFoundryServiceWizard_JOB_TASK_CREATING_SERVICES);
			this.servicesToCreate = servicesToCreate;
			this.cloudServer = cloudServer;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitorParam) {
			
			SubMonitor monitor = SubMonitor.convert(monitorParam);
			
			monitor.beginTask(Messages.CloudFoundryServiceWizard_JOB_TASK_CREATING_SERVICES,
					servicesToCreate.size() * 100 + 100);
			
			Status status = null;
			
			try {

				for(CloudService cs : servicesToCreate) {
					monitor.subTask(Messages.CloudFoundryServiceWizard_JOB_SUBTASK_ADDING_SERVICE+" "+cs.getName());
					cloudServer.getBehaviour().operations().createServices((new CloudService[] { cs }))
					.run(monitor.newChild(100));
				}
				
				monitor.subTask(Messages.CloudFoundryServiceWizard_JOB_SUBTASK_VERIFYING_SERVICES);
				
				// Find the newly created services in the service list
				List<CloudService> allServices = cloudServer.getBehaviour().getServices(monitor.newChild(100));

				if (allServices != null) {

					// Locate the new services from the server's service instances
					for(CloudService localService : servicesToCreate) {
					
						boolean matchFound = false;
						for (CloudService existingService : allServices) {					
							
							if (existingService.getName().equals(localService.getName())) {
								allServices.add(existingService);
								matchFound = true;
								break;
							}
							
						}
						
						if(!matchFound) {
							
							status = new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
									Messages.CloudFoundryServiceWizard_ERROR_ADD_SERVICE, cloudServer.getServer().getName(), localService.getName())
								);
							break;
						}
						
					}
					monitor.worked(100);
					
				}
				
			} 
			catch (CoreException e) {
				status = new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
						Messages.CloudFoundryServiceWizard_ERROR_ADD_SERVICE,
						cloudServer.getServer().getName(),
						e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : e
								.toString()), e);
			}			

			monitor.done();

			if(status != null && !status.isOK()) {
				final IStatus statusToDisplay = status;
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						StatusManager.getManager().handle(statusToDisplay,
								StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
					}
				});
				return Status.CANCEL_STATUS;
			}

			
			return Status.OK_STATUS;
		}
				
	}
	
	public void setParent(WizardDialog parent) {
		this.parent = parent;
	}

	protected WizardDialog getParent() {
		return parent;
	}
	

}
