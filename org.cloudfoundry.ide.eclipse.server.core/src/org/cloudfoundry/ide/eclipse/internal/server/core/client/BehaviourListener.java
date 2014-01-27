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
 * Listens for server behaviour events, like starting or stopping an
 * application. The event types that the listener should listen too are
 * specified when the listener is registered in the listener handler.
 */
public interface BehaviourListener {

	public <T> void handle(BehaviourEvent<T> event);

}
