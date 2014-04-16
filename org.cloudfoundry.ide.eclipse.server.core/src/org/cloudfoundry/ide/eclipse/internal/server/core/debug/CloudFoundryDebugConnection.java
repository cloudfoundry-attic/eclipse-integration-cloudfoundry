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
package org.cloudfoundry.ide.eclipse.internal.server.core.debug;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.WaitWithProgressJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

/**
 * Performs a connection to a given server and module. Handles network timeouts,
 * including retrying if connections failed.
 */
public class CloudFoundryDebugConnection {
	protected final IModule[] modules;

	private final CloudFoundryServer cloudFoundryServer;

	public CloudFoundryDebugConnection(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
		this.cloudFoundryServer = cloudFoundryServer;
		this.modules = modules;
	}

	public List<DebugConnectionDescriptor> getDebugConnectionDescriptors(IProgressMonitor monitor) {
		final List<List<DebugConnectionDescriptor>> resolvedDescriptors = new ArrayList<List<DebugConnectionDescriptor>>();

		final CloudFoundryApplicationModule appModule = modules != null && modules.length > 0 ? cloudFoundryServer
				.getExistingCloudModule(modules[0]) : null;

		if (appModule != null) {
			try {
				new WaitWithProgressJob(5, 3000) {

					protected boolean internalRunInWait(IProgressMonitor monitor) {
						List<DebugConnectionDescriptor> descriptors = null;
						try {

							InstancesInfo instancesInfo = cloudFoundryServer.getBehaviour().getInstancesInfo(
									appModule.getDeployedApplicationName(), monitor);

							if (instancesInfo != null) {
								List<InstanceInfo> infos = instancesInfo.getInstances();
								if (infos != null) {
									// make sure list of descriptors is same
									// size as
									// the
									// number of instance infos, as the
									// descriptor
									// is
									// added to the same
									// info index
									descriptors = new ArrayList<DebugConnectionDescriptor>(infos.size());
									for (int i = 0; i < infos.size(); i++) {
										InstanceInfo info = infos.get(i);
										String debugIP = info.getDebugIp();
										int debugPort = info.getDebugPort();
										DebugConnectionDescriptor descriptor = new DebugConnectionDescriptor(debugIP,
												debugPort);
										if (descriptor.areValidIPandPort() && !descriptors.contains(descriptor)) {
											descriptors.add(i, descriptor);
										}
									}

									// keep trying again until all instances are
									// launched. Since each instance is on a
									// different
									// port
									// checking the size of successfully
									// connected
									// instances
									// with the total instances is sufficient
									if (descriptors.size() == infos.size()) {
										resolvedDescriptors.add(descriptors);
										return true;
									}
								}
							}
						}
						catch (CoreException e) {
							// ignore
						}
						// Try again
						return false;
					}

				}.run(monitor);
			}
			catch (Exception e) {
				// Ignore
			}
		}
		else {
			CloudFoundryPlugin.logError("No cloud module found for cloud application. Unable to connect debugger.");
		}

		return !resolvedDescriptors.isEmpty() ? resolvedDescriptors.get(0) : null;

	}
}
