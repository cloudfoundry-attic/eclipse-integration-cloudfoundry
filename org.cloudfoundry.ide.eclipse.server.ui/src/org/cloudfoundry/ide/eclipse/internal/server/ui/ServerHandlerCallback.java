/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Configures a server after a server operation (for example, creating a
 * server). A typical use of this callback is to set credentials in the server
 * @author Steffen Pingel
 */
public abstract class ServerHandlerCallback {

	public abstract void configureServer(IServerWorkingCopy wc) throws CoreException;

}