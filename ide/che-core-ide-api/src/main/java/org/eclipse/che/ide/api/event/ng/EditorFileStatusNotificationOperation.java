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
package org.eclipse.che.ide.api.event.ng;


import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.jsonrpc.commons.RequestHandlerConfigurator;
import org.eclipse.che.api.project.shared.dto.event.FileStateUpdateDto;
import org.eclipse.che.api.project.shared.dto.event.FileWatcherEventType;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.FileContentUpdateEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.resources.ExternalResourceDelta;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.util.loging.Log;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;

import java.util.function.BiConsumer;

import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.EMERGE_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;
import static org.eclipse.che.ide.api.resources.ResourceDelta.REMOVED;

/**
 * Receives file status notifications from sever VFS file watchers for registered files.
 * The list of registered files contains files opened in an editor. Notifications can be
 * of only two types: file modified and file deleted. Each kind of notification invokes
 * specified behaviour.
 *
 * @author Dmitry Kuleshov
 */
@Singleton
public class EditorFileStatusNotificationOperation implements BiConsumer<String, FileStateUpdateDto> {

    private final EventBus               eventBus;
    private final DeletedFilesController deletedFilesController;
    private final Provider<EditorAgent>  editorAgentProvider;
    private final AppContext             appContext;

    private NotificationManager notificationManager;

    @Inject
    public EditorFileStatusNotificationOperation(EventBus eventBus,
                                                 DeletedFilesController deletedFilesController,
                                                 Provider<EditorAgent> editorAgentProvider,
                                                 AppContext appContext) {
        this.eventBus = eventBus;
        this.deletedFilesController = deletedFilesController;
        this.editorAgentProvider = editorAgentProvider;
        this.appContext = appContext;
    }

    @Inject
    public void configureHandler(RequestHandlerConfigurator configurator) {
        configurator.newConfiguration()
                    .methodName("event:file-state-changed")
                    .paramsAsDto(FileStateUpdateDto.class)
                    .noResult()
                    .withConsumer(this);
    }

    public void inject(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public void accept(String endpointId, FileStateUpdateDto params) {
        final FileWatcherEventType status = params.getType();
        final String stringPath = params.getPath();
        final String name = stringPath.substring(stringPath.lastIndexOf("/") + 1);

        switch (status) {
            case MODIFIED: {
                Log.debug(getClass(), "Received updated file event status: " + stringPath);

                Log.error(getClass(), "MODIFIED new FileContentUpdateEvent(stringPath, params.getHashCode())");
                eventBus.fireEvent(new FileContentUpdateEvent(stringPath, params.getHashCode()));

                break;
            }
            case DELETED: {
                Log.debug(getClass(), "Received removed file event status: " + stringPath);

                final Path path = Path.valueOf(stringPath);
                appContext.getWorkspaceRoot().synchronize(new ExternalResourceDelta(path, path, REMOVED));
                if (notificationManager != null && !deletedFilesController.remove(stringPath)) {
                    notificationManager.notify("External operation", "File '" + name + "' is removed", SUCCESS, EMERGE_MODE);
                    closeOpenedEditor(path);
                }

                break;
            }
        }
    }

    private void closeOpenedEditor(Path path) {
        final EditorAgent editorAgent = editorAgentProvider.get();
        final List<EditorPartPresenter> openedEditors = editorAgent.getOpenedEditors();
        for (EditorPartPresenter openEditor : openedEditors) {
            if (openEditor.getEditorInput().getFile().getLocation().equals(path)) {
                editorAgent.closeEditor(openEditor);
            }
        }
    }
}
