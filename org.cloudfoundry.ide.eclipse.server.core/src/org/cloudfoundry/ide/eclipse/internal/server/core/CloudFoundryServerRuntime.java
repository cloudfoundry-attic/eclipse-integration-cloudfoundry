/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jst.server.core.IJavaRuntime;
import org.eclipse.jst.server.core.IJavaRuntimeWorkingCopy;
import org.eclipse.wst.server.core.model.RuntimeDelegate;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 */
public class CloudFoundryServerRuntime extends RuntimeDelegate implements IJavaRuntime, IJavaRuntimeWorkingCopy {

	protected static final String PROP_VM_INSTALL_ID = "vm-install-id";

	protected static final String PROP_VM_INSTALL_TYPE_ID = "vm-install-type-id";

	public IVMInstall getVMInstall() {
		if (getVMInstallTypeId() == null)
			return JavaRuntime.getDefaultVMInstall();
		try {
			IVMInstallType vmInstallType = JavaRuntime.getVMInstallType(getVMInstallTypeId());
			IVMInstall[] vmInstalls = vmInstallType.getVMInstalls();
			int size = vmInstalls.length;
			String id = getVMInstallId();
			for (int i = 0; i < size; i++) {
				if (id.equals(vmInstalls[i].getId()))
					return vmInstalls[i];
			}
		}
		catch (Exception e) {
			// ignore
		}
		return null;
	}

	public boolean isUsingDefaultJRE() {
		return getVMInstallTypeId() == null;
	}

	public void setVMInstall(IVMInstall vmInstall) {
		if (vmInstall == null) {
			setVMInstall(null, null);
		}
		else
			setVMInstall(vmInstall.getVMInstallType().getId(), vmInstall.getId());
	}

	@Override
	public IStatus validate() {
		return Status.OK_STATUS;
	}

	protected String getVMInstallId() {
		return getAttribute(PROP_VM_INSTALL_ID, (String) null);
	}

	protected String getVMInstallTypeId() {
		return getAttribute(PROP_VM_INSTALL_TYPE_ID, (String) null);
	}

	protected void setVMInstall(String typeId, String id) {
		if (typeId == null)
			setAttribute(PROP_VM_INSTALL_TYPE_ID, (String) null);
		else
			setAttribute(PROP_VM_INSTALL_TYPE_ID, typeId);

		if (id == null)
			setAttribute(PROP_VM_INSTALL_ID, (String) null);
		else
			setAttribute(PROP_VM_INSTALL_ID, id);
	}

}
