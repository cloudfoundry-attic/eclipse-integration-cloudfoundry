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
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.application.AbstractApplicationProvider;
import org.eclipse.core.runtime.IConfigurationElement;


/**
 * 
 *  Wrapper around the wizard page contributions for a particular application type from the extension point:
 *  
 *  <p/>
 *  org.cloudfoundry.ide.eclipse.server.ui.applicationWizard
 *  
 *  <p/>
 *  
 */
public class ApplicationWizardProvider extends AbstractApplicationProvider<IApplicationWizardDelegate> {

	public ApplicationWizardProvider(IConfigurationElement configuration) {
		super(configuration);
	}

}
