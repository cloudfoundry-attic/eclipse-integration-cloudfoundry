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
package org.cloudfoundry.ide.eclipse.internal.server.core.debug;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
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

		new WaitOperation() {

			protected int getWaitTime() {
				return 3000;
			}

			protected boolean runInWaitCycle(IProgressMonitor monitor) {
				List<DebugConnectionDescriptor> descriptors = null;
				try {
					ApplicationModule appModule = cloudFoundryServer.getApplication(modules);
					InstancesInfo instancesInfo = cloudFoundryServer.getBehaviour().getInstancesInfo(
							appModule.getApplicationId(), monitor);

					if (instancesInfo != null) {
						List<InstanceInfo> infos = instancesInfo.getInstances();
						if (infos != null) {
							// make sure list of descriptors is same size as
							// the
							// number of instance infos, as the descriptor is
							// added to the same
							// info index
							descriptors = new ArrayList<DebugConnectionDescriptor>(infos.size());
							for (int i = 0; i < infos.size(); i++) {
								InstanceInfo info = infos.get(i);
								String debugIP = info.getDebugIp();
								int debugPort = info.getDebugPort();
								DebugConnectionDescriptor descriptor = new DebugConnectionDescriptor(debugIP, debugPort);
								if (descriptor.areValidIPandPort() && !descriptors.contains(descriptor)) {
									descriptors.add(i, descriptor);
								}
							}

							// keep trying again until all instances are
							// launched. Since each instance is on a
							// different
							// port
							// checking the size of successfully connected
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

		}.doWait(monitor);

		return !resolvedDescriptors.isEmpty() ? resolvedDescriptors.get(0) : null;

	}
}
