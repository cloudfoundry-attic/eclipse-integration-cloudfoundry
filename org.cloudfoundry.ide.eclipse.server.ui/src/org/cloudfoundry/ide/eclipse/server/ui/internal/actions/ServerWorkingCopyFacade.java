package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryServerUiPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.ProgressUtil;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerWorkingCopy;

/**
 * Acts as a facade for an instance of <code>IServerWorkingCopy</code>.
 * <p>
 * Besides all functions of <code>IServerWorkingCopy</code>, it also provides
 * the function to delete module just in the local server but not trigger the
 * server delegate to delete the related cloud application in CF.
 * 
 * 
 *
 */
@SuppressWarnings("restriction")
public class ServerWorkingCopyFacade extends ServerWorkingCopy {

	/**
	 * Constructs an instance of <code>ServerWorkingCopyFacade</code>.
	 * 
	 * @param actualWC the actual <code>IServerWorkingCopy</code> instance to be facaded
	 */
	public ServerWorkingCopyFacade(IServerWorkingCopy actualWC) {
		super((Server)actualWC.getOriginal());
	}
	
	/**
	 * Delete the given modules just in local server, and doesn't trigger the
	 * server delegate to delete the cloud application in CF.
	 * 
	 * @param removes the modules to be removed locally
	 * @param monitor the progress monitor
	 * @throws CoreException error occurs when delete the given modules locally
	 */
	public void deleteModulesLocally(IModule[] removes, IProgressMonitor monitor) throws CoreException {
		IStatus status = canModifyModules(null, removes, monitor);
		if (status != null && status.getSeverity() == IStatus.ERROR)
			throw new CoreException(status);
		
		try {
			monitor = ProgressUtil.getMonitorFor(monitor);
			monitor.subTask("Delete module locally");
			
			//<=>wch.setDirty(true);
			Class<?> wchClazz = Class.forName("org.eclipse.wst.server.core.internal.WorkingCopyHelper");
			Method setDirtyMethod = wchClazz.getDeclaredMethod("setDirty", boolean.class);
			setDirtyMethod.setAccessible(true);
			setDirtyMethod.invoke(wch, true);
			
			// trigger load of modules list
			synchronized (modulesLock){
				getModulesWithoutLock();
				
				if (removes != null) {
					externalModules = getExternalModules();
					for (IModule module : removes) {
						if (modules.contains(module)) {
							modules.remove(module);
							resetState(new IModule[] { module }, monitor);
						}
						if (externalModules != null && externalModules.contains(module)) {
							externalModules.remove(module);
							resetState(new IModule[] { module }, monitor);
						}
					}
				}
				
				// convert to attribute
				List<String> list = new ArrayList<String>();
				Iterator<IModule> iterator = modules.iterator();
				while (iterator.hasNext()) {
					IModule module = iterator.next();
					StringBuffer sb = new StringBuffer(module.getName());
					sb.append("::");
					sb.append(module.getId());
					IModuleType mt = module.getModuleType();
					if (mt != null) {
						sb.append("::");
						sb.append(mt.getId());
						sb.append("::");
						sb.append(mt.getVersion());
					}
					list.add(sb.toString());
				}
				setAttribute(MODULE_LIST, list);
			}

			resetOptionalPublishOperations();
			resetPreferredPublishOperations();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, 0, "" + e.getLocalizedMessage(), e));
		}
	}
}
