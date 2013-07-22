/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.server.standalone.internal.application;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.eclipse.wst.server.core.IModule;

/**
 * Determines if a WST IModule corresponds to a Java standalone application.
 */
public class StandaloneModuleHelper {

	private final CloudFoundryApplicationModule appModule;

	private final IModule module;

	public StandaloneModuleHelper(CloudFoundryApplicationModule appModule) {
		this.appModule = appModule;
		this.module = appModule.getLocalModule();
	}

	public StandaloneModuleHelper(IModule module) {
		this.appModule = null;
		this.module = module;
	}

	public boolean isSupportedStandalone() {
		if (appModule == null && module == null) {
			return false;
		}

		boolean isStandalone = module != null
				&& StandaloneFacetHandler.ID_MODULE_STANDALONE.equals(module
						.getModuleType().getId());
		// If standalone app cannot be determined by the local module, check the
		// staging
		if (!isStandalone) {
			Staging staging = getStaging();
			isStandalone = staging != null
					&& CloudApplication.STANDALONE.equals(staging
							.getFramework());
		}
		return isStandalone;
	}

	public Staging getStaging() {
		if (appModule == null) {
			return null;
		}
		Staging staging = appModule.getStaging();
		if (staging == null) {
			CloudApplication cloudApp = appModule.getApplication();
			if (cloudApp != null) {
				staging = cloudApp.getStaging();
			}
		}
		return staging;
	}

}
