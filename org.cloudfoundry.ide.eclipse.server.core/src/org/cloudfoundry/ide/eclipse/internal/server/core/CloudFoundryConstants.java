/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *     Keith Chong, IBM - Allow module to bypass facet check
 *     Keith Chong, IBM - Branding doesn't have to be on the Sign-up button
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

public class CloudFoundryConstants {

	public static final String GRAILS_NATURE = "com.springsource.sts.grails.core.nature";

	public static final String LIFT = "lift/1.0";

	public static final String JAVA_WEB = "java_web";

	public static final String SPRING = "spring";

	public static final String GRAILS = "grails";

	public static final String ID_GRAILS_APP = "grails.app";

	public static final String ID_WEB_MODULE = "jst.web";
	
	// Does this have to be vendor specific?  Removing Pivotal CF
	public static final String PUBLIC_CF_SERVER_SIGNUP_LABEL = "Sign Up";

    public static final String PROPERTY_PROJECT_INDEPENDENT = "org.cloudfoundry.ide.eclipse.server.core.property.ProjectIndependent";

}
