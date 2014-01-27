/*******************************************************************************
 * Copyright (c) 2012, 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryServerBehaviour;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.statushandlers.StatusManager;


/**
 * @author Steffen Pingel
 */
public class RegisterAccountWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private RegisterAccountWizardPage page;

	private String email;

	private String password;

	public RegisterAccountWizard(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		setWindowTitle("Register Account");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = new RegisterAccountWizardPage(cloudServer);
		addPage(page);
	}

	public String getEmail() {
		return email;
	}
	
	public String getPassword() {
		return password;
	}
	
	@Override
	public boolean performFinish() {
		final String url = cloudServer.getUrl();
		email = page.getEmail();
		password = page.getPassword();
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						CloudFoundryServerBehaviour.register(url, email, password, monitor);
					}
					catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
					catch (OperationCanceledException e) {
						throw new InterruptedException();
					}
					finally {
						monitor.done();
					}
				}
			});
			return true;
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				String message = NLS.bind("Registering account failed for {0}: {1}", cloudServer.getServer()
						.getName(), e.getCause().getMessage());
				page.setErrorMessage(message);
				Status status = new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, message, e);
				StatusManager.getManager().handle(status, StatusManager.LOG);

			}
			else {
				Status status = new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
						NLS.bind("Unexpected error registering account: {0}", e.getMessage()), e);
				StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			}
		}
		catch (InterruptedException e) {
			// ignore
		}
		return false;
	}

}
