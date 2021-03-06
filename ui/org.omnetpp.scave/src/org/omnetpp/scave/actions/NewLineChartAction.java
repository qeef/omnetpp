/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.actions;

import org.omnetpp.scave.ScaveImages;
import org.omnetpp.scave.ScavePlugin;
import org.omnetpp.scave.model.ScaveModelFactory;


/**
 * Create a Line Chart.
 */
public class NewLineChartAction extends NewAnalysisItemAction {
    public NewLineChartAction(boolean withEditDialog) {
        super(ScaveModelFactory.eINSTANCE.createLineChart(), withEditDialog);
        setText("New Line Chart");
        setImageDescriptor(ScavePlugin.getImageDescriptor(ScaveImages.IMG_ETOOL16_NEWLINECHART));
    }
}
