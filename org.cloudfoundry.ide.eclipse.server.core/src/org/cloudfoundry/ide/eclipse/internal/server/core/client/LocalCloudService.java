/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.springframework.util.Assert;

/**
 * Represents a cloud service that is either not yet created or exists and is
 * bound to an application, but does not contain full information for the
 * service, as bound services typically only contain the service name.
 * 
 */
public class LocalCloudService extends CloudService {

	public LocalCloudService(String name) {
		Assert.notNull(name);
		setMeta(CloudEntity.Meta.defaultMeta());
		setName(name);
	}

}
