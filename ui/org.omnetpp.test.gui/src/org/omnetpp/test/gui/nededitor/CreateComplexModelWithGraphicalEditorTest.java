package org.omnetpp.test.gui.nededitor;

import org.omnetpp.test.gui.access.CompoundModuleEditPartAccess;
import org.omnetpp.test.gui.access.GraphicalNedEditorAccess;

import com.simulcraft.test.gui.access.MultiPageEditorPartAccess;
import com.simulcraft.test.gui.access.TextEditorAccess;

public class CreateComplexModelWithGraphicalEditorTest
	extends NedFileTestCase
{
	public void testCreateSimpleModel() throws Throwable {
		createNewNedFileByWizard();
		MultiPageEditorPartAccess multiPageEditorPart = findMultiPageEditor();
		GraphicalNedEditorAccess graphicalNedEditor = (GraphicalNedEditorAccess)multiPageEditorPart.ensureActiveEditor("Graphical");
		graphicalNedEditor.createSimpleModuleWithPalette("TestNode");
		TextEditorAccess textualEditor = (TextEditorAccess)multiPageEditorPart.activatePageEditor("Text");
		textualEditor.moveCursorAfter("simple TestNode.*\\n\\{");
		textualEditor.pressEnter();
		textualEditor.typeIn("gates:");
		textualEditor.pressEnter();
		textualEditor.typeIn("inout g;");
		textualEditor.pressEnter();
		multiPageEditorPart.activatePageEditor("Graphical");
		CompoundModuleEditPartAccess compoundModuleEditPart = graphicalNedEditor.createCompoundModuleWithPalette("TestNetwork");
		compoundModuleEditPart.createSubModuleWithPalette("TestNode", "node1", 200, 200);
		compoundModuleEditPart.createSubModuleWithPalette("TestNode", "node2", 100, 100);
		compoundModuleEditPart.createConnectionWithPalette("node1", "node2", ".*g.*");
		multiPageEditorPart.saveWithHotKey();
	}
}
