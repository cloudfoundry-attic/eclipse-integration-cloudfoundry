/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License”); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.FrameworkProvider;
import org.cloudfoundry.ide.eclipse.internal.server.core.application.IApplicationDelegate;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * 
 * Wrapper around the provider that contributes wizard page for a particular
 * application type from the extension point:
 * 
 * <p/>
 * org.cloudfoundry.ide.eclipse.server.ui.applicationWizard
 * <p/>
 * 
 * The wrapper converts an {@link IApplicationWizardDelegate} as defined in the
 * extension point above into an {@link ApplicationWizardDelegate}, which
 * contains additional API used by the CF plug-in framework, as well as a
 * mapping to the corresponding {@link IApplicationDelegate}.
 * 
 */
public class ApplicationWizardProvider {

	private final InternalApplicationWizardProvider internalProvider;

	private ApplicationWizardDelegate wizardDelegate;

	public ApplicationWizardProvider(IConfigurationElement configuration, String extensionPointID) {
		internalProvider = new InternalApplicationWizardProvider(configuration, extensionPointID);
	}

	public ApplicationWizardDelegate getDelegate(IApplicationDelegate coreDelegate) {
		if (wizardDelegate == null) {
			IApplicationWizardDelegate actualDelegate = internalProvider.getDelegate();
			if (actualDelegate instanceof ApplicationWizardDelegate) {
				wizardDelegate = (ApplicationWizardDelegate) actualDelegate;
				wizardDelegate.setApplicationDelegate(coreDelegate);
			}
			else {
				wizardDelegate = new ApplicationWizardDelegateImp(actualDelegate, coreDelegate);
			}
		}
		return wizardDelegate;
	}

	public String getProviderID() {
		return internalProvider.getProviderID();
	}

	/**
	 * Actual wizard provider that gets loaded from the extension point.
	 * 
	 */
	static class InternalApplicationWizardProvider extends FrameworkProvider<IApplicationWizardDelegate> {
		public InternalApplicationWizardProvider(IConfigurationElement configuration, String extensionPointID) {
			super(configuration, extensionPointID);
		}
	}

	/**
	 * Maps an {@link IApplicationWizardDelegate} to an
	 * {@link IApplicationDelegate}. The mapping is an internal framework
	 * relation, and therefore not part of the
	 * {@link IApplicationWizardDelegate} API.
	 * 
	 */
	static class ApplicationWizardDelegateImp extends ApplicationWizardDelegate {

		private final IApplicationWizardDelegate actualDelegate;

		public ApplicationWizardDelegateImp(IApplicationWizardDelegate actualDelegate, IApplicationDelegate coreDelegate) {
			this.actualDelegate = actualDelegate;
			setApplicationDelegate(coreDelegate);
		}

		public List<IWizardPage> getWizardPages(ApplicationWizardDescriptor descriptor, CloudFoundryServer cloudServer,
				CloudFoundryApplicationModule applicationModule) {
			return actualDelegate.getWizardPages(descriptor, cloudServer, applicationModule);
		}

	}
}
