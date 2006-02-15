/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.omnetpp.ned.editor.graph;

import java.io.*;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.draw2d.*;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.gef.*;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.*;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.gef.ui.stackview.CommandStackInspectorPage;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.omnetpp.ned.editor.graph.actions.IncrementDecrementAction;
import org.omnetpp.ned.editor.graph.actions.ModulePasteTemplateAction;
import org.omnetpp.ned.editor.graph.dnd.ModuleTemplateTransferDropTargetListener;
import org.omnetpp.ned.editor.graph.dnd.TextTransferDropTargetListener;
import org.omnetpp.ned.editor.graph.edit.NedEditPartFactory;
import org.omnetpp.ned.editor.graph.edit.TreePartFactory;
import org.omnetpp.ned.editor.graph.figures.properties.LayerSupport;
import org.omnetpp.ned.editor.graph.misc.*;
import org.omnetpp.ned.editor.graph.misc.MessageFactory;
import org.omnetpp.ned.editor.graph.misc.ModulePaletteCustomizer;
import org.omnetpp.ned.editor.graph.misc.ModuleRulerProvider;
import org.omnetpp.ned.editor.graph.model.Container;
import org.omnetpp.ned.editor.graph.model.Ruler;

import sun.security.action.GetLongAction;

public class ModuleEditor extends GraphicalEditorWithFlyoutPalette {
    
    
    class OutlinePage extends ContentOutlinePage implements IAdaptable {

        private PageBook pageBook;

        private Control outline;

        private Canvas overview;

        private IAction showOutlineAction, showOverviewAction;

        static final int ID_OUTLINE = 0;

        static final int ID_OVERVIEW = 1;

        private Thumbnail thumbnail;

        private DisposeListener disposeListener;

        public OutlinePage(EditPartViewer viewer) {
            super(viewer);
        }

        public void init(IPageSite pageSite) {
            super.init(pageSite);
            ActionRegistry registry = getActionRegistry();
            IActionBars bars = pageSite.getActionBars();
            String id = ActionFactory.UNDO.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.REDO.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.DELETE.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = IncrementDecrementAction.INCREMENT;
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = IncrementDecrementAction.DECREMENT;
            bars.setGlobalActionHandler(id, registry.getAction(id));
            bars.updateActionBars();
        }

        protected void configureOutlineViewer() {
            getViewer().setEditDomain(getEditDomain());
            getViewer().setEditPartFactory(new TreePartFactory());
            ContextMenuProvider provider = new ModuleEditorContextMenuProvider(getViewer(), getActionRegistry());
            getViewer().setContextMenu(provider);
            getSite().registerContextMenu("org.eclipse.gef.examples.logic.outline.contextmenu", //$NON-NLS-1$
                    provider, getSite().getSelectionProvider());
            getViewer().setKeyHandler(getCommonKeyHandler());
            getViewer().addDropTargetListener(new ModuleTemplateTransferDropTargetListener(getViewer()));
            IToolBarManager tbm = getSite().getActionBars().getToolBarManager();
            showOutlineAction = new Action() {
                public void run() {
                    showPage(ID_OUTLINE);
                }
            };
            showOutlineAction.setImageDescriptor(ImageFactory.getDescriptor(ImageFactory.TOOLBAR_IMAGE_OUTLINE));
            tbm.add(showOutlineAction);
            showOverviewAction = new Action() {
                public void run() {
                    showPage(ID_OVERVIEW);
                }
            };
            showOverviewAction.setImageDescriptor(ImageFactory.getDescriptor(ImageFactory.TOOLBAR_IMAGE_OVERVIEW));
            tbm.add(showOverviewAction);
            showPage(ID_OUTLINE);
        }

        public void createControl(Composite parent) {
            pageBook = new PageBook(parent, SWT.NONE);
            outline = getViewer().createControl(pageBook);
            overview = new Canvas(pageBook, SWT.NONE);
            pageBook.showPage(outline);
            configureOutlineViewer();
            hookOutlineViewer();
            initializeOutlineViewer();
        }

        public void dispose() {
            unhookOutlineViewer();
            if (thumbnail != null) {
                thumbnail.deactivate();
                thumbnail = null;
            }
            super.dispose();
            ModuleEditor.this.outlinePage = null;
            outlinePage = null;
        }

        public Object getAdapter(Class type) {
            if (type == ZoomManager.class)
                return getGraphicalViewer().getProperty(ZoomManager.class.toString());
            return null;
        }

        public Control getControl() {
            return pageBook;
        }

        protected void hookOutlineViewer() {
            getSelectionSynchronizer().addViewer(getViewer());
        }

        protected void initializeOutlineViewer() {
            setContents(getDiagram());
        }

        protected void initializeOverview() {
            LightweightSystem lws = new LightweightSystem(overview);
            RootEditPart rep = getGraphicalViewer().getRootEditPart();
            if (rep instanceof ScalableFreeformRootEditPart) {
                ScalableFreeformRootEditPart root = (ScalableFreeformRootEditPart) rep;
                thumbnail = new ScrollableThumbnail((Viewport) root.getFigure());
                thumbnail.setBorder(new MarginBorder(3));
                thumbnail.setSource(root.getLayer(LayerConstants.PRINTABLE_LAYERS));
                lws.setContents(thumbnail);
                disposeListener = new DisposeListener() {
                    public void widgetDisposed(DisposeEvent e) {
                        if (thumbnail != null) {
                            thumbnail.deactivate();
                            thumbnail = null;
                        }
                    }
                };
                getEditor().addDisposeListener(disposeListener);
            }
        }

        public void setContents(Object contents) {
            getViewer().setContents(contents);
        }

        protected void showPage(int id) {
            if (id == ID_OUTLINE) {
                showOutlineAction.setChecked(true);
                showOverviewAction.setChecked(false);
                pageBook.showPage(outline);
                if (thumbnail != null) thumbnail.setVisible(false);
            } else if (id == ID_OVERVIEW) {
                if (thumbnail == null) initializeOverview();
                showOutlineAction.setChecked(false);
                showOverviewAction.setChecked(true);
                pageBook.showPage(overview);
                thumbnail.setVisible(true);
            }
        }

        protected void unhookOutlineViewer() {
            getSelectionSynchronizer().removeViewer(getViewer());
            if (disposeListener != null && getEditor() != null && !getEditor().isDisposed())
                getEditor().removeDisposeListener(disposeListener);
        }
    }

    private KeyHandler sharedKeyHandler;

    private PaletteRoot root;

    private OutlinePage outlinePage;

    private boolean editorSaving = false;

    // This class listens to changes to the file system in the workspace, and
    // makes changes accordingly.
    // 1) An open, saved file gets deleted -> close the editor
    // 2) An open file gets renamed or moved -> change the editor's input
    // accordingly
    class ResourceTracker implements IResourceChangeListener, IResourceDeltaVisitor {
        public void resourceChanged(IResourceChangeEvent event) {
            IResourceDelta delta = event.getDelta();
            try {
                if (delta != null) delta.accept(this);
            } catch (CoreException exception) {
                // What should be done here?
            }
        }

        public boolean visit(IResourceDelta delta) {
            if (delta == null || !delta.getResource().equals(((IFileEditorInput) getEditorInput()).getFile()))
                return true;

            if (delta.getKind() == IResourceDelta.REMOVED) {
                Display display = getSite().getShell().getDisplay();
                if ((IResourceDelta.MOVED_TO & delta.getFlags()) == 0) { // if
                    // the
                    // file
                    // was
                    // deleted
                    // NOTE: The case where an open, unsaved file is deleted is
                    // being handled by the
                    // PartListener added to the Workbench in the initialize()
                    // method.
                    display.asyncExec(new Runnable() {
                        public void run() {
                            if (!isDirty()) closeEditor(false);
                        }
                    });
                } else { // else if it was moved or renamed
                    final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
                            delta.getMovedToPath());
                    display.asyncExec(new Runnable() {
                        public void run() {
                            superSetInput(new FileEditorInput(newFile));
                        }
                    });
                }
            } else if (delta.getKind() == IResourceDelta.CHANGED) {
                if (!editorSaving) {
                    // the file was overwritten somehow (could have been
                    // replaced by another
                    // version in the respository)
                    final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
                            delta.getFullPath());
                    Display display = getSite().getShell().getDisplay();
                    display.asyncExec(new Runnable() {
                        public void run() {
                            setInput(new FileEditorInput(newFile));
                            getCommandStack().flush();
                        }
                    });
                }
            }
            return false;
        }
    }

    private IPartListener partListener = new IPartListener() {
        // If an open, unsaved file was deleted, query the user to either do a
        // "Save As"
        // or close the editor.
        public void partActivated(IWorkbenchPart part) {
            if (part != ModuleEditor.this) return;
            if (!((IFileEditorInput) getEditorInput()).getFile().exists()) {
                Shell shell = getSite().getShell();
                String title = MessageFactory.GraphicalEditor_FILE_DELETED_TITLE_UI;
                String message = MessageFactory.GraphicalEditor_FILE_DELETED_WITHOUT_SAVE_INFO;
                String[] buttons = { MessageFactory.GraphicalEditor_SAVE_BUTTON_UI,
                        MessageFactory.GraphicalEditor_CLOSE_BUTTON_UI };
                MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION,
                        buttons, 0);
                if (dialog.open() == 0) {
                    if (!performSaveAs()) partActivated(part);
                } else {
                    closeEditor(false);
                }
            }
        }

        public void partBroughtToTop(IWorkbenchPart part) {
        }

        public void partClosed(IWorkbenchPart part) {
        }

        public void partDeactivated(IWorkbenchPart part) {
        }

        public void partOpened(IWorkbenchPart part) {
        }
    };

    private Container logicDiagram = new Container();

    private boolean savePreviouslyNeeded = false;

    private ResourceTracker resourceListener = new ResourceTracker();

    private RulerComposite rulerComp;

    protected static final String PALETTE_DOCK_LOCATION = "Dock location"; //$NON-NLS-1$

    protected static final String PALETTE_SIZE = "Palette Size"; //$NON-NLS-1$

    protected static final String PALETTE_STATE = "Palette state"; //$NON-NLS-1$

    protected static final int DEFAULT_PALETTE_SIZE = 130;

    static {
    	System.out.println("Editor loaded");
    }

    public ModuleEditor() {
    	System.out.println("Editor started");
        ModuleEditorPlugin.getDefault().getPreferenceStore().setDefault(PALETTE_SIZE, DEFAULT_PALETTE_SIZE);
        setEditDomain(new DefaultEditDomain(this));
    }

    protected void closeEditor(boolean save) {
        getSite().getPage().closeEditor(ModuleEditor.this, save);
    }

    public void commandStackChanged(EventObject event) {
        if (isDirty()) {
            if (!savePreviouslyNeeded()) {
                setSavePreviouslyNeeded(true);
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }
        } else {
            setSavePreviouslyNeeded(false);
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
        super.commandStackChanged(event);
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        ScrollingGraphicalViewer viewer = (ScrollingGraphicalViewer) getGraphicalViewer();

        ScalableFreeformRootEditPart root = new ScalableFreeformRootEditPart();

        List zoomLevels = new ArrayList(3);
        zoomLevels.add(ZoomManager.FIT_ALL);
        zoomLevels.add(ZoomManager.FIT_WIDTH);
        zoomLevels.add(ZoomManager.FIT_HEIGHT);
        root.getZoomManager().setZoomLevelContributions(zoomLevels);

        IAction zoomIn = new ZoomInAction(root.getZoomManager());
        IAction zoomOut = new ZoomOutAction(root.getZoomManager());
        getActionRegistry().registerAction(zoomIn);
        getActionRegistry().registerAction(zoomOut);
        getSite().getKeyBindingService().registerAction(zoomIn);
        getSite().getKeyBindingService().registerAction(zoomOut);
        
        // create decoration layers and add them before and after the primary layer
        // they are used to hold figure decorations like side texts and range indicators
        LayeredPane printableLayers = (LayeredPane)root.getLayer(LayerConstants.PRINTABLE_LAYERS);
        Layer backDecorationLayer = new FreeformLayer();
        backDecorationLayer.setLayoutManager(new DelegatingLayout());
        printableLayers.addLayerBefore(backDecorationLayer, LayerSupport.BACK_DECORATION_LAYER, LayerConstants.PRIMARY_LAYER);
        Layer frontDecorationLayer = new FreeformLayer();
        frontDecorationLayer.setLayoutManager(new DelegatingLayout());
        printableLayers.addLayerAfter(frontDecorationLayer, LayerSupport.FRONT_DECORATION_LAYER, LayerConstants.PRIMARY_LAYER);

        // set the root edit part as the main viewer
        viewer.setRootEditPart(root);

        viewer.setEditPartFactory(new NedEditPartFactory());
        ContextMenuProvider provider = new ModuleEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
        getSite().registerContextMenu("org.omnetpp.ned.editor.graph.contextmenu", //$NON-NLS-1$
                provider, viewer);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer).setParent(getCommonKeyHandler()));

        loadProperties();

        // Actions
        IAction showRulers = new ToggleRulerVisibilityAction(getGraphicalViewer());
        getActionRegistry().registerAction(showRulers);

        IAction snapAction = new ToggleSnapToGeometryAction(getGraphicalViewer());
        getActionRegistry().registerAction(snapAction);

        IAction showGrid = new ToggleGridAction(getGraphicalViewer());
        getActionRegistry().registerAction(showGrid);

        Listener listener = new Listener() {
            public void handleEvent(Event event) {
                handleActivationChanged(event);
            }
        };
        getGraphicalControl().addListener(SWT.Activate, listener);
        getGraphicalControl().addListener(SWT.Deactivate, listener);
    }

    protected void createOutputStream(OutputStream os) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(os);
        out.writeObject(getDiagram());
        out.close();
    }

    protected CustomPalettePage createPalettePage() {
        return new CustomPalettePage(getPaletteViewerProvider()) {
            public void init(IPageSite pageSite) {
                super.init(pageSite);
                IAction copy = getActionRegistry().getAction(ActionFactory.COPY.getId());
                pageSite.getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copy);
            }
        };
    }

    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {
            private IMenuListener menuListener;

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.setCustomizer(new ModulePaletteCustomizer());
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }

            protected void hookPaletteViewer(PaletteViewer viewer) {
                super.hookPaletteViewer(viewer);
                final CopyTemplateAction copy = (CopyTemplateAction) getActionRegistry().getAction(
                        ActionFactory.COPY.getId());
                viewer.addSelectionChangedListener(copy);
                if (menuListener == null) menuListener = new IMenuListener() {
                    public void menuAboutToShow(IMenuManager manager) {
                        manager.appendToGroup(GEFActionConstants.GROUP_COPY, copy);
                    }
                };
                viewer.getContextMenu().addMenuListener(menuListener);
            }
        };
    }

    public void dispose() {
        getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
        partListener = null;
        ((IFileEditorInput) getEditorInput()).getFile().getWorkspace().removeResourceChangeListener(
                resourceListener);
        super.dispose();
    }

    public void doSave(IProgressMonitor progressMonitor) {
        try {
            editorSaving = true;
            saveProperties();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            createOutputStream(out);
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();
            file.setContents(new ByteArrayInputStream(out.toByteArray()), true, false, progressMonitor);
            out.close();
            getCommandStack().markSaveLocation();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            editorSaving = false;
        }
    }

    public void doSaveAs() {
        performSaveAs();
    }

    public Object getAdapter(Class type) {
        if (type == CommandStackInspectorPage.class) return new CommandStackInspectorPage(getCommandStack());
        if (type == IContentOutlinePage.class) {
            outlinePage = new OutlinePage(new TreeViewer());
            return outlinePage;
        }
        if (type == ZoomManager.class) return getGraphicalViewer().getProperty(ZoomManager.class.toString());

        return super.getAdapter(type);
    }

    protected Control getGraphicalControl() {
        return rulerComp;
    }

    /**
     * Returns the KeyHandler with common bindings for both the Outline and
     * Graphical Views. For example, delete is a common action.
     */
    protected KeyHandler getCommonKeyHandler() {
        if (sharedKeyHandler == null) {
            sharedKeyHandler = new KeyHandler();
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(
                    GEFActionConstants.DIRECT_EDIT));
        }
        return sharedKeyHandler;
    }

    protected Container getDiagram() {
        return logicDiagram;
    }

    protected FlyoutPreferences getPalettePreferences() {
        return new FlyoutPreferences() {
            public int getDockLocation() {
                return ModuleEditorPlugin.getDefault().getPreferenceStore().getInt(PALETTE_DOCK_LOCATION);
            }

            public int getPaletteState() {
                return ModuleEditorPlugin.getDefault().getPreferenceStore().getInt(PALETTE_STATE);
            }

            public int getPaletteWidth() {
                return ModuleEditorPlugin.getDefault().getPreferenceStore().getInt(PALETTE_SIZE);
            }

            public void setDockLocation(int location) {
                ModuleEditorPlugin.getDefault().getPreferenceStore().setValue(PALETTE_DOCK_LOCATION, location);
            }

            public void setPaletteState(int state) {
                ModuleEditorPlugin.getDefault().getPreferenceStore().setValue(PALETTE_STATE, state);
            }

            public void setPaletteWidth(int width) {
                ModuleEditorPlugin.getDefault().getPreferenceStore().setValue(PALETTE_SIZE, width);
            }
        };
    }

    protected PaletteRoot getPaletteRoot() {
        if (root == null) {
            root = ModuleEditorPlugin.createPalette();
        }
        return root;
    }

    public void gotoMarker(IMarker marker) {
    }

    protected void handleActivationChanged(Event event) {
        IAction copy = null;
        if (event.type == SWT.Deactivate) copy = getActionRegistry().getAction(ActionFactory.COPY.getId());
        if (getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()) != copy) {
            getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copy);
            getEditorSite().getActionBars().updateActionBars();
        }
    }

    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        getGraphicalViewer().setContents(getDiagram());

        getGraphicalViewer().addDropTargetListener(
                new ModuleTemplateTransferDropTargetListener(getGraphicalViewer()));
        getGraphicalViewer().addDropTargetListener(
                new TextTransferDropTargetListener(getGraphicalViewer(), TextTransfer.getInstance()));
    }

    protected void createActions() {
        super.createActions();
        ActionRegistry registry = getActionRegistry();
        IAction action;

        action = new CopyTemplateAction(this);
        registry.registerAction(action);

        action = new MatchWidthAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new MatchHeightAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new ModulePasteTemplateAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new IncrementDecrementAction(this, true);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new IncrementDecrementAction(this, false);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new DirectEditAction((IWorkbenchPart) this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.LEFT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.RIGHT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.TOP);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.BOTTOM);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.CENTER);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());

        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.MIDDLE);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#createGraphicalViewer(org.eclipse.swt.widgets.Composite)
     */
    protected void createGraphicalViewer(Composite parent) {
        rulerComp = new RulerComposite(parent, SWT.NONE);
        super.createGraphicalViewer(rulerComp);
        rulerComp.setGraphicalViewer((ScrollingGraphicalViewer) getGraphicalViewer());
    }

    protected FigureCanvas getEditor() {
        return (FigureCanvas) getGraphicalViewer().getControl();
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    protected void loadProperties() {
        // Ruler properties
        Ruler ruler = getDiagram().getRuler(PositionConstants.WEST);
        RulerProvider provider = null;
        if (ruler != null) {
            provider = new ModuleRulerProvider(ruler);
        }
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_VERTICAL_RULER, provider);
        ruler = getDiagram().getRuler(PositionConstants.NORTH);
        provider = null;
        if (ruler != null) {
            provider = new ModuleRulerProvider(ruler);
        }
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER, provider);
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY,
                new Boolean(getDiagram().getRulerVisibility()));

        // Snap to Geometry property
        getGraphicalViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED,
                new Boolean(getDiagram().isSnapToGeometryEnabled()));

        // Grid properties
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED,
                new Boolean(getDiagram().isGridEnabled()));
        // We keep grid visibility and enablement in sync
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE,
                new Boolean(getDiagram().isGridEnabled()));

        // Zoom
        ZoomManager manager = (ZoomManager) getGraphicalViewer().getProperty(ZoomManager.class.toString());
        if (manager != null) manager.setZoom(getDiagram().getZoom());
        // Scroll-wheel Zoom
        getGraphicalViewer().setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.CTRL),
                MouseWheelZoomHandler.SINGLETON);

    }

    protected boolean performSaveAs() {
        SaveAsDialog dialog = new SaveAsDialog(getSite().getWorkbenchWindow().getShell());
        dialog.setOriginalFile(((IFileEditorInput) getEditorInput()).getFile());
        dialog.open();
        IPath path = dialog.getResult();

        if (path == null) return false;

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IFile file = workspace.getRoot().getFile(path);

        if (!file.exists()) {
            WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
                public void execute(final IProgressMonitor monitor) {
                    saveProperties();
                    try {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        createOutputStream(out);
                        file.create(new ByteArrayInputStream(out.toByteArray()), true, monitor);
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            try {
                new ProgressMonitorDialog(getSite().getWorkbenchWindow().getShell()).run(false, true, op);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            superSetInput(new FileEditorInput(file));
            getCommandStack().markSaveLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean savePreviouslyNeeded() {
        return savePreviouslyNeeded;
    }

    protected void saveProperties() {
        getDiagram().setRulerVisibility(
                ((Boolean) getGraphicalViewer().getProperty(RulerProvider.PROPERTY_RULER_VISIBILITY))
                        .booleanValue());
        getDiagram()
                .setGridEnabled(
                        ((Boolean) getGraphicalViewer().getProperty(SnapToGrid.PROPERTY_GRID_ENABLED))
                                .booleanValue());
        getDiagram().setSnapToGeometry(
                ((Boolean) getGraphicalViewer().getProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED))
                        .booleanValue());
        ZoomManager manager = (ZoomManager) getGraphicalViewer().getProperty(ZoomManager.class.toString());
        if (manager != null) getDiagram().setZoom(manager.getZoom());
    }

    public void setInput(IEditorInput input) {
        superSetInput(input);

        IFile file = ((IFileEditorInput) input).getFile();
        try {
            InputStream is = file.getContents(false);
            ObjectInputStream ois = new ObjectInputStream(is);
            setLogicDiagram((Container) ois.readObject());
            ois.close();
        } catch (Exception e) {
            // This is just an example. All exceptions caught here.
            e.printStackTrace();
        }

        if (!editorSaving) {
            if (getGraphicalViewer() != null) {
                getGraphicalViewer().setContents(getDiagram());
                loadProperties();
            }
            if (outlinePage != null) {
                outlinePage.setContents(getDiagram());
            }
        }
    }

    public void setLogicDiagram(Container diagram) {
        logicDiagram = diagram;
    }

    private void setSavePreviouslyNeeded(boolean value) {
        savePreviouslyNeeded = value;
    }

    protected void superSetInput(IEditorInput input) {
        // The workspace never changes for an editor. So, removing and re-adding
        // the
        // resourceListener is not necessary. But it is being done here for the
        // sake
        // of proper implementation. Plus, the resourceListener needs to be
        // added
        // to the workspace the first time around.
        if (getEditorInput() != null) {
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();
            file.getWorkspace().removeResourceChangeListener(resourceListener);
        }

        super.setInput(input);

        if (getEditorInput() != null) {
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();
            file.getWorkspace().addResourceChangeListener(resourceListener);
            setPartName(file.getName());
        }
    }

    protected void setSite(IWorkbenchPartSite site) {
        super.setSite(site);
        getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
    }

}