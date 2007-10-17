package com.simulcraft.test.gui.recorder.event;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import com.simulcraft.test.gui.recorder.GUIRecorder;
import com.simulcraft.test.gui.recorder.JavaExpr;

public class ClickRecognizer extends EventRecognizer {
    public ClickRecognizer(GUIRecorder recorder) {
        super(recorder);
    }

    public List<JavaExpr> recognizeEvent(Event e) {
        if (e.type == SWT.MouseDown)
            return chainE(e, "click()", 0.3);
        return null;
    }
}