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
package org.cloudfoundry.ide.eclipse.server.core.internal.client;

/**
 * Behaviour events are events that occur while a server request is being
 * processed, for example starting or stopping an application.
 * 
 * 
 * 
 */
public enum BehaviourEventType {
	
	APP_START,

	APP_STARTED,

	APP_PRE_START,

	APP_STARTING,

	APP_DELETE,

	APP_STOPPED,

	DISCONNECT,

	PROMPT_CREDENTIALS,

	REFRESH_TUNNEL_CONNECTIONS,

	SERVICES_DELETED

}
