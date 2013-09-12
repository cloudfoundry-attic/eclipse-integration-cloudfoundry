/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.client;

/**
 * Behaviour events are events that occur while or after a request to the Cloud
 * Foundry client is being made.
 * <p/>
 * NOTE: As of CF 1.6.0, Cloud Foundry behaviour requests are mixed sequential
 * and event driven.
 * <p/>
 * Results from request are sequential, while certain types of events occuring
 * during a request are event-driven. This is due to legacy sequential design
 * from previous CF plugin versions not being able to cleanly accommodate
 * certain types of behaviour handling, like refreshing app stats after an
 * operation, or performing some type of function like console output during
 * different stages of starting an application.
 * 
 * 
 */
public enum BehaviourEventType {

	APP_STARTED, 
	
	APP_PRE_START, 
	
	APP_STARTING, 
	
	APP_DELETE, 
	
	APP_STOPPED, 
	
	TUNNEL_CONNECTIONS_REFRESHED

}
