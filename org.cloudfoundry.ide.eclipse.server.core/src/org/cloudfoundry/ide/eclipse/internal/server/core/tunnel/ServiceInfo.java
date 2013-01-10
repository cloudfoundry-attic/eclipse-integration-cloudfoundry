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
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

/**
 * 
 * Using getters and setters with no-argument constructors for JSON serialisation
 * 
 */
public enum ServiceInfo {

	BLOB_SERVICE("blob"),

	MONGODB_SERVICE("mongodb"),

	MYSQL_SERVICE("mysql"),

	POSTGRESQL_SERVICE("postgresql"),

	RABBITMQ_SERVICE("rabbitmq"),

	REDIS_SERVICE("redis");

	private final String vendor;

	private ServiceInfo(String vendor) {
		this.vendor = vendor;
	}

	public String getVendor() {
		return vendor;
	}

}