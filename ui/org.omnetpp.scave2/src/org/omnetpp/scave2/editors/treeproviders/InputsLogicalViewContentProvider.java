package org.omnetpp.scave2.editors.treeproviders;

import static org.omnetpp.scave2.model.RunAttribute.EXPERIMENT;
import static org.omnetpp.scave2.model.RunAttribute.MEASUREMENT;
import static org.omnetpp.scave2.model.RunAttribute.REPLICATION;
import static org.omnetpp.scave2.model.RunAttribute.getRunAttribute;
import org.omnetpp.scave.engine.ResultFile;
import org.omnetpp.scave.engine.Run;
import org.omnetpp.scave.engine.RunList;
import org.omnetpp.scave.engineext.ResultFileManagerEx;

/**
 * Content provider for the "Logical view" tree of the Inputs page.
 */
public class InputsLogicalViewContentProvider extends CachedTreeContentProvider {

	public GenericTreeNode buildTree(Object element) {
		// ResultFileManager/Experiment/Measurement/Replication
		ResultFileManagerEx manager = (ResultFileManagerEx)element;
		GenericTreeNode root = new GenericTreeNode(manager);
		for (ResultFile file : manager.getFiles().toArray()) {
			RunList runlist = manager.getRunsInFile(file);
			for (int j = 0; j < runlist.size(); ++j) {
				Run run = runlist.get(j);
				GenericTreeNode experimentNode = root.getOrCreateChild(getRunAttribute(run, EXPERIMENT));
				GenericTreeNode measurementNode = experimentNode.getOrCreateChild(getRunAttribute(run, MEASUREMENT));
				measurementNode.getOrCreateChild(getRunAttribute(run, REPLICATION));
			}
		}
		return root;
	}
}
