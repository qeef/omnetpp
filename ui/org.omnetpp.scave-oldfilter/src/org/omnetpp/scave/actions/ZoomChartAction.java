package org.omnetpp.scave.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.scave.charting.ChartCanvas;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.editors.ui.ChartPage2;
import org.omnetpp.scave.editors.ui.ScaveEditorPage;

import static org.omnetpp.common.image.ImageFactory.*;

/**
 * Zooms in/out the chart of the active chart page in the active Scave editor.
 *
 * @author tomi
 */
public class ZoomChartAction extends AbstractScaveAction {
	
	private double zoomFactor;
	private boolean horizontally;

	public ZoomChartAction(boolean horizontally, double zoomFactor) {
		this.horizontally = horizontally;
		this.zoomFactor = zoomFactor;

		String dir = (zoomFactor > 1.0 ? "in" : "out");
		setText("Zoom " +  dir + (horizontally ? " X" : " Y"));
		setDescription("Zoom " + dir + " chart " + (horizontally ? "horizontally" : "vertically"));
		String imageId = zoomFactor > 1.0 ? TOOLBAR_IMAGE_ZOOMPLUS : TOOLBAR_IMAGE_ZOOMMINUS;
		setImageDescriptor(getDescriptor(imageId));
	}

	@Override
	protected void doRun(ScaveEditor scaveEditor, IStructuredSelection selection) {
		ScaveEditorPage page = scaveEditor.getActiveEditorPage();
		if (page != null && page instanceof ChartPage2) {
			ChartCanvas canvas = ((ChartPage2)page).getChartView();
			if (horizontally)
				canvas.zoomXBy(zoomFactor);
			else
				canvas.zoomYBy(zoomFactor);
		}
	}

	@Override
	protected boolean isApplicable(ScaveEditor editor, IStructuredSelection selection) {
		return editor.getActiveEditorPage() instanceof ChartPage2;
	}
}
