/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others
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
 *     IBM Corporation - initial API and implementation
 ********************************************************************************/

package org.cloudfoundry.ide.eclipse.server.ui;

import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Concrete implementations of this class should be thread safe, allowing
 * multiple concurrent calls to its member methods. Returned images will be
 * cached for the lifetime of the wizard dialog, and re-acquired for subsequent
 * wizard invocations.
 */
public interface ICloudFoundryServiceWizardIconProvider {

	/**
	 * This interface method can be implemented by consumers to provide icons for the services of the add services wizard.
	 * 
	 * @param offering The specific service offering for which an icon is being requested
	 * @param server The specific server for which the request is being made
	 * @return An image descriptor which can be used to create an image for used by the service wizard.
	 */
	public ImageDescriptor getServiceIcon(CloudServiceOffering offering, CloudFoundryServer server);

}
