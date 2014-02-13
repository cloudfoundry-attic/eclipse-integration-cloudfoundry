/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.runtime.CoreException;

public class CloudFoundryConsoleTest extends AbstractCloudFoundryTest {

	@Override
	protected Harness createHarness() throws CoreException {
		return CloudFoundryTestFixture.current().harness();
	}

	// public void testRangedConsoleContent() throws Exception {
	// harness.createProjectAndAddModule("dynamic-webapp");
	//
	// IModule[] modules = server.getModules();
	// assertEquals("Expected dynamic-webapp module, got " +
	// Arrays.toString(modules), 1, modules.length);
	// int moduleState = server.getModulePublishState(modules);
	// assertEquals(IServer.PUBLISH_STATE_UNKNOWN, moduleState);
	//
	// serverBehavior.deployOrStartModule(modules, true, null);
	// moduleState = server.getModuleState(modules);
	// assertEquals(IServer.STATE_STARTED, moduleState);
	//
	// ConsoleStreamContent content = getRangeFileConsoleContent(modules[0]);
	// assertNotNull(content);
	//
	// ConsoleStreamContent.Result result = getResult(content);
	// assertNotNull(result);
	//
	// String errorContent = result.getErrorContent();
	// assertTrue("Expected error log content for app " + modules[0].getName(),
	// errorContent != null && errorContent.length() > 0);
	// assertEquals(result.getErrorContent().length(),
	// result.getErrorEndingPosition());
	//
	// }
	//
	// public void testFullConsoleContent() throws Exception {
	// harness.createProjectAndAddModule("dynamic-webapp");
	//
	// IModule[] modules = server.getModules();
	// assertEquals("Expected dynamic-webapp module, got " +
	// Arrays.toString(modules), 1, modules.length);
	// int moduleState = server.getModulePublishState(modules);
	// assertEquals(IServer.PUBLISH_STATE_UNKNOWN, moduleState);
	//
	// serverBehavior.deployOrStartModule(modules, true, null);
	// moduleState = server.getModuleState(modules);
	// assertEquals(IServer.STATE_STARTED, moduleState);
	//
	// ConsoleStreamContent content = getFullFileConsoleContent(modules[0]);
	// assertNotNull(content);
	//
	// ConsoleStreamContent.Result result = getResult(content);
	// assertNotNull(result);
	//
	// String errorContent = result.getErrorContent();
	// assertTrue("Expected error log content for app " + modules[0].getName(),
	// errorContent != null && errorContent.length() > 0);
	// assertEquals(result.getErrorContent().length(),
	// result.getErrorEndingPosition());
	//
	// }
	//
	// public void testFullMatchesRangedConsoleContent() throws Exception {
	// harness.createProjectAndAddModule("dynamic-webapp");
	//
	// IModule[] modules = server.getModules();
	// assertEquals("Expected dynamic-webapp module, got " +
	// Arrays.toString(modules), 1, modules.length);
	// int moduleState = server.getModulePublishState(modules);
	// assertEquals(IServer.PUBLISH_STATE_UNKNOWN, moduleState);
	//
	// serverBehavior.deployOrStartModule(modules, true, null);
	// moduleState = server.getModuleState(modules);
	// assertEquals(IServer.STATE_STARTED, moduleState);
	//
	// ConsoleStreamContent content = getFullFileConsoleContent(modules[0]);
	// assertNotNull(content);
	//
	// ConsoleStreamContent.Result fullResult = getResult(content);
	// assertNotNull(fullResult);
	//
	// content.reset();
	//
	// content = getRangeFileConsoleContent(modules[0]);
	// assertNotNull(content);
	//
	// ConsoleStreamContent.Result rangedResult = getResult(content);
	// assertNotNull(rangedResult);
	//
	// assertEquals(fullResult.getErrorContent(),
	// rangedResult.getErrorContent());
	// assertEquals(fullResult.getErrorEndingPosition(),
	// rangedResult.getErrorEndingPosition());
	// assertEquals(fullResult.getOutContent(), rangedResult.getOutContent());
	// assertEquals(fullResult.getOutEndingPosition(),
	// rangedResult.getOutEndingPosition());
	//
	// }
	//
	// /*
	// * HELPER METHODS
	// */
	// protected ConsoleStreamContent getFullFileConsoleContent(IModule module)
	// throws CoreException {
	//
	// int instanceIndex = 0;
	// CloudApplication app = getCloudApplication(module);
	// MessageConsole console = ConsoleManager.getOrCreateConsole(server, app,
	// instanceIndex);
	//
	// return new
	// ConsoleStreamContent(ConsoleContent.getStandardLogContent(cloudServer,
	// app), console, app, instanceIndex) {
	// protected FileContentHandler getFileContentHandler(FileContent content,
	// OutputStream stream,
	// String appName, int instanceIndex) {
	// return new FullFileContentHandler(content, stream, appName,
	// instanceIndex);
	// }
	// };
	//
	// }
	//
	// protected ConsoleStreamContent getRangeFileConsoleContent(IModule module)
	// throws CoreException {
	//
	// int instanceIndex = 0;
	// CloudApplication app = getCloudApplication(module);
	// MessageConsole console = ConsoleManager.getOrCreateConsole(server, app,
	// instanceIndex);
	//
	// return new
	// ConsoleStreamContent(ConsoleContent.getStandardLogContent(cloudServer,
	// app), console, app, instanceIndex);
	//
	// }
	//
	// protected ConsoleStreamContent.Result getResult(final
	// ConsoleStreamContent content) throws Exception {
	// return new AbstractWaitWithProgressJob<ConsoleStreamContent.Result>(10,
	// 2000) {
	//
	// @Override
	// protected Result runInWait(IProgressMonitor monitor) throws CoreException
	// {
	//
	// Result result = content.getFileContent(monitor);
	// // if either error or stdout content is obtained, stop waiting
	// if ((result.getErrorContent() != null &&
	// result.getErrorContent().length() > 0)
	// || (result.getOutContent() != null && result.getOutContent().length() >
	// 0)) {
	// return result;
	// }
	// else {
	// return null;
	// }
	// }
	//
	// // Set it to fix build errors that fail because it takes too long to
	// // get a result
	// protected boolean shouldRetryOnError(Throwable t) {
	// return true;
	// }
	//
	// }.run(new NullProgressMonitor());
	// }
}
