/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

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
