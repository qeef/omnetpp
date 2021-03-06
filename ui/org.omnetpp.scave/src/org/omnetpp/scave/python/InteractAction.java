package org.omnetpp.scave.python;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.scave.actions.AbstractScaveAction;
import org.omnetpp.scave.editors.ChartScriptEditor;
import org.omnetpp.scave.editors.ScaveEditor;

public class InteractAction extends AbstractScaveAction {

    public InteractAction() {
        super(IAction.AS_RADIO_BUTTON);

        setText("Interact Tool");
        setDescription("Only lets you interact with the chart, if the chart supports it");
        setImageDescriptor(ImageFactory.global().getDescriptor(ImageFactory.TOOLBAR_IMAGE_POINTER));
    }

    @Override
    protected void doRun(ScaveEditor scaveEditor, IStructuredSelection selection) {
        if (isChecked()) {
            ChartScriptEditor editor = scaveEditor.getActiveChartScriptEditor();
            if (editor != null)
                editor.getMatplotlibChartViewer().interact();
        }
    }

    @Override
    protected boolean isApplicable(ScaveEditor scaveEditor, IStructuredSelection selection) {
        ChartScriptEditor editor = scaveEditor.getActiveChartScriptEditor();
        return editor != null && editor.isPythonProcessAlive();
    }
}