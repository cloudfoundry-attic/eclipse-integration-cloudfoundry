/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal.debug;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;

public interface IDebugProvider {

	/**
	 * Resolve the connection descriptor. Throw {@link CoreException} if failed
	 * to resolve.
	 * @param appModule
	 * @param cloudServer
	 * @param monitor
	 * @return
	 * @throws CoreException if error occurs.
	 */
	public DebugConnectionDescriptor getDebugConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, IProgressMonitor monitor) throws CoreException;

	/**
	 * Determine if the application is in a state where it can be launched. If
	 * true, the debug framework will proceed to configure and launch the
	 * application in debug mode. If false, the framework will stop the launch
	 * without errors.
	 * @param appModule
	 * @param cloudServer
	 * @return True if app should be debugged. False if debug should stop.
	 */
	public boolean canLaunch(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException;

	/**
	 * Return true if debug is supported for the given application running on
	 * the target cloud server. This is meant to be a fairly quick check
	 * therefore avoid long-running operations.
	 * @param appModule
	 * @param cloudServer
	 * @return true if debug is supported for the given app. False otherwise
	 */
	public boolean isDebugSupported(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer);

	/**
	 * Optional configuration ID to be used for launching the application in
	 * debug mode. Return null if the default launch configuration should be
	 * used.
	 * @see ILaunchConfigurationType
	 * @see ILaunchConfiguration
	 * @return Optional launch configuration ID or null otherwise if default is
	 * to be used.
	 */
	public String getLaunchConfigurationID();

	/**
	 * Perform any necessary configuration on the application before launching
	 * it in debug mode, For example, if environment variables need to be set in
	 * the application. Return true if configuration was successful and app is
	 * ready to be launched in debug mode . Return false if debug should not
	 * proceed. Throw {@link CoreException} if error occurred while configuring
	 * the application.
	 * @param appModule
	 * @param cloudServer
	 * @param monitor
	 * @throws CoreException
	 * @return true if application is ready to be launched. False if debug
	 * launch should stop.
	 */
	public boolean configureApp(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException;

}