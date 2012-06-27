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

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryProjectUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;

public abstract class StartCommand {

	private final StandaloneRuntimeType type;

	private final IProject project;

	protected StartCommand(StandaloneRuntimeType type, IProject project) {
		this.type = type;
		this.project = project;
	}

	public IProject getProject() {
		return project;
	}

	abstract public String getStartCommand();

	abstract public String getArguments();

	public StandaloneRuntimeType getRuntimeType() {
		return type;
	}

	public static StartCommand getCommand(StandaloneRuntimeType type, IProject project) {
		if (type != null) {
			switch (type) {
			case Java:
				return new JavaStartCommand(type, project);
			}
		}
		return null;
	}

	public static StartCommand getCommand(String type, IProject project) {
		if (type != null) {
			StandaloneRuntimeType runtimeType = null;
			for (StandaloneRuntimeType rType : StandaloneRuntimeType.values()) {
				if (type.equals(rType.getId())) {
					runtimeType = rType;
					break;
				}
			}
			return getCommand(runtimeType, project);
		}
		return null;
	}

	static class JavaStartCommand extends StartCommand {

		protected JavaStartCommand(StandaloneRuntimeType type, IProject project) {
			super(type, project);
		}

		protected String getMainMethodType() {
			if (getProject().isAccessible()) {
				IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(getProject());
				if (javaProject != null) {

				}
			}
			return null;
		}

		@Override
		public String getStartCommand() {
			return "java $JAVA_OPTS";
		}

		@Override
		public String getArguments() {
			return null;
		}

	}

}
