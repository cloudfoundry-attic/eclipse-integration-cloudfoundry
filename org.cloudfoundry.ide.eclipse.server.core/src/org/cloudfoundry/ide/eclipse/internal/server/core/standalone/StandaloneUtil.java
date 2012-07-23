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

import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.client.lib.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.wst.server.core.IModule;

public class StandaloneUtil {
	private StandaloneUtil() {
		// Util class
	}

	public static boolean isStandaloneApp(ApplicationModule appModule) {
		IModule module = appModule.getLocalModule();
		boolean isStandalone = module != null
				&& CloudFoundryServer.ID_JAVA_STANDALONE_APP.equals(module.getModuleType().getId());
		if (!isStandalone) {
			Staging staging = getStaging(appModule);
			isStandalone = staging != null && CloudApplication.STANDALONE.equals(staging.getFramework());
		}
		return isStandalone;
	}

	public static Staging getStaging(ApplicationModule appModule) {
		if (appModule == null) {
			return null;
		}
		Staging staging = appModule.getStaging();
		if (staging == null) {
			CloudApplication cloudApp = appModule.getApplication();
			if (cloudApp != null) {
				staging = new StagingHandler(cloudApp.getStaging()).getStaging();
			}
		}
		return staging;
	}

	public static boolean isValidStaging(Staging staging) {
		return staging != null && staging.getCommand() != null
				&& CloudApplication.STANDALONE.equals(staging.getFramework()) && staging.getRuntime() != null;
	}
}
