/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core.tunnel;

public class CommandOption extends CommandMetaElement {

	private final Option option;

	private final Value value;

	public CommandOption(Option option, Value value) {
		super("CommandOption");
		this.option = option;
		this.value = value;
	}

	public Option getOption() {
		return option;
	}

	public Value getValue() {
		return value;
	}

	public class Option extends CommandMetaElement {
		private final String optionName;

		public Option(String optionName) {
			super("Option");
			this.optionName = optionName;
		}

		public String getOptionName() {
			return optionName;
		}
	}

	public class Value extends CommandMetaElement {

		private final String value;

		public Value(String value) {
			super("Value");
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

}
