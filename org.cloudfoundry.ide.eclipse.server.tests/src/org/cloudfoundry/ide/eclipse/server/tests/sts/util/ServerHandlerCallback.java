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
package org.cloudfoundry.ide.eclipse.server.tests.sts.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * @author Steffen Pingel
 */
public abstract class ServerHandlerCallback {

	public abstract void configureServer(IServerWorkingCopy wc) throws CoreException;

}