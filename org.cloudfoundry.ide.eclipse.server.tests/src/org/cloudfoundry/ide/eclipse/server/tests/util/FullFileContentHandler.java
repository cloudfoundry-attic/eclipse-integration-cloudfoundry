package org.cloudfoundry.ide.eclipse.server.tests.util;

import java.io.IOException;
import java.io.OutputStream;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.FileContent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.console.FileContentHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.springframework.http.HttpStatus;

public class FullFileContentHandler extends FileContentHandler {

	public FullFileContentHandler(FileContent fileContent, OutputStream outputStream, String appName, int instanceIndex) {
		super(fileContent, outputStream, appName, instanceIndex);
	}

	@Override
	public String getContent(IProgressMonitor monitor) throws CoreException {

		// For testing purposes only, bypass the offset-based content fetching
		// from the file content,
		// and instead fetch the full contents of the file directly from the
		// server behaviour
		CloudFoundryServer server = fileContent.getServer();
		String path = fileContent.getPath();
		String content = null;
		try {
			content = server.getBehaviour().getFile(appName, instanceIndex, path, monitor);
			if (outputStream != null && content != null && content.length() > 0) {
				outputStream.write(content.getBytes());
			}
		}
		catch (CoreException e) {
			Throwable t = e.getCause();
			// Ignore errors due to specified start position being past the
			// content length (i.e there is no new content). Otherwise rethrow
			// error
			if (t == null || !(t instanceof CloudFoundryException)
					|| !HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.equals(((CloudFoundryException) t).getStatusCode())) {
				throw e;
			}
		}
		catch (IOException ioe) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(ioe));
		}

		if (content != null) {
			offset = content.length();
		}
		return content;
	}

}
