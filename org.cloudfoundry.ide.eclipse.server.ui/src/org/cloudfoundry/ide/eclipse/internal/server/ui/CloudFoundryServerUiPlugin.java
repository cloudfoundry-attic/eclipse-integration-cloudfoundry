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
package org.cloudfoundry.ide.eclipse.internal.server.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @author Christian Dupuis
 */
public class CloudFoundryServerUiPlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.cloudfoundry.ide.eclipse.server.ui";

	private static CloudFoundryServerUiPlugin plugin;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static CloudFoundryServerUiPlugin getDefault() {
		return plugin;
	}

	public static void logError(Throwable e) {
		if (plugin != null) {
			plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getLocalizedMessage()));
		}
	}

}
