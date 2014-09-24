package org.cloudfoundry.ide.eclipse.server.ui.internal.actions;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryProjectUtil;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.ICloudFoundryOperation;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.Module;

/**
 * It is a menu action responsible for replacing application in 
 * the Cloud Foundry with existing local project.
 * 
 * 
 */
@SuppressWarnings("restriction")
public class ReplaceCloudApplicationAction extends CloudFoundryEditorAction {
	private final CloudFoundryApplicationModule cloudAppModule;
	private IModule createdModule;
	protected IProject selectProj;
	
	/**
	 * Constructs an instance of <code>ReplaceCloudApplicationAction</code>.
	 * 
	 * @param cloudAppModule the selected cloud application module to replace with
	 * a select project
	 * @param editorPage the related editor page
	 */
	public ReplaceCloudApplicationAction(CloudFoundryApplicationModule cloudAppModule, 
        CloudFoundryApplicationsEditorPage editorPage) {
		super(editorPage, RefreshArea.ALL);
		this.cloudAppModule = cloudAppModule;
		setText("Replace");
		setImageDescriptor(CloudFoundryImages.REPLACE_CLOUD_APP);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getJobName() {
		StringBuffer buff = new StringBuffer();
		buff.append("Replace cloud application ")
		    .append(cloudAppModule.getDeployedApplicationName());
		return buff.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		final boolean[] prepareFinished = new boolean[1];
		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				if (selectReplaceProject() && resetModuleInfo()) {
					prepareFinished[0] = true;
				}
			}
			
		});
		if (prepareFinished[0]) {
			return getEditorPage().getCloudServer().getBehaviour()
			    .getReplaceCloudApplicationOperation(new IModule[] {createdModule});
		}
		return null;
	}
	
	/**
	 * Resets the module information of the selected <code>CloudApplicationModule</code>
	 * instance in the server. 
	 * 
	 * @return true is the reset operation is successful, otherwise false.
	 */
	protected boolean resetModuleInfo() {
		IModule[] modules = ServerUtil.getModules(selectProj);
		if (modules != null && modules.length == 1) {
			try {
				IServer server = getEditorPage().getCloudServer().getServer();
				ServerWorkingCopyFacade wc = new ServerWorkingCopyFacade(server.createWorkingCopy());
				IModule[] add = new IModule[] { modules[0] };
				if (!ServerUtil.containsModule(server, modules[0], null)) {
					wc.modifyModules(add, null, null);
					wc.save(false, null);
				}
				//should realize delete cloud module but doesn't delete it in cf.
				IModule[] delete = new IModule[] { cloudAppModule };
				wc.deleteModulesLocally(delete, null);
				wc.save(false, null);
				createdModule = add[0];
				cloudAppModule.setLocalModule(createdModule);
				return true;
			} catch (final CoreException ce) {
				showMessage(ce.getMessage(), true);
				CloudFoundryPlugin.log(ce);
			}
		} else {
			// Re-import the project to generate .settings which contains the metadata 
			// 'org.eclipse.wst.common.project.facet.core.xml' used to create Module instance.
			// Here remind user to do this is to avoid the situation that without generated 
			// module there will be no further operation of updating the selected cloud application.
			String logMsg = NLS.bind("Can't create module for the project {0}.", selectProj.getName());
			showMessage(logMsg + "Please re-import the project to fix the problem", false);
			CloudFoundryPlugin.logWarning(logMsg);
		}
		return false;
	}
	
	/**
	 * Chooses the project to replace current selected cloud application.
	 * 
	 * @return true if the replace project is selected, false indicates the user aborts
	 *    the selection operation
	 */
	protected boolean selectReplaceProject() {
		Class<?>[] acceptedClasses = new Class[] { IJavaProject.class, IProject.class };
		TypedElementSelectionValidator selectionValidator = new TypedElementSelectionValidator(acceptedClasses, false);

		acceptedClasses = new Class[] { IJavaProject.class, IProject.class };
		ViewerFilter viewerFilter = new TypedViewerFilter(acceptedClasses) {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean select(Viewer viewer, Object parent, Object element) {
				IProject potentialProject = null;
				if (element instanceof IJavaProject) {
					potentialProject = ((IJavaProject)element).getProject();
				} else if (element instanceof IProject 
					&& CloudFoundryProjectUtil.isJavaProject((IProject)element)) {
					potentialProject = (IProject)element;
				}
				
				if (null != potentialProject && !getEditorPage().getCloudServer()
					.getBehaviour().existBoundModule(potentialProject)) {
					return true;
				}
				return false;
			}
			
		};

		StandardJavaElementContentProvider contentProvider = new StandardJavaElementContentProvider();
		ILabelProvider labelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), labelProvider, contentProvider);
		dialog.setValidator(selectionValidator);
		dialog.setComparator(new JavaElementComparator());
		dialog.setTitle("Replacing Project Selection");
		dialog.setMessage(NLS.bind("Select a project to replace the cloud application {0}", cloudAppModule.getDeployedApplicationName()));
		dialog.addFilter(viewerFilter);
		dialog.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
		dialog.setInitialSelection(null);
		dialog.setHelpAvailable(false);

		selectProj = null;
		if (dialog.open() == Window.OK) {
			Object selectObj = dialog.getFirstResult();
			if (selectObj instanceof IJavaProject) {
				selectProj = ((IJavaProject)selectObj).getProject();
			} else if (selectObj instanceof IProject) {
				selectProj = (IProject)selectObj;
			}
			final boolean[] result = new boolean[1];
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					result[0] = MessageDialog.openConfirm(
							getShell(), 
							"Cloud application replacement confirmation", 
							NLS.bind(
									"Are you sure to use the contents of project {0} to replace the cloud application {1}",
									selectProj.getName(), 
									cloudAppModule.getDeployedApplicationName()));
				}
			});
			if (result[0]) {
				return true;
			} else {
				selectProj = null;
			}
		} 
		return false;
	}
	
	/**
	 * Gets the <code>Shell</code> instance for current UI components
	 * used in <code>ReplaceCloudApplicationAction</code>.
	 * 
	 * @return the <code>Shell</code> instance for current UI components
	 */
	protected Shell getShell() {
		return getEditorPage().getSite().getShell();
	}
	
	/**
	 * Presents the given message in a dialog.
	 * 
	 * @param msg the message to be shown
	 * @param isErrorMsg true indicates 
	 */
	protected void showMessage(final String msg, final boolean isErrorMsg) {
		final Shell shell = getShell();
		shell.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (isErrorMsg) {
					MessageDialog.openError(getShell(), "", msg);
				} else {
					MessageDialog.openInformation(getShell(), "", msg);
				}
			}
		});
	}
}
