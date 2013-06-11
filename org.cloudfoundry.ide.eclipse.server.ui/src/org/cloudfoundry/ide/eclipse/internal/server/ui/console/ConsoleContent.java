/*******************************************************************************
 * Copyright (c) 2013 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.console;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.FileContent;

public class ConsoleContent {
	
	
	private final List<FileContent> content;
	
	
	public ConsoleContent(List<FileContent> content) {
		this.content = content;
	}
	
	public List<FileContent> getFileContents() {
		return content;
	}

}
