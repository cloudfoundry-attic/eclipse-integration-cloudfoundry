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
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Defines an Eclipse CF console content. Generally, there is one console
 * content instance per file resource being streamed to the CF console, and the
 * responsibility of the console content is to create a stream that fetches
 * content from a CF server for a particular file and sends it as output to the
 * Eclipse CF console output stream.
 */
public interface IConsoleContent {

	public ICloudFoundryConsoleOutputStream getOutputStream(IOConsoleOutputStream consoleOutputStream);
}
