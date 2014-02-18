/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

/**
 * Constants used in validating server credentials.
 */
public class ServerCredentialsValidationStatics {
	public static final int EVENT_CREDENTIALS_FILLED = 1000;

	public static final int EVENT_SPACE_VALID = 1002;

	public static final int EVENT_SPACE_SELECTED = 1003;
	
	public static final int EVENT_SELF_SIGNED_ERROR = 1004;

	public static final int EVENT_NONE = -1;

	public static final String DEFAULT_DESCRIPTION = "Enter credentials to log on to a selected Cloud Foundry server.";

	public static final String VALID_ACCOUNT_MESSAGE = "Account information is valid.";

}
