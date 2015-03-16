/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	public static String AbstractApplicationDelegate_ERROR_MISSING_APPNAME;

	public static String AbstractApplicationDelegate_ERROR_MISSING_DEPLOY_INFO;

	public static String AbstractApplicationDelegate_ERROR_MISSING_MEM;

	public static String ApplicationLogConsoleManager_NO_RECENT_LOGS;

	public static String ApplicationUrlLookupService_ERROR_GET_CLOUD_URL;

	public static String ApplicationUrlLookupService_ERROR_GETDEFAULT_APP_URL;

	public static String ERROR_PERFORMING_CLOUD_FOUNDRY_OPERATION;

	public static String ERROR_WRONG_EMAIL_OR_PASSWORD_UNAUTHORISED;

	public static String ERROR_WRONG_EMAIL_OR_PASSWORD_FORBIDDEN;

	public static String ERROR_ACCESS_TOKEN;

	public static String ERROR_UNABLE_TO_ESTABLISH_CONNECTION_UNKNOWN_HOST;

	public static String ERROR_FAILED_REST_CLIENT;

	public static String ERROR_NO_DOMAINS;

	public static String ERROR_NO_DOMAINS_AVAILABLE;

	public static String ERROR_NO_DOMAIN_RESOLVED_FOR_URL;

	public static String ERROR_INVALID_SUBDOMAIN;

	public static String ERROR_MANIFEST_PARSING_NO_DOMAINS;

	public static String ERROR_FAILED_RESOLVE_ORGS_SPACES_DUE_TO_ERROR;

	public static String ERROR_UNABLE_TO_COMMUNICATE_SERVER;

	public static String ERROR_CREATE_ZIP;

	public static String ERROR_UNKNOWN;

	public static String ERROR_FAILED_MEMORY_UPDATE;

	public static String ERROR_FAILED_READ_SELF_SIGNED_PREFS;

	public static String ERROR_FAILED_STORE_SELF_SIGNED_PREFS;

	public static String ERROR_UNABLE_CONNECT_SERVER_CREDENTIALS;

	public static String ERROR_FAILED_CLIENT_CREATION_NO_SPACE;

	public static String ERROR_FAILED_MODULE_REFRESH;


	public static String ERROR_APP_DEPLOYMENT_VALIDATION_ERROR;

	public static String ERROR_FAILED_TO_PUSH_APP;

	public static String ERROR_NO_CLOUD_APPLICATION_FOUND;

	public static String ERROR_NO_MODULES_TO_PUBLISH;

	public static String ERROR_NO_CLOUD_SERVER_DESCRIPTOR;

	public static String ERROR_NO_CLIENT;

	public static String ERROR_NO_CONSOLE_STREAM_FOUND;

	public static String ERROR_EXISTING_APPLICATION_LOGS;

	public static String ERROR_APPLICATION_LOG_LISTENER;

	public static String ERROR_NO_MAPPED_APPLICATION_URLS;

	public static String ERROR_HOST_TAKEN;

	public static String INVALID_CHARACTERS_ERROR;

	public static String WARNING_SELF_SIGNED_PROMPT_USER;

	public static String TITLE_SELF_SIGNED_PROMPT_USER;

	public static String TunnelServiceCommandStore_ERROR_SERIALIZE_JAVAMAP;

	public static String TunnelServiceCommandStore_ERROR_VALUE_CANNOT_SERILIZE;

	public static String ClientRequest_RETRY_REQUEST;

	public static String ClientRequest_SECOND_ATTEMPT_FAILED;

	public static String ClientRequest_TOKEN_EXPIRED;

	public static String ClientRequest_NO_TOKEN;

	public static String CloudBehaviourOperations_REFRESHING_APPS_AND_SERVICES;

	public static String CloudFoundryApplicationModule_STATE_DEPLOYABLE;

	public static String CloudFoundryApplicationModule_STATE_DEPLOYED;

	public static String CloudFoundryApplicationModule_STATE_LAUNCHED;

	public static String CloudFoundryApplicationModule_STATE_LAUNCHING;

	public static String CloudFoundryApplicationModule_STATE_STARTING_SERVICES;

	public static String CloudFoundryApplicationModule_STATE_STOPPED;

	public static String CloudFoundryApplicationModule_STATE_STOPPING;

	public static String CloudFoundryApplicationModule_STATE_UPLOADING;

	public static String CloudFoundryApplicationModule_STATE_WAITING_TO_LAUNCH;

	public static String CloudFoundryConstants_LABEL_SIGNUP;

	public static String CloudFoundryLoginHandler_LABEL_PERFORM_CF_OPERATION;

	public static String CloudFoundryServer_UPDATING_MODULE;

	public static String CloudFoundryServer_ERROR_APPTYPE_NOT_SUPPORTED;

	public static String CloudFoundryServer_ERROR_FAIL_ON_CFAPP_CREATION;

	public static String CloudFoundryServer_ERROR_SERVER_ORIGIN_NOT_FOUND;
	
	public static String CloudFoundryServer_ERROR_UNABLE_REPLACE_MODULE_NO_CLOUD_APP;

	public static String CloudFoundryServer_JOB_UPDATE;

	public static String CloudFoundryServerBehaviour_APP_STATS;

	public static String CloudFoundryServerBehaviour_APP_INFO;

	public static String CloudFoundryServerBehaviour_CREATE_SERVICES;

	public static String CloudFoundryServerBehaviour_CREATING_SERVICE;

	public static String CloudFoundryServerBehaviour_DELETE_SERVICES;

	public static String CloudFoundryServerBehaviour_DELETING_SERVICE;

	public static String CloudFoundryServerBehaviour_DOMAINS_FOR_SPACE;

	public static String CloudFoundryServerBehaviour_GET_ALL_APPS;

	public static String CloudFoundryServerBehaviour_GET_ALL_SERVICES;

	public static String CloudFoundryServerBehaviour_GET_APPLICATION;

	public static String CloudFoundryServerBehaviour_UPDATE_APP_MEMORY;

	public static String CloudFoundryServerBehaviour_UPDATE_APP_URLS;

	public static String CloudFoundryServerBehaviour_UPDATE_ENV_VARS;

	public static String CloudFoundryServerBehaviour_UPDATE_SERVICE_BINDING;

	public static String CloudFoundryServerBehaviour_WAITING_APP_START;

	public static String CommandOptions_DESCRIPTION_VARIABLES_FOR_TUNNEL;

	public static String CONSOLE_ERROR_MESSAGE;

	public static String CONSOLE_GENERATING_ARCHIVE;

	public static String CONSOLE_APP_STOPPED;

	public static String PushApplicationOperation_PUSH_MESSAGE;

	public static String PushApplicationOperation_UPDATE_APP_MESSAGE;

	public static String CONSOLE_APP_CREATION;

	public static String CONSOLE_APP_FOUND;

	public static String CONSOLE_APP_MAPPING_STARTED;

	public static String CONSOLE_APP_MAPPING_COMPLETED;

	public static String CONSOLE_APP_PUSHED_MESSAGE;

	public static String CONSOLE_PREPARING_APP;

	public static String CONSOLE_STILL_WAITING_FOR_APPLICATION_TO_START;

	public static String CONSOLE_WAITING_FOR_APPLICATION_TO_START;

	public static String CONSOLE_STOPPING_APPLICATION;

	public static String LocalServerRequest_SERVER_LABEL;

	public static String ManifestParser_READING;

	public static String ManifestParser_NO_APP_NAME;

	public static String ManifestParser_WRITING;

	public static String REFRESHING_MODULES;

	public static String RefreshModulesHandler_REFRESH_FAILURE;

	public static String RefreshModulesHandler_REFRESH_JOB;

	public static String RefreshModulesHandler_EVENT_CLOUD_SERVER_NULL;
	
	public static String RemapModuleProjectCommand_JOB_LABEL;

	public static String RestartOperation_STARTING_APP;

	public static String PUBLISHING_MODULE;

	public static String StopApplicationOperation_STOPPING_APP;

	public static String DELETING_MODULE;

	public static String VALIDATING_CREDENTIALS;

	public static String ROUTES;

	public static String EMPTY_URL_ERROR;

	public static String JavaWebApplicationDelegate_ERROR_NO_MAPPED_APP_URL;

	public static String ModuleResourceApplicationArchive_ERROR_NO_DEPLOYABLE_RES_FOUND;

	private static final String BUNDLE_NAME = CloudFoundryPlugin.PLUGIN_ID + ".internal.Messages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
