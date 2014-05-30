
/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.cloudfoundry.ide.eclipse.server.core.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates whether a url name value.
 * <p/>
 * Valid url names should not include the protocol (e.g. http://www.google.com)
 * or queries in the name valid names are:
 * <p/>
 * www.google.com
 * <p/>
 * www$.google.com
 * <p/>
 * www.google.com4
 * <p/>
 * names with trailing or ending spaces, or spaces in between the name segments
 * are invalid.
 * 
 */
public class URLNameValidation {

	private final String value;

	public URLNameValidation(String value) {
		this.value = value;
	}

	private final Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-.]+");



	public boolean hasInvalidCharacters() {
		if (!ValueValidationUtil.isEmpty(value)) {
			Matcher matcher = VALID_CHARS.matcher(value);
			return !matcher.matches();
		}
		return true;
	}

}
