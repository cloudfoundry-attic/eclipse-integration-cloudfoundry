package org.cloudfoundry.ide.eclipse.server.ui.internal.wizards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.server.ui.internal.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.ICoreRunnable;
import org.cloudfoundry.ide.eclipse.server.ui.internal.Messages;
import org.cloudfoundry.ide.eclipse.server.ui.internal.UIPart;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.IModule;

/**
 * 
 * Supports wizard page with choices of publishing directly and replace 
 * existing project to the user, who has just dragged and dropped a local
 * project to the application viewer.
 *
 */
public class DragAndDropProjectProcessChoiceWizard extends Wizard {

	private final CloudFoundryServer cloudServer;
	private ProjectProcessChoicePage projProChoicePage;
	private boolean choosePublish = false;
	
	/**
	 * Constructs an instance of <code>DragAndDropProjectProcessChoiceWizard</code>.
	 * 
	 * @param cloudServer the cloud foundry server instance.
	 */
	public DragAndDropProjectProcessChoiceWizard(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		setWindowTitle(cloudServer.getServer().getName());
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addPages() {
		projProChoicePage = new ProjectProcessChoicePage(cloudServer);
		projProChoicePage.setWizard(this);
		addPage(projProChoicePage);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean performFinish() {
		choosePublish = false;
		if (projProChoicePage.choosePublish()) {
			choosePublish = true;
			return true;
		}
		if (projProChoicePage.chooseReplace() 
			&& null != projProChoicePage.getReplaceApplication()) {
			
			return true;
		}
		return false;
	}

	/**
	 * Judges the whether the user has chosen an option to publish the project
	 * as a new application or not.
	 * 
	 * <p>
	 * This return value is meaningful if the corresponding Dialog.<code>open()
	 * </code> returns <code>Window.OK</code>.
	 * 
	 * @return true indicates the user has chosen to publish it, false otherwise
	 */
	public boolean choosePublish() {
		return choosePublish;
	}
	
	/**
	 * Gets the <code>CloudFoundryApplicationModule</code> instance of an existing application 
	 * whose contents is about to be replaced.
	 * <p>
	 * Notes the return value is meaningful if the corresponding Dialog.<code>open()
	 * </code> returns <code>Window.OK</code> and <code>choosePublish</code> returns false.
	 * 
	 * @return the <code>CloudFoundryApplicationModule</code> instance of an existing application 
	 * whose contents is about to be replaced
	 */
	public CloudFoundryApplicationModule getReplacedApplication() {
		return cloudServer.getExistingCloudModule(projProChoicePage.getReplaceApplication());
	}
	
	/**
	 * 
	 * The wizard page provides user two options to process the newly drag-and-drop
	 * project.
	 *
	 */
	private static class ProjectProcessChoicePage extends PartsWizardPage {
		
		private final CloudFoundryServer cloudServer;
		private ProjectProcessChoicePart projProcessChoicePart;
		
		/**
		 * Constructs an instance of <code>ProjectProcessChoicePage</code>.
		 * 
		 * @param cloudServer the cloud foundry server instance.
		 */
		public ProjectProcessChoicePage(CloudFoundryServer cloudServer) {
			super("Drag And Drog Project Process Choice Page", Messages.DND_PROJECT_PROCESS_PAGE_TITLE, 
				CloudFoundryImages.getWizardBanner(cloudServer.getServer()
					.getServerType().getId()));
			setDescription(Messages.DND_PROJECT_PROCESS_PAGE_DESCRIPTION);
			this.cloudServer = cloudServer;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isPageComplete() {
			return super.isPageComplete() && (choosePublish() 
				|| (chooseReplace() && null != getReplaceApplication()));
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void createControl(Composite parent) {
			projProcessChoicePart = new ProjectProcessChoicePart(this.getWizard());
			projProcessChoicePart.addPartChangeListener(this);
			Control control = projProcessChoicePart.createPart(parent);
			setControl(control);
		}
		
		boolean choosePublish() {
			return projProcessChoicePart.choosePublish();
		}
		
		boolean chooseReplace() {
			return projProcessChoicePart.chooseReplace();
		}
		
		IModule getReplaceApplication() {
			return projProcessChoicePart.getReplaceApplication();
		}
		
		@Override
		protected void performWhenPageVisible() {
			updateApplications();
		}
		
		private void updateApplications() {
			runAsynchWithWizardProgress(new ICoreRunnable() {

				public void run(IProgressMonitor monitor) throws CoreException {

					final List<IModule> existModules = cloudServer.getBehaviour().getExistModules();
					Display.getDefault().syncExec(new Runnable() {

						public void run() {
							projProcessChoicePart.setModules(existModules);
						}

					});
				}

			}, Messages.REFRESHING_APPLICATION);
		}
	}
	
	/**
	 * 
	 * The actual ui part presenting in the <code>ProjectProcessChoicePage</code>
	 * instance.
	 *
	 */
	private static class ProjectProcessChoicePart extends UIPart {

		private final IWizard belongedWizard;
		private Button publishButton;
		private Button replaceButton;
		private Combo cloudApplitionComobo;

		private Map<String, IModule> allModules = new HashMap<String, IModule>();
		
		private IModule replacedApplication;
		
		/**
		 * Constructs an instance of <code>ProjectProcessChoicePart</code>.
		 * 
		 * @param belongedWizard the wizard current <code>ProjectProcessChoicePart</code>
		 *	 instance belonged to
		 */
		public ProjectProcessChoicePart(IWizard belongedWizard) {
			this.belongedWizard = belongedWizard;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Control createPart(Composite parent) {
			Composite uiArea = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(uiArea);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(uiArea);
			
			publishButton = new Button(uiArea, SWT.RADIO);
			publishButton.setText(Messages.PUBLISH_NEW_APPLICATION);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(publishButton);
			publishButton.addSelectionListener(new SelectionAdapter() {
				
				/**
				 * {@inheritDoc}
				 */
				@Override
				public void widgetSelected(SelectionEvent e) {
					cloudApplitionComobo.setEnabled(false);
					cloudApplitionComobo.setText("");
					belongedWizard.getContainer().updateButtons();
				}
			});
			publishButton.setSelection(true);
			
			replaceButton = new Button(uiArea, SWT.RADIO);
			replaceButton.setText(Messages.REPLACE_EXISTING_APPLICATION);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(replaceButton);
			replaceButton.addSelectionListener(new SelectionAdapter() {
				
				/**
				 * {@inheritDoc}
				 */
				@Override
				public void widgetSelected(SelectionEvent e) {
					cloudApplitionComobo.setEnabled(true);
					belongedWizard.getContainer().updateButtons();
				}
			});
			
			cloudApplitionComobo = new Combo(uiArea, SWT.NONE | SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(cloudApplitionComobo);
			cloudApplitionComobo.setEnabled(false);
			cloudApplitionComobo.addSelectionListener(new SelectionAdapter() {
				
				/**
				 * {@inheritDoc}
				 */
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (e.getSource().equals(cloudApplitionComobo) 
						&& cloudApplitionComobo.getText() != null && !"".equals(cloudApplitionComobo.getText())) {
						replacedApplication = allModules.get(cloudApplitionComobo.getText());
					}
					belongedWizard.getContainer().updateButtons();
				}
			});
			return uiArea;
		}
	
		/**
		 * Sets the modules whose names will be presented in a 
		 * combo.
		 * 
		 * @param modules a list containing all the modules 
		 *		in current cloud foundry server.
		 */
		public void setModules(List<IModule> modules) {
			if (modules == null || modules.isEmpty()) {
				return;
			}
			allModules.clear();
			for (IModule module : modules) {
				// Currently only CloudFoundryApplicationModule 
				// without local bounded project can be replaced
				if (module instanceof CloudFoundryApplicationModule) {
					allModules.put(module.getName(), module);
				}
				
			}
			cloudApplitionComobo.setItems(allModules.keySet().toArray(new String[allModules.size()]));
		}
		
		/**
		 * Gets the application (the instance of <code>IModule</code> actually) which the user 
		 * choose to replace.
		 * <p>
		 * Note that the return value of this method is meaningful only <code>chooseReplace()</code>
		 * returns true.
		 * 
		 * @return the application module to be replace, or null there is no item to be selected
		 */
		public IModule getReplaceApplication() {
			return replacedApplication;
		}
		
		/**
		 * Judges whether the user has chosen to publish the selected project as a 
		 * new application or not.
		 * 
		 * @return true indicates the user has chosen to publish it, false otherwise
		 */
		public boolean choosePublish() {
			return publishButton.getSelection();
		}
		
		/**
		 * Judges whether the user has chosen to replace an existing cloud application with
		 * the contents of the selected project.
		 * 
		 * @return true indicates the user has chosen to replace an selected existing cloud
		 * application, false otherwise
		 */
		public boolean chooseReplace() {
			return this.replaceButton.getSelection() && null != cloudApplitionComobo.getText()
				&& !"".equals(cloudApplitionComobo.getText());
		}
	}
}
