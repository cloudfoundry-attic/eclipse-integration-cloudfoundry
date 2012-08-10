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
package org.cloudfoundry.ide.eclipse.internal.server.core.standalone;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class JavaStartCommand extends StartCommand {

	public static final String DEFAULT_LIB = "lib";

	public static final IPath DEFAULT_LIB_PATH = Path.EMPTY.append(DEFAULT_LIB);

	protected JavaStartCommand(StandaloneDescriptor descriptor) {
		super(descriptor);
	}

	@Override
	public String getStartCommand() {
		StringWriter writer = new StringWriter();
		writer.append("java");
		if (getOptions() != null) {
			writer.append(" ");
			writer.append(getOptions());
		}
		return writer.toString();
	}

	public String getOptions() {
		StringWriter options = new StringWriter();
		options.append("$JAVA_OPTS");
		options.append(" ");
		options.append("-cp");
		options.append(" ");
		options.append(getClassPathOptionArg());
		return options.toString();
	}

	protected String getClassPathOptionArg() {
		StringWriter options = new StringWriter();
		options.append(DEFAULT_LIB);
		options.append("/");
		options.append("*");
		options.append(":");
		options.append(".");
		return options.toString();
	}

	@Override
	public StartCommandType getDefaultStartCommandType() {
		return StartCommandType.Java;
	}

	/**
	 * May be empty, but never null
	 * @return
	 */
	public List<StartCommandType> getStartCommandTypes() {
		return Arrays.asList(new StartCommandType[] { StartCommandType.Java, StartCommandType.Other });
	}

}