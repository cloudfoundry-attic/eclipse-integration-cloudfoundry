/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
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
package org.eclipse.cft.server.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

/**
 * Provides a wizard context for UI Parts, that includes getting a runnable
 * context for performing operations in a wizard.
 */
public class WizardHandleContext {

	private final IWizardHandle wizardHandle;

	public WizardHandleContext(IWizardHandle wizardHandle) {
		this.wizardHandle = wizardHandle;
	}

	public IWizardHandle getWizardHandle() {
		return wizardHandle;
	}

	public IRunnableContext getRunnableContext() {
		return new IRunnableContext() {
			public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
					throws InvocationTargetException, InterruptedException {
				wizardHandle.run(fork, cancelable, runnable);
			}
		};
	}

}
