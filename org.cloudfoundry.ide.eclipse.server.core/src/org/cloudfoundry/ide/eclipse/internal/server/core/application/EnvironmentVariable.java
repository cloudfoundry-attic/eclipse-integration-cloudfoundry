/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.application;

/**
 * Representation of a process environment variable used for JSON serialisation of a service command
 *
 */
public class EnvironmentVariable {

	private String variable;

	private String value;

	public EnvironmentVariable() {
		//
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * In some cases the value itself is a variable in the form of ${valuevar}.
	 * This helper method returns the value variable (e.g., returns "valuevar"
	 * for ${valuevar}). Returns null if the value is not a variable that has
	 * ${} pattern
	 * @param envVar
	 * @return
	 */
	public static String getValueVariable(EnvironmentVariable envVar) {
		String value = envVar.getValue();
		if (value == null) {
			return null;
		}
		else {

			value = value.trim();

			if (value.startsWith("${") && value.endsWith("}")) {
				StringBuffer val = new StringBuffer();
				for (int i = 0; i < value.length(); i++) {
					char ch = value.charAt(i);
					if (ch != '$' && ch != '{' && ch != '}') {
						val.append(ch);
					}
				}
				return val.toString();
			}

		}
		return null;
	}
}
