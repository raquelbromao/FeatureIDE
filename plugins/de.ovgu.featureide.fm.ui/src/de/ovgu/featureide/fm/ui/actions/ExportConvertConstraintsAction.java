/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2013  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.ui.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * Action for exporting models using ComplexConstraintConverter. 
 * 
 * @author Arthur Hammer
 */
public class ExportConvertConstraintsAction implements IObjectActionDelegate {

	private ISelection selection;

	public void run(IAction action) {
		if (!(selection instanceof IStructuredSelection))
			return;

		Iterator<?> it = ((IStructuredSelection) selection).iterator();
		Object element = it.next();
		final IFile inputFile = getInputFile(element);
		if (inputFile == null)
			return;
		
		ExportConvertConstraintsWizard wizard = new ExportConvertConstraintsWizard(inputFile);
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
		dialog.create();
		dialog.open(); 
	}


	/**
	 * Gets the feature model file.
	 * 
	 * @param element The element that is selected when the action is performed
	 * @return The corresponding file
	 */
	private IFile getInputFile(Object element) {
		IFile inputFile = null;
		if (element instanceof IFile) {
			inputFile = (IFile) element;
		} else if (element instanceof IAdaptable) {
			inputFile = (IFile) ((IAdaptable) element).getAdapter(IFile.class);
		}
		return inputFile;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
}
