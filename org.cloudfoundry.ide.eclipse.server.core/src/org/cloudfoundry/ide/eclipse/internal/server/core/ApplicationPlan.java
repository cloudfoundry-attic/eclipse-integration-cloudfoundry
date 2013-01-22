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
package org.cloudfoundry.ide.eclipse.internal.server.core;

/**
 * An application plan is applicable to V2 servers and determines if the memory and resources
 * used by an application are part of the free allowance, or paid. Default should always be free
 */
public enum ApplicationPlan {

	free("Free"), paid("Paid");

	private final String display;

	private ApplicationPlan(String display) {
		this.display = display;
	}

	public String getDisplay() {
		return display;
	}

}
