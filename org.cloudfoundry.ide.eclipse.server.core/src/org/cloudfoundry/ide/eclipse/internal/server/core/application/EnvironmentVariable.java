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
