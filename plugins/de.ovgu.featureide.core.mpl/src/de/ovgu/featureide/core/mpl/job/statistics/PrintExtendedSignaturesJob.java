/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
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
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.core.mpl.job.statistics;

import static de.ovgu.featureide.fm.core.localization.StringTable.BUILD_EXTENDED_MODULES;
import static de.ovgu.featureide.fm.core.localization.StringTable.BUILT_EXTENDED_MODULES;

import java.util.LinkedList;

import org.eclipse.core.resources.IFolder;
import org.prop4j.Node;

import de.ovgu.featureide.core.mpl.InterfaceProject;
import de.ovgu.featureide.core.mpl.MPLPlugin;
import de.ovgu.featureide.core.signature.ProjectSignatures;
import de.ovgu.featureide.core.signature.ProjectSignatures.SignatureIterator;
import de.ovgu.featureide.core.signature.ProjectStructure;
import de.ovgu.featureide.core.signature.filter.ContextFilter;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.io.IOConstants;
import de.ovgu.featureide.fm.core.job.AProjectJob;
import de.ovgu.featureide.fm.core.job.util.JobArguments;

/**
 * Builds extended signatures for different constraints.
 * 
 * @author Sebastian Krieter
 * @author Marcus Pinnecke (Feature Interface)
 */
@SuppressWarnings("unused")
public class PrintExtendedSignaturesJob extends AProjectJob<PrintExtendedSignaturesJob.Arguments> {
	
	public static class Arguments extends JobArguments {
		private final String foldername;
		
		public Arguments(String foldername) {
			super(Arguments.class);
			this.foldername = foldername;
		}
	}
	
	protected PrintExtendedSignaturesJob(Arguments arguments) {
		super(BUILD_EXTENDED_MODULES, arguments);
	}

	@Override
	protected boolean work() {
		InterfaceProject interfaceProject = MPLPlugin.getDefault().getInterfaceProject(this.project);
		if (interfaceProject == null) {
			MPLPlugin.getDefault().logWarning(this.project.getName() + " is no Interface Project!");
			return false;
		}
		IFolder folder = this.project.getFolder(arguments.foldername);
		IOConstants.clearFolder(folder);
		
		LinkedList<String> allConcreteFeatures = new LinkedList<String>();
		for (IFeature feature : FeatureUtils.extractConcreteFeatures(interfaceProject.getFeatureModel())) {
			if (!feature.getStructure().isHidden()) {
				allConcreteFeatures.add(feature.getName());
			}
		}
		workMonitor.setMaxAbsoluteWork(allConcreteFeatures.size() + 1);
		
		StringBuilder sb = new StringBuilder();
		sb.append("_No_Constraints_\n");

		ProjectSignatures p = interfaceProject.getProjectSignatures();
		SignatureIterator it = p.iterator();
		
		ContextFilter filter = new ContextFilter(new Node[]{}, p);
		it.addFilter(filter);
		
		ProjectStructure ps = new ProjectStructure(it);
		
//		sb.append(writeExtendedModule(ps, "_No_Constraints_", arguments.foldername));
		workMonitor.worked();
		
		for (String featureName : allConcreteFeatures) {
			sb.append("\n");
			sb.append(featureName);
			sb.append("\n\n");
			filter.init(featureName);
			ps = new ProjectStructure(it);
//			sb.append(writeExtendedModule(ps, featureName, foldername));
			workMonitor.worked();
		}
		
		IOConstants.writeToFile(folder.getFile("all_statistics.txt"), sb.toString());
		
		MPLPlugin.getDefault().logInfo(BUILT_EXTENDED_MODULES);
		return true;
	}
	
//	private String writeExtendedModule(ProjectStructure signature, String featureName, String folderName) {
//		for (AbstractClassFragment curClass : signature.getClasses()) {
//			AbstractClassSignature classSig = curClass.getSignature();
//			final String packagename = classSig.getPackage();
//			final String path = folderName + "/" + featureName + (packagename.isEmpty() ? "" :"/" + packagename);
//			
//			IFolder folder = CorePlugin.createFolder(project, path);
//			
//			IOConstants.writeToFile(folder.getFile(classSig.getName() + IOConstants.EXTENSION_JAVA), curClass.toShortString());
//		}
//		
//		IFolder folder = CorePlugin.createFolder(project, folderName + "/" + featureName);		
//		IOConstants.writeToFile(folder.getFile("statistics.txt"), signature.getStatisticsString());
//		return signature.getStatisticsStringHeader();
//	}
}
