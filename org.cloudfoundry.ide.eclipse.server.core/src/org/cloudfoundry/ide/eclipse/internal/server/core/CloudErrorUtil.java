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

import java.net.UnknownHostException;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * Utility to detect various types of HTTP errors, like 400 Bad Request errors.
 * 
 */
public class CloudErrorUtil {

	private static final String ERROR_UNKNOWN = "Unknown Cloud Foundry error";

	private CloudErrorUtil() {
		// Util class
	}

	/**
	 * 
	 * @param e error to check if it is a connection error.
	 * @return User-friendly error message IFF the error is a validation error due to wrong
	 * credentials or connection error. Return null otherwise.
	 */
	public static String getConnectionError(CoreException e) {
		if (isUnauthorisedException(e)) {
			return "Validation failed: Wrong email or password";
		}
		else if (isForbiddenException(e)) {
			return "Validation failed: Wrong email or password";
		}
		else if (isUnknownHostException(e)) {
			return "Validation failed: Unable to establish connection";
		}
		else if (isRestClientException(e)) {
			return "Validation failed: Unknown URL";
		}
		return null;
	}

	public static boolean isCloudFoundryServer(IServer server) {
		String serverId = server.getServerType().getId();
		return serverId.startsWith("org.cloudfoundry.appcloudserver.");
	}

	// check if error is caused by wrong credentials
	public static boolean isWrongCredentialsException(CoreException e) {
		Throwable cause = e.getCause();
		if (cause instanceof HttpClientErrorException) {
			HttpClientErrorException httpException = (HttpClientErrorException) cause;
			HttpStatus statusCode = httpException.getStatusCode();
			if (statusCode.equals(HttpStatus.FORBIDDEN) && httpException instanceof CloudFoundryException) {
				return ((CloudFoundryException) httpException).getDescription().equals("Operation not permitted");
			}
		}
		return false;
	}

	public static boolean isAppStoppedStateError(Exception e) {
		HttpClientErrorException badRequestException = getBadRequestException(e);
		if (badRequestException != null) {
			String message = getHttpErrorMessage(badRequestException);

			if (message != null) {
				message = message.toLowerCase();
				return message.contains("state") && message.contains("stop");
			}
		}
		return false;
	}

	/**
	 * 
	 * @param error
	 * @return Error message containing description of the error. If no
	 * description is found, it will return the exception error or HTTP status
	 * text message, if present. May return null if no message can be resolved.
	 */
	protected static String getHttpErrorMessage(HttpClientErrorException error) {
		String message = null;
		if (error instanceof CloudFoundryException) {
			message = ((CloudFoundryException) error).getDescription();
		}

		if (message == null) {
			message = error.getMessage();
			if (message == null) {
				message = error.getStatusText();
				if (message == null) {
					message = error.getResponseBodyAsString();
				}
			}
		}
		return message;
	}

	public static boolean isFileNotFoundForInstance(Exception e) {
		HttpClientErrorException badRequestException = getBadRequestException(e);
		if (badRequestException != null) {
			String message = getHttpErrorMessage(badRequestException);

			if (message != null) {
				message = message.toLowerCase();
				return message.contains("file error") && message.contains("request failed")
						&& message.contains("as the instance is not found");
			}
		}
		return false;
	}

	public static boolean isRequestedFileRangeNotSatisfiable(CloudFoundryException cfe) {
		return cfe != null && HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.equals(cfe.getStatusCode());
	}

	/**
	 * 
	 * @param e to check if it is a Bad Request 400 HTTP Error.
	 * @return determines if the given exception is a Bad Request 400 Exception.
	 * If so, returns it the corresponding HttpClientErrorException. Otherwise
	 * returns null.
	 */
	public static HttpClientErrorException getBadRequestException(Exception e) {
		if (e == null) {
			return null;
		}
		HttpClientErrorException httpException = null;
		if (e instanceof HttpClientErrorException) {
			httpException = (HttpClientErrorException) e;
		}
		else {
			Throwable cause = e.getCause();
			if (cause instanceof HttpClientErrorException) {
				httpException = (HttpClientErrorException) cause;
			}
		}

		if (httpException != null) {
			HttpStatus statusCode = httpException.getStatusCode();
			if (statusCode.equals(HttpStatus.BAD_REQUEST)) {
				return httpException;
			}
		}
		return null;
	}

	public static CoreException toCoreException(Throwable e) {
		if (e instanceof CloudFoundryException) {
			if (((CloudFoundryException) e).getDescription() != null) {
				return new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind("{0} ({1})",
						((CloudFoundryException) e).getDescription(), e.getMessage()), e));
			}
		}
		return new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind(
				"Error performing Cloud Foundry operation: {0}", e.getMessage()), e));
	}

	// check if error is 403 - take CoreException
	public static boolean isForbiddenException(CoreException e) {
		Throwable cause = e.getCause();
		if (cause instanceof HttpClientErrorException) {
			HttpClientErrorException httpException = (HttpClientErrorException) cause;
			HttpStatus statusCode = httpException.getStatusCode();
			return statusCode.equals(HttpStatus.FORBIDDEN);

		}
		return false;
	}

	// check 401 error due to invalid credentials
	public static boolean isUnauthorisedException(CoreException e) {
		Throwable cause = e.getCause();
		if (cause instanceof HttpClientErrorException) {
			HttpClientErrorException httpException = (HttpClientErrorException) cause;
			HttpStatus statusCode = httpException.getStatusCode();
			return statusCode.equals(HttpStatus.UNAUTHORIZED);
		}
		return false;
	}

	// check if error is 404 - take CoreException
	public static boolean isNotFoundException(CoreException e) {
		Throwable cause = e.getCause();
		if (cause instanceof HttpClientErrorException) {
			HttpClientErrorException httpException = (HttpClientErrorException) cause;
			HttpStatus statusCode = httpException.getStatusCode();
			return statusCode.equals(HttpStatus.NOT_FOUND);
		}
		return false;
	}

	public static boolean isUnknownHostException(CoreException e) {
		Throwable cause = e.getStatus().getException();
		if (cause instanceof ResourceAccessException) {
			return ((ResourceAccessException) cause).getCause() instanceof UnknownHostException;
		}
		return false;
	}

	public static boolean isRestClientException(CoreException e) {
		Throwable cause = e.getStatus().getException();
		return cause instanceof RestClientException;
	}

	public static CoreException toCoreException(String message) {
		return toCoreException(message, null);
	}

	public static CoreException toCoreException(String message, Throwable error) {
		if (message == null) {
			message = ERROR_UNKNOWN;
		}
		if (error != null) {
			if (error.getMessage() != null) {
				message += " - " + error.getMessage();
			}
			return new CoreException(CloudFoundryPlugin.getErrorStatus(message, error));
		}
		else {
			return new CoreException(CloudFoundryPlugin.getErrorStatus(message));
		}
	}

	public static String getCloudFoundryErrorMessage(CloudFoundryException cfe) {
		return cfe.getMessage();
	}

}
