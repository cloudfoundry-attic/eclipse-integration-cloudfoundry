/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.client;

import org.eclipse.wst.server.core.IModule;

/**
 * An operation performed in a {@link CloudFoundryServerBehaviour} target Cloud
 * space on a target IModule
 */
public abstract class BehaviourOperation implements ICloudFoundryOperation {

	private final CloudFoundryServerBehaviour behaviour;

	private final IModule module;

	public BehaviourOperation(CloudFoundryServerBehaviour behaviour, IModule module) {
		this.behaviour = behaviour;
		this.module = module;
	}

	public CloudFoundryServerBehaviour getBehaviour() {
		return behaviour;
	}

	public IModule getModule() {
		return module;
	}
}
