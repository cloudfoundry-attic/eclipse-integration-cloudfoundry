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

import junit.framework.TestCase;

import org.eclipse.core.runtime.IStatus;

/**
 * Tests various types of URL domain names for validity. The validation is used
 * by the UI to determine whether deployment can be completed given a URL. Also
 * tests whether URLs are needed for standalone applications, and whether
 * standalone have valid start commands.
 * 
 */
public class DeploymentURLTest extends TestCase {

	public void testDeploymentInvalidURLValueNull() throws Exception {
		String value = null;
		assertEquals(true, isEmpty(value));
	}

	public void testDeploymentInvalidURLValueEmpty() throws Exception {
		String value = "";
		assertEquals(true, isEmpty(value));
	}

	public void testDeploymentInvalidURLValueExtraSpaces() throws Exception {
		String value = "  	";
		assertEquals(true, isEmpty(value));
	}

	public void testDeploymentInvalidURLExtraSpacesNonSpaceChar() throws Exception {
		String value = "  	5";
		assertEquals(false, isEmpty(value));
	}

	public void testDeploymentValidURLName() throws Exception {
		String value = "www.google.com";
		assertEquals(false, isEmpty(value));
	}

	public void testDeploymentValidURLNameValidator() throws Exception {
		String value = "www$.google.com";
		assertEquals(false, isInvalidWithValidator(value));
	}

	public void testDeploymentValidURLNameValidator2() throws Exception {
		String value = "www.google.com";
		assertEquals(false, isInvalidWithValidator(value));
	}

	public void testDeploymentInvalidURLNameValidator() throws Exception {
		String value = "www .google.com";
		assertEquals(true, isInvalidWithValidator(value));
	}

	public void testDeploymentInvalidURLNameValidator2() throws Exception {
		String value = "http://www.google.com";
		assertEquals(true, isInvalidWithValidator(value));
	}

	public void testDeploymentInvalidURLNameValidator3() throws Exception {
		// empty space at start of url name
		String value = " www.google.com";
		assertEquals(true, isInvalidWithValidator(value));
	}

	public void testDeploymentInvalidURLNameValidator4() throws Exception {
		// empty space at end of url name
		String value = "www.google.com ";
		assertEquals(true, isInvalidWithValidator(value));
	}

	public void testDeploymentInvalidURLValidatorNull() throws Exception {
		String value = null;
		assertEquals(true, isInvalidWithValidator(value));
	}

	public void testDeploymentInvalidURLValidatorEmpty() throws Exception {
		String value = "";
		assertEquals(true, isInvalidWithValidator(value));
	}

	public void testDeploymentInvalidURLValidatorExtraSpaces() throws Exception {
		String value = "  	";
		assertEquals(true, isInvalidWithValidator(value));
	}

	// public void testDeploymentInfoStandaloneNOurlEMPTYHAScommand() throws
	// Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(true,
	// "java HelloWord.java", " ");
	// assertValidator(Status.OK_STATUS.getMessage(), false, validator);
	// }
	//
	// public void testDeploymentInfoStandaloneHASurlHAScommand() throws
	// Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(true,
	// "java HelloWord.java",
	// "myapp.cloudfoundry.com");
	// assertValidator(Status.OK_STATUS.getMessage(), false, validator);
	// }
	//
	// public void testDeploymentInfoStandaloneHASurlNocommandEmpty() throws
	// Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(true, " ",
	// "myapp.cloudfoundry.com");
	// assertValidator(INVALID_START_COMMAND, true, validator);
	// }
	//
	// public void testDeploymentInfoStandaloneHASurlNOcommandNull() throws
	// Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(true,
	// null, "myapp.cloudfoundry.com");
	// assertValidator(INVALID_START_COMMAND, true, validator);
	// }
	//
	// public void testDeploymentInfoStandaloneNOurlNOcommandNull() throws
	// Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(true,
	// null, null);
	// assertValidator(INVALID_START_COMMAND, true, validator);
	// }
	//
	// public void testDeploymentInfoStandaloneNOurlNOcommandEmptySpaces()
	// throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(true,
	// "   ", null);
	// assertValidator(INVALID_START_COMMAND, true, validator);
	// }
	//
	// public void testDeploymentInfoStandaloneNOurlEMPTYNOcommandEmptySpaces()
	// throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(true,
	// "   ", " ");
	// assertValidator(INVALID_START_COMMAND, true, validator);
	// }
	//
	// public void testDeploymentInfoStandaloneNOurlEMPTYNOcommand() throws
	// Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(true,
	// null, " ");
	// assertValidator(INVALID_START_COMMAND, true, validator);
	// }
	//
	// public void testDeploymentInfoStandaloneHASInvalidurlHAScommand() throws
	// Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(true,
	// "java HelloWord.java",
	// " h ttp://myapp.^cloudfoundry.com");
	// assertValidator(INVALID_CHARACTERS_ERROR, true, validator);
	// }
	//
	// public void testDeploymentInfoWebAppNOurlNULL() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, null);
	// assertValidator(EMPTY_URL_ERROR, true, validator);
	// }
	//
	// public void testDeploymentInfoWebAppNOurlEmpty() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "");
	// assertValidator(EMPTY_URL_ERROR, true, validator);
	// }
	//
	// public void testDeploymentInfoWebAppNOurlSpaces() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "  	");
	// assertValidator(EMPTY_URL_ERROR, true, validator);
	// }
	//
	// public void testDeploymentInfoWebAppInvalidurlname() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, " h ttp:>vin valid  	");
	// assertValidator(INVALID_CHARACTERS_ERROR, true, validator);
	// }
	//
	// public void testDeploymentInfoWebAppInvalidurlname2() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "myapp .cloudfoundry.com");
	// assertValidator(INVALID_CHARACTERS_ERROR, true, validator);
	// }
	//
	// public void testDeploymentInfoWebAppInvalidurlname3() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, " myapp.cloudfoundry.com");
	// assertValidator(INVALID_CHARACTERS_ERROR, true, validator);
	// }
	//
	// public void testDeploymentInfoWebAppInvalidurlname4() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "http://myapp.cloudfoundry.com");
	// assertValidator(INVALID_CHARACTERS_ERROR, true, validator);
	// }
	//
	// public void testDeploymentInfoWebAppInvalidurlname5() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "http:myapp.cloudfoundry.com");
	// assertValidator(INVALID_CHARACTERS_ERROR, true, validator);
	// }
	//
	// public void testDeploymentInfoWebAppInvalidurlname6() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "cloudfoundry?");
	// assertValidator(INVALID_CHARACTERS_ERROR, true, validator);
	// }
	//
	// public void testDeploymentInfoWebAppValidurlname() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "myapp.cloudfoundry.com");
	// assertValidator(Status.OK_STATUS.getMessage(), false, validator);
	// }
	//
	// public void testDeploymentInfoWebAppValidurlname2() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "$myapp.cloudfoundry.com");
	// assertValidator(Status.OK_STATUS.getMessage(), false, validator);
	// }
	//
	// public void testDeploymentInfoWebAppValidurlname3() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "myapp$.cloudfoundry.com");
	// assertValidator(Status.OK_STATUS.getMessage(), false, validator);
	// }
	//
	// public void testDeploymentInfoWebAppValidurlname4() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "4myapp.cloudfoundry.com");
	// assertValidator(Status.OK_STATUS.getMessage(), false, validator);
	// }
	//
	// public void testDeploymentInfoWebAppValidurlname5() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "4myapp.cloudfoundry.com4");
	// assertValidator(Status.OK_STATUS.getMessage(), false, validator);
	// }
	//
	// public void testDeploymentInfoWebAppValidurlname6() throws Exception {
	// ApplicationUrlValidator validator = getDeploymentInfoValidator(false,
	// null, "cloudfoundry");
	// assertValidator(Status.OK_STATUS.getMessage(), false, validator);
	// }

	protected boolean isEmpty(String value) {
		return ValueValidationUtil.isEmpty(value);
	}

	protected boolean isInvalidWithValidator(String value) {
		return new URLNameValidation(value).hasInvalidCharacters();
	}

	protected ApplicationUrlValidator getDeploymentInfoValidator(boolean isStandalone, String startCommand, String url) {
		return new ApplicationUrlValidator();
	}

	protected void assertValidator(String expectedMessage, boolean expectedError, ApplicationUrlValidator validator,
			String url) throws Exception {
		IStatus status = validator.isValid(url);
		assertEquals(expectedError, status.getSeverity() == IStatus.ERROR);
		String actualMessage = status.getMessage();
		assertEquals(expectedMessage, actualMessage);
	}

}
