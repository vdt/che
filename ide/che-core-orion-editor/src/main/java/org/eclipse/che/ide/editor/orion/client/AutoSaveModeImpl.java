/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.editor.orion.client;

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import org.eclipse.che.api.core.jsonrpc.commons.RequestTransmitter;
import org.eclipse.che.api.project.shared.dto.EditorChangesDto;
import org.eclipse.che.api.project.shared.dto.EditorChangesDto.Type;
import org.eclipse.che.ide.api.editor.EditorOpenedEvent;
import org.eclipse.che.ide.api.editor.EditorOpenedEventHandler;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.document.DocumentHandle;
import org.eclipse.che.ide.api.editor.events.DocumentChangeEvent;
import org.eclipse.che.ide.api.editor.reconciler.DirtyRegion;
import org.eclipse.che.ide.api.editor.reconciler.DirtyRegionQueue;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.event.ActivePartChangedHandler;
import org.eclipse.che.ide.api.event.EditorSettingsChangedEvent;
import org.eclipse.che.ide.api.event.EditorSettingsChangedEvent.EditorSettingsChangedHandler;
import org.eclipse.che.ide.api.event.FileContentUpdatedEvent;
import org.eclipse.che.ide.api.event.FileContentUpdatedEvent.FileContentUpdatedHandler;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.api.resources.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.editor.preferences.EditorPreferencesManager;
import org.eclipse.che.ide.util.loging.Log;

import java.util.HashSet;

import static org.eclipse.che.api.project.shared.dto.EditorChangesDto.Type.INSERT;
import static org.eclipse.che.api.project.shared.dto.EditorChangesDto.Type.REMOVE;
import static org.eclipse.che.api.project.shared.dto.EditorChangesDto.Type.REPLACE_ALL;
import static org.eclipse.che.ide.editor.orion.client.AutoSaveMode.Mode.ACTIVATED;
import static org.eclipse.che.ide.editor.orion.client.AutoSaveMode.Mode.DEACTIVATED;
import static org.eclipse.che.ide.editor.orion.client.AutoSaveMode.Mode.SUSPENDED;
import static org.eclipse.che.ide.editor.preferences.editorproperties.EditorProperties.ENABLE_AUTO_SAVE;

/**
 * Default implementation of {@link AutoSaveMode} which provides autosave function.
 *
 * @author Roman Nikitenko
 */
public class AutoSaveModeImpl implements AutoSaveMode, EditorSettingsChangedHandler, ActivePartChangedHandler, EditorOpenedEventHandler,
                                         FileContentUpdatedHandler {

    private static final int    DELAY            = 1000;
    private static final String ENDPOINT_ID      = "ws-agent";
    private static final String OUTCOMING_METHOD = "track:editor-content-changes";

    private EventBus                 eventBus;
    private EditorPreferencesManager editorPreferencesManager;
    private DtoFactory               dtoFactory;
    private RequestTransmitter       requestTransmitter;
    private DocumentHandle           documentHandle;
    private TextEditor               editor;
    private EditorPartPresenter      activeEditor;
    private DirtyRegionQueue         dirtyRegionQueue;
    private Mode                     mode;

    private HashSet<HandlerRegistration> handlerRegistrations = new HashSet<>(6);

    private final Timer saveTimer = new Timer() {
        @Override
        public void run() {
            save();
        }
    };

    @Inject
    public AutoSaveModeImpl(EventBus eventBus,
                            EditorPreferencesManager editorPreferencesManager,
                            DtoFactory dtoFactory,
                            RequestTransmitter requestTransmitter) {
        this.eventBus = eventBus;
        this.editorPreferencesManager = editorPreferencesManager;
        this.dtoFactory = dtoFactory;
        this.requestTransmitter = requestTransmitter;

        mode = ACTIVATED; //autosave is activated by default

        addHandlers();
    }

    @Override
    public DocumentHandle getDocumentHandle() {
        return documentHandle;
    }

    @Override
    public void setDocumentHandle(DocumentHandle documentHandle) {
        this.documentHandle = documentHandle;
    }

    @Override
    public void install(TextEditor editor) {
        this.editor = editor;
        this.dirtyRegionQueue = new DirtyRegionQueue();
        updateAutoSaveState();
    }

    @Override
    public void uninstall() {
        saveTimer.cancel();
        handlerRegistrations.forEach(HandlerRegistration::removeHandler);
    }

    @Override
    public void suspend() {
        Log.error(getClass(), "///// suspend " + editor.getDocument().getFile().getLocation());
        mode = SUSPENDED;
        saveTimer.cancel();
    }

    @Override
    public void resume() {
        Log.error(getClass(), "///// resume " + editor.getDocument().getFile().getLocation());
        updateAutoSaveState();
    }

    @Override
    public boolean isActivated() {
        return mode == ACTIVATED;
    }

    @Override
    public void onEditorSettingsChanged(EditorSettingsChangedEvent event) {
        updateAutoSaveState();
    }

    private void updateAutoSaveState() {
        Boolean autoSaveValue = editorPreferencesManager.getBooleanValueFor(ENABLE_AUTO_SAVE);
        if (autoSaveValue == null) {
            return;
        }

        if (DEACTIVATED != mode && !autoSaveValue) {
            mode = DEACTIVATED;
            saveTimer.cancel();
        } else if (ACTIVATED != mode && autoSaveValue) {
            mode = ACTIVATED;

            saveTimer.cancel();
            saveTimer.schedule(DELAY);
        }
    }

    @Override
    public void onActivePartChanged(ActivePartChangedEvent event) {
        PartPresenter activePart = event.getActivePart();
        if (activePart instanceof EditorPartPresenter) {
            activeEditor = (EditorPartPresenter)activePart;
        }
    }

    @Override
    public void onEditorOpened(EditorOpenedEvent editorOpenedEvent) {
        if (documentHandle != null && editor == editorOpenedEvent.getEditor()) {
            HandlerRegistration documentChangeHandlerRegistration =
                    documentHandle.getDocEventBus().addHandler(DocumentChangeEvent.TYPE, this);
            handlerRegistrations.add(documentChangeHandlerRegistration);
        }
    }

    @Override
    public void onDocumentChange(final DocumentChangeEvent event) {
        Log.error(getClass(), "//////////////////////////////////////////////////////////////// onDocumentChange " + mode + " /// " +
                              editor.getEditorInput().getFile().getLocation());
//        Log.error(getClass(), "//////////////////////////////////////////////////////////////// onDocumentChange " + event.getText());
        Log.error(getClass(), "onDocumentChange  " + documentHandle.hashCode() + " /// " + event.getDocument().hashCode());

        if (documentHandle == null || !event.getDocument().isSameAs(documentHandle)) {
            return;
        }

        createDirtyRegion(event);

        saveTimer.cancel();
        saveTimer.schedule(DELAY);

    }

    /**
     * Creates a dirty region for a document event and adds it to the queue.
     *
     * @param event
     *         the document event for which to create a dirty region
     */
    private void createDirtyRegion(final DocumentChangeEvent event) {
//        Log.error(getClass(), "====///////////////////////////  createDirtyRegion " +
//                              event.getDocument().getDocument().getFile().getLocation().toString());
        if (event.getRemoveCharCount() == 0 && event.getText() != null && !event.getText().isEmpty()) {
            // Insert
            dirtyRegionQueue.addDirtyRegion(new DirtyRegion(event.getOffset(),
                                                            event.getLength(),
                                                            DirtyRegion.INSERT,
                                                            event.getText()));

        } else if (event.getText() == null || event.getText().isEmpty()) {
            // Remove
            dirtyRegionQueue.addDirtyRegion(new DirtyRegion(event.getOffset(),
                                                            event.getRemoveCharCount(),
                                                            DirtyRegion.REMOVE,
                                                            null));

        } else {
            // Replace (Remove + Insert)
            dirtyRegionQueue.addDirtyRegion(new DirtyRegion(event.getOffset(),
                                                            event.getRemoveCharCount(),
                                                            DirtyRegion.REMOVE,
                                                            null));
            dirtyRegionQueue.addDirtyRegion(new DirtyRegion(event.getOffset(),
                                                            event.getLength(),
                                                            DirtyRegion.INSERT,
                                                            event.getText()));
        }
    }

    private void save() {
        Log.error(getClass(), "=============================================================== save " +
                              editor.getEditorInput().getFile().getLocation() + " /// " + mode);

        if (SUSPENDED == mode && editor != activeEditor) {
            return;
        }

        if (ACTIVATED == mode && editor.isDirty()) {
            editor.doSave();
        }

        VirtualFile file = editor.getEditorInput().getFile();
        Project project = getProject(file);
        if (project == null) {
            return;
        }

        String filePath = file.getLocation().toString();
        String projectPath = project.getPath();

        while (dirtyRegionQueue.getSize() > 0) {
            DirtyRegion region = dirtyRegionQueue.removeNextDirtyRegion();
            transmit(filePath, projectPath, region);
        }
    }

    private void transmit(String filePath, String projectPath, DirtyRegion dirtyRegion) {
        Type type = dirtyRegion.getType().equals(DirtyRegion.INSERT) ? INSERT : REMOVE;
        EditorChangesDto changes = dtoFactory.createDto(EditorChangesDto.class)
                                             .withType(type)
                                             .withProjectPath(projectPath)
                                             .withFileLocation(filePath)
                                             .withOffset(dirtyRegion.getOffset())
                                             .withText(dirtyRegion.getText());

        int length = dirtyRegion.getLength();
        if (DirtyRegion.REMOVE.equals(dirtyRegion.getType())) {
            changes.withRemovedCharCount(length);
        } else {
            changes.withLength(length);
        }

        Log.error(getClass(), "====///////////////////////////  before transmit dirty region " + filePath);
        requestTransmitter.newRequest()
                          .endpointId(ENDPOINT_ID)
                          .methodName(OUTCOMING_METHOD)
                          .paramsAsDto(changes)
                          .sendAndSkipResult();
    }

    @Override
    public void onFileContentUpdated(FileContentUpdatedEvent event) {
        Document document = documentHandle.getDocument();
        VirtualFile file = document.getFile();
        if (!event.getFilePath().equals(file.getLocation().toString())) {
            return;
        }

        Project project = getProject(file);
        if (project == null) {
            return;
        }

        String newContent = document.getContents();
        String projectPath = project.getPath();
        EditorChangesDto changes = dtoFactory.createDto(EditorChangesDto.class)
                                             .withType(REPLACE_ALL)
                                             .withProjectPath(projectPath)
                                             .withFileLocation(file.getLocation().toString())
                                             .withText(newContent);
        Log.error(getClass(), "====///////////////////////////  before transmit REPLACE_ALL " + file.getLocation().toString());
        requestTransmitter.newRequest()
                          .endpointId(ENDPOINT_ID)
                          .methodName(OUTCOMING_METHOD)
                          .paramsAsDto(changes)
                          .sendAndSkipResult();
    }

    private Project getProject(VirtualFile file) {
        if (file == null || !(file instanceof Resource)) {
            return null;
        }

        Project project = ((Resource)file).getProject();
        return (project != null && project.exists()) ? project : null;
    }

    private void addHandlers() {
        HandlerRegistration activePartChangedHandlerRegistration = eventBus.addHandler(ActivePartChangedEvent.TYPE, this);
        handlerRegistrations.add(activePartChangedHandlerRegistration);

        HandlerRegistration editorSettingsChangedHandlerRegistration = eventBus.addHandler(EditorSettingsChangedEvent.TYPE, this);
        handlerRegistrations.add(editorSettingsChangedHandlerRegistration);

        HandlerRegistration fileContentUpdatedHandlerRegistration = eventBus.addHandler(FileContentUpdatedEvent.TYPE, this);
        handlerRegistrations.add(fileContentUpdatedHandlerRegistration);

        HandlerRegistration editorOpenedHandlerRegistration = eventBus.addHandler(EditorOpenedEvent.TYPE, this);
        handlerRegistrations.add(editorOpenedHandlerRegistration);
    }
}
