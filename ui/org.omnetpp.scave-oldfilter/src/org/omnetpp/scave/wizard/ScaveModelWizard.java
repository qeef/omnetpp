/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.omnetpp.scave.wizard;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.edit.ui.provider.ExtendedImageRegistry;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.omnetpp.scave.model.Analysis;
import org.omnetpp.scave.model.InputFile;
import org.omnetpp.scave.model.Inputs;
import org.omnetpp.scave.model.ScaveModelFactory;
import org.omnetpp.scave.model.ScaveModelPackage;
import org.omnetpp.scave.model.provider.ScaveEditPlugin;


/**
 * This is a simple wizard for creating a new analysis file.
 * Code is based on the EMF-created wizard.
 * 
 * The new analisis file may be empty or may contain
 * a specified input file.
 */
//XXX uses property file from the model package
public class ScaveModelWizard extends Wizard implements INewWizard {
	
	private static final String ENCODING = "UTF-8";
	private static final String FILENAME_EXTENSION = "scave";

	/**
	 * This is the file creation page.
	 */
	protected NewFileCreationPage newFileCreationPage;

	/**
	 * Remember the selection during initialization for populating the default container.
	 */
	protected IStructuredSelection selection;

	/**
	 * Remember the workbench during initialization.
	 */
	protected IWorkbench workbench;

	/**
	 * Result file that the new analysis file should contain.
	 */
	protected IFile initialInputFile;

	/**
	 * This just records the information.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
		setWindowTitle(ScaveEditPlugin.INSTANCE.getString("_UI_Wizard_label"));
		setDefaultPageImageDescriptor(ExtendedImageRegistry.INSTANCE.getImageDescriptor(ScaveEditPlugin.INSTANCE.getImage("full/wizban/NewScaveModel")));
	}
	
	public void setInitialInputFile(IFile file) {
		initialInputFile = file;
	}
	
	/**
	 * The framework calls this to create the contents of the wizard.
	 */
	public void addPages() {
		// Create a page, set the title, and the initial model file name.
		//
		newFileCreationPage = new NewFileCreationPage("Whatever", selection);
		newFileCreationPage.setTitle(ScaveEditPlugin.INSTANCE.getString("_UI_ScaveModelModelWizard_label"));
		newFileCreationPage.setDescription(ScaveEditPlugin.INSTANCE.getString("_UI_ScaveModelModelWizard_description"));
		newFileCreationPage.setFileName(getDefaultAnalysisFileName());
		addPage(newFileCreationPage);

		// Try and get the resource selection to determine a current directory for the file dialog.
		//
		if (selection != null && !selection.isEmpty()) {
			// Get the resource...
			//
			Object selectedElement = selection.iterator().next();
			if (selectedElement instanceof IResource) {
				// Get the resource parent, if its a file.
				//
				IResource selectedResource = (IResource)selectedElement;
				if (selectedResource.getType() == IResource.FILE) {
					selectedResource = selectedResource.getParent();
				}

				// This gives us a directory...
				//
				if (selectedResource instanceof IFolder || selectedResource instanceof IProject) {
					// Set this for the container.
					//
					newFileCreationPage.setContainerFullPath(selectedResource.getFullPath());

					// Make up a unique new name here.
					//
					String defaultModelBaseFilename = initialInputFile != null ? initialInputFile.getName() : "new";
					String defaultModelFilenameExtension = FILENAME_EXTENSION;
					String modelFilename = defaultModelBaseFilename + "." + defaultModelFilenameExtension;
					for (int i = 1; ((IContainer)selectedResource).findMember(modelFilename) != null; ++i) {
						modelFilename = defaultModelBaseFilename + i + "." + defaultModelFilenameExtension;
					}
					newFileCreationPage.setFileName(modelFilename);
				}
			}
		}
	}
	
	public String getDefaultAnalysisFileName() {
		String baseFileName = initialInputFile != null ? initialInputFile.getName() : "new";
		return baseFileName + ".scave";
	}
	
	/**
	 * Get the file from the page.
	 */
	public IFile getAnalysisFile() {
		return newFileCreationPage.getAnalysisFile();
	}

	/**
	 * Create a new analysis file.
	 */
	protected EObject createAnalysisNode() {
		ScaveModelFactory factory = ScaveModelFactory.eINSTANCE;
		Analysis analysis = factory.createAnalysis();
		Inputs inputs = factory.createInputs();
		analysis.setInputs(inputs);
		analysis.setDatasets(factory.createDatasets());
		analysis.setChartSheets(factory.createChartSheets());
		
		if (initialInputFile != null) {
			InputFile inputFile = factory.createInputFile();
			inputFile.setName(initialInputFile.getFullPath().toString());
			inputs.getInputs().add(inputFile);
		}
		
		return analysis;
	}

	/**
	 * Do the work after everything is specified.
	 */
	public boolean performFinish() {
		try {
			// Remember the file.
			//
			final IFile modelFile = getAnalysisFile();

			// Do the work within an operation.
			//
			WorkspaceModifyOperation operation =
				new WorkspaceModifyOperation() {
					protected void execute(IProgressMonitor progressMonitor) {
						try {
							// Create a resource set
							//
							ResourceSet resourceSet = new ResourceSetImpl();

							// Get the URI of the model file.
							//
							URI fileURI = URI.createPlatformResourceURI(modelFile.getFullPath().toString());

							// Create a resource for this file.
							//
							Resource resource = resourceSet.createResource(fileURI);

							// Add the initial model object to the contents.
							//
							EObject rootObject = createAnalysisNode();
							if (rootObject != null) {
								resource.getContents().add(rootObject);
							}

							// Save the contents of the resource to the file system.
							//
							Map options = new HashMap();
							options.put(XMLResource.OPTION_ENCODING, ENCODING);
							resource.save(options);
						}
						catch (Exception exception) {
							ScaveEditPlugin.INSTANCE.log(exception);
						}
						finally {
							progressMonitor.done();
						}
					}
				};

			getContainer().run(false, false, operation);

			// Select the new file resource in the current view.
			//
			IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
			IWorkbenchPage page = workbenchWindow.getActivePage();
			final IWorkbenchPart activePart = page.getActivePart();
			if (activePart instanceof ISetSelectionTarget) {
				final ISelection targetSelection = new StructuredSelection(modelFile);
				getShell().getDisplay().asyncExec
					(new Runnable() {
						 public void run() {
							 ((ISetSelectionTarget)activePart).selectReveal(targetSelection);
						 }
					 });
			}

			// Open an editor on the new file.
			//
			try {
				page.openEditor
					(new FileEditorInput(modelFile),
					 workbench.getEditorRegistry().getDefaultEditor(modelFile.getFullPath().toString()).getId());
			}
			catch (PartInitException exception) {
				MessageDialog.openError(workbenchWindow.getShell(), ScaveEditPlugin.INSTANCE.getString("_UI_OpenEditorError_label"), exception.getMessage());
				return false;
			}

			return true;
		}
		catch (Exception exception) {
			ScaveEditPlugin.INSTANCE.log(exception);
			return false;
		}
	}

	/**
	 * This is the one page of the wizard.
	 */
	public class NewFileCreationPage extends WizardNewFileCreationPage {
		/**
		 * Pass in the selection.
		 */
		public NewFileCreationPage(String pageId, IStructuredSelection selection) {
			super(pageId, selection);
		}

		/**
		 * The framework calls this to see if the file is correct.
		 */
		protected boolean validatePage() {
			if (super.validatePage()) {
				// Make sure the file ends in ".scavemodel".
				//
				String requiredExt = ScaveEditPlugin.INSTANCE.getString("_UI_ScaveModelEditorFilenameExtension");
				String enteredExt = new Path(getFileName()).getFileExtension();
				if (enteredExt == null || !enteredExt.equals(requiredExt)) {
					setErrorMessage(ScaveEditPlugin.INSTANCE.getString("_WARN_FilenameExtension", new Object [] { requiredExt }));
					return false;
				}
				else {
					return true;
				}
			}
			else {
				return false;
			}
		}

		/**
		 * 
		 */
		public IFile getAnalysisFile() {
			return ResourcesPlugin.getWorkspace().getRoot().getFile(getContainerFullPath().append(getFileName()));
		}
	}
}
