/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

/**
 * Console content type indicating that content is coming from a remote file and
 * should be streamed to the console.
 */
public class FileStreamContentType implements IContentType {

	private final String id = "file_stream";

	public String getId() {
		return id;
	}

}
