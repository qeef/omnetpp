package com.simulcraft.test.gui.recorder.object;

import java.util.List;

import org.eclipse.swt.widgets.Button;

import com.simulcraft.test.gui.recorder.GUIRecorder;
import com.simulcraft.test.gui.recorder.JavaExpr;

public class ButtonRecognizer extends ObjectRecognizer {
    
    public ButtonRecognizer(GUIRecorder recorder) {
        super(recorder);
    }

    public List<JavaExpr> identifyObject(Object uiObject) {
        if (uiObject instanceof Button) {
            Button button = (Button)uiObject;
            return chainO(button.getShell(), "findButtonWithLabel("+quote(button.getText()) + ")", 0.8);
        }
        return null;
    }
}