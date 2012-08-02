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

/**
 * Defines a Standalone start command for a given runtime type. A start command
 * may be defined by multiple start command types. For example, a Java start
 * command may defined a java application start command "java ..." or a script
 * file.
 * <p/>
 * If defining multiple start command definitions, a default start command type
 * can also be specified.
 */
public abstract class StartCommand {

	private final StandaloneDescriptor descriptor;

	public StartCommand(StandaloneDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	public StandaloneDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * The start command in the form that it would be used to start the
	 * application.
	 */
	abstract public String getStartCommand();

	abstract public StartCommandType getDefaultStartCommandType();

	abstract public List<StartCommandType> getStartCommandTypes();

	public static class JavaStartCommand extends StartCommand {

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
			return "$JAVA_OPTS";
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

}
