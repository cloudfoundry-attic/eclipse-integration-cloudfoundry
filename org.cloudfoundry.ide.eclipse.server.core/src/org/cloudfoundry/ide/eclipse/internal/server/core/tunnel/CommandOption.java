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
public class CommandOption {

	private String option;

	private String value;

	public CommandOption() {

	}

	public String getOption() {
		return option;
	}

	public void setOption(String option) {
		this.option = option;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public static boolean isOptionValueSet(CommandOption option) {
		return option.getValue() != null && option.getValue().length() > 0 && !option.getValue().trim().startsWith("$");
	}
	
	/**
	 * Returns a variable name, if the value is detected as a variable starting with a '$'. Otherwise
	 * returns null, meaning that the value is not a variable
	 * @param option
	 * @return
	 */
	public static String getVariableName(CommandOption option) {
		String variableName = null;
		String value = option.getValue();
		if (value!= null ) {
			value = value.trim();
			if (value.startsWith("$") && value.length() > 1) {
				return value.substring(value.indexOf('$') + 1);
			}
		}
		
		return variableName;
	}

}
