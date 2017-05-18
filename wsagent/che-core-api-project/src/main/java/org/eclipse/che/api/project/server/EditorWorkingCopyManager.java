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
package org.eclipse.che.api.project.server;

import com.google.common.hash.Hashing;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.jsonrpc.commons.JsonRpcException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.project.server.notification.EditorContentUpdatedEvent;
import org.eclipse.che.api.project.shared.dto.EditorChangesDto;
import org.eclipse.che.api.project.shared.dto.event.FileTrackingOperationDto;
import org.eclipse.che.api.vfs.impl.file.event.detectors.FileTrackingOperationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static org.eclipse.che.api.project.shared.Constants.CHE_DIR;

/**
 * @author Roman Nikitenko
 */
@Singleton
public class EditorWorkingCopyManager {
    private static final Logger LOG                = LoggerFactory.getLogger(EditorWorkingCopyManager.class);
    private static final String WORKING_COPIES_DIR = "/" + CHE_DIR + "/workingCopies";

    private Provider<ProjectManager>                    projectManagerProvider;
    private EventService                                eventService;
    private EventSubscriber<FileTrackingOperationEvent> fileOperationEventSubscriber;

    private final Map<String, EditorWorkingCopy> workingCopiesStorage = new ConcurrentHashMap<>();

    @Inject
    public EditorWorkingCopyManager(Provider<ProjectManager> projectManagerProvider,
                                    EventService eventService) {
        this.projectManagerProvider = projectManagerProvider;
        this.eventService = eventService;

        fileOperationEventSubscriber = new EventSubscriber<FileTrackingOperationEvent>() {
            @Override
            public void onEvent(FileTrackingOperationEvent event) {
                onFileOperation(event.getEndpointId(), event.getFileTrackingOperation());
            }
        };
        eventService.subscribe(fileOperationEventSubscriber);
    }

    public File getWorkingCopyFile(String filePath) throws NotFoundException, ServerException, ForbiddenException {
        VirtualFileEntry persistentWorkingCopy = null;
        if (workingCopiesStorage.containsKey(filePath)) {
            EditorWorkingCopy workingCopy = workingCopiesStorage.get(filePath);
            byte[] workingCopyContent = workingCopy.getContentAsBytes();

            persistentWorkingCopy = getPersistentWorkingCopy(filePath, workingCopy.getProjectPath());
            persistentWorkingCopy.getVirtualFile().updateContent(workingCopyContent);
        } else {
            FileEntry originalFile = projectManagerProvider.get().asFile(filePath);
            if (originalFile != null) {
                persistentWorkingCopy = getPersistentWorkingCopy(filePath, originalFile.getProject());
            }
        }
        return persistentWorkingCopy == null ? null : new File(persistentWorkingCopy.getVirtualFile().toIoFile().getAbsolutePath());
    }

    public String getContentFor(String filePath) throws NotFoundException, ServerException, ConflictException, ForbiddenException {
        EditorWorkingCopy workingCopy = workingCopiesStorage.get(filePath);
        if (workingCopy != null) {
            return workingCopy.getContentAsString();
        }

        FileEntry originalFile = projectManagerProvider.get().asFile(filePath);
        if (originalFile == null) {
            throw new NotFoundException(format("Item '%s' isn't found. ", filePath));
        }
        return originalFile.getVirtualFile().getContentAsString();
    }

    void onEditorContentUpdated(String endpointId, EditorChangesDto changes)
            throws IOException, ForbiddenException, ConflictException, NotFoundException, ServerException {

        String filePath = changes.getFileLocation();
        String projectPath = changes.getProjectPath();

        String text = changes.getText();
        int offset = changes.getOffset();
        int removedCharCount = changes.getRemovedCharCount();

        EditorWorkingCopy workingCopy = workingCopiesStorage.get(filePath);
        if (workingCopy == null) {
            workingCopy = createWorkingCopy(filePath);
        }

        String newContent = null;
        String oldContent = workingCopy.getContentAsString();
        System.out.println("*********** EditorWorkingCopyManager onWorkingCopyChanged " + changes.getType());
        switch (changes.getType()) {
            case INSERT: {
                newContent = new StringBuilder(oldContent).insert(offset, text).toString();
                break;
            }
            case REMOVE: {
                if (removedCharCount > 0) {
                    newContent = new StringBuilder(oldContent).delete(offset, offset + removedCharCount).toString();
                }
                break;
            }
            case REPLACE_ALL: {
                newContent = new StringBuilder(oldContent).replace(0, oldContent.length(), text).toString();
                break;
            }
            default: {
                break;
            }

        }

        if (newContent != null) {
            System.out.println("*********** EditorWorkingCopyManager onWorkingCopyChanged update workingCopy " + filePath);
            workingCopy.updateContent(newContent);
            eventService.publish(new EditorContentUpdatedEvent(endpointId, changes));

            //TODO only for testing
            VirtualFileEntry persistentWorkingCopy = getPersistentWorkingCopy(filePath, projectPath);
            persistentWorkingCopy.getVirtualFile().updateContent(newContent);
        }
    }


    private void onFileOperation(String endpointId, FileTrackingOperationDto operation) {
        try {
            FileTrackingOperationDto.Type type = operation.getType();
            System.out.println("*********** EditorWorkingCopyManager onFileOperation " + type);
            switch (type) {
                case START: {
                    String path = operation.getPath();
                    EditorWorkingCopy workingCopy = workingCopiesStorage.get(path);
                    if (workingCopy == null) {
                        createWorkingCopy(path);
                    }
                    break;
                }
                case STOP: {
                    String path = operation.getPath();
                    EditorWorkingCopy workingCopy = workingCopiesStorage.get(path);
                    if (workingCopy == null) {
                        return;
                    }

                    if (isWorkingCopyHasUnsavedData(path)) {
                        updatePersistentWorkingCopy(path);//to have ability to recover unsaved data when the file will be open later
                        return;
                    } else {
                        VirtualFileEntry persistentWorkingCopy = getPersistentWorkingCopy(path, workingCopy.getProjectPath());
                        if (persistentWorkingCopy != null) {
                            persistentWorkingCopy.remove();
                        }
                    }
                    workingCopiesStorage.remove(path);
                    break;
                }

                case MOVE: {
                    String oldPath = operation.getOldPath();
                    String newPath = operation.getPath();

                    EditorWorkingCopy workingCopy = workingCopiesStorage.remove(oldPath);
                    if (workingCopy == null) {
                        return;
                    }

                    String workingCopyNewPath = toWorkingCopyPath(newPath);
                    workingCopy.setPath(workingCopyNewPath);
                    workingCopiesStorage.put(newPath, workingCopy);

                    String projectPath = workingCopy.getProjectPath();
                    VirtualFileEntry persistentWorkingCopy = getPersistentWorkingCopy(oldPath, projectPath);
                    if (persistentWorkingCopy != null) {
                        persistentWorkingCopy.remove();
                    }

                    FolderEntry persistentWorkingCopiesStorage = getPersistentWorkingCopiesStorage(projectPath);
                    if (persistentWorkingCopiesStorage == null) {
                        persistentWorkingCopiesStorage = createWorkingCopiesStorage(projectPath);
                    }

                    persistentWorkingCopiesStorage.createFile(workingCopyNewPath, workingCopy.getContentAsBytes());
                    break;
                }

                default: {
                    break;
                }
            }
        } catch (ServerException | IOException | ForbiddenException | ConflictException e) {
            String errorMessage = "Can not handle file operation: " + e.getMessage();

            LOG.error(errorMessage);

            throw new JsonRpcException(500, errorMessage);
        } catch (NotFoundException e) {
            String errorMessage = "Can not handle file operation: " + e.getMessage();

            LOG.error(errorMessage);

            throw new JsonRpcException(400, errorMessage);
        }
    }

    private void updatePersistentWorkingCopy(String originalFilePath) throws ServerException, ForbiddenException, ConflictException {
        EditorWorkingCopy workingCopy = workingCopiesStorage.get(originalFilePath);
        if (workingCopy == null) {
            return;
        }

        byte[] content = workingCopy.getContentAsBytes();
        String projectPath = workingCopy.getProjectPath();

        VirtualFileEntry persistentWorkingCopy = getPersistentWorkingCopy(originalFilePath, projectPath);
        if (persistentWorkingCopy != null) {
            persistentWorkingCopy.getVirtualFile().updateContent(content);
            return;
        }

        FolderEntry persistentWorkingCopiesStorage = getPersistentWorkingCopiesStorage(projectPath);
        if (persistentWorkingCopiesStorage == null) {
            persistentWorkingCopiesStorage = createWorkingCopiesStorage(projectPath);
        }

        persistentWorkingCopiesStorage.createFile(workingCopy.getPath(), content);
    }

    private boolean isWorkingCopyHasUnsavedData(String originalFilePath) {
        try {
            EditorWorkingCopy workingCopy = workingCopiesStorage.get(originalFilePath);
            if (workingCopy == null) {
                return false;
            }

            FileEntry originalFile = projectManagerProvider.get().asFile(originalFilePath);
            if (originalFile == null) {
                return false;
            }

            String workingCopyContent = workingCopy.getContentAsString();
            String originalFileContent = originalFile.getVirtualFile().getContentAsString();
            if (workingCopyContent == null || originalFileContent == null) {
                return false;
            }

            String workingCopyHash = Hashing.md5().hashString(workingCopyContent, defaultCharset()).toString();
            String originalFileHash = Hashing.md5().hashString(originalFileContent, defaultCharset()).toString();

            return !Objects.equals(workingCopyHash, originalFileHash);
        } catch (NotFoundException | ServerException | ForbiddenException e) {
            LOG.error(e.getLocalizedMessage());
        }

        return false;
    }

    private EditorWorkingCopy createWorkingCopy(String filePath)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException, IOException {

        FileEntry file = projectManagerProvider.get().asFile(filePath);
        if (file == null) {
            throw new NotFoundException(format("Item '%s' isn't found. ", filePath));
        }

        String projectPath = file.getProject();
        FolderEntry persistentWorkingCopiesStorage = getPersistentWorkingCopiesStorage(projectPath);
        if (persistentWorkingCopiesStorage == null) {
            persistentWorkingCopiesStorage = createWorkingCopiesStorage(projectPath);
        }

        String workingCopyPath = toWorkingCopyPath(filePath);
        byte[] content = file.contentAsBytes();

        VirtualFileEntry persistentWorkingCopy = persistentWorkingCopiesStorage.getChild(workingCopyPath);
        if (persistentWorkingCopy == null) {
            persistentWorkingCopiesStorage.createFile(workingCopyPath, content);
        } else {
            //TODO
            //At opening file we can have persistent working copy ONLY when user has unsaved data
            // at this case we need provide ability to recover unsaved data, so update content is temporary solution
            persistentWorkingCopy.getVirtualFile().updateContent(content);
        }

        EditorWorkingCopy workingCopy = new EditorWorkingCopy(workingCopyPath, projectPath, file.contentAsBytes());
        workingCopiesStorage.put(filePath, workingCopy);

        return workingCopy;
    }

    private VirtualFileEntry getPersistentWorkingCopy(String originalFilePath, String projectPath) {
        try {
            FolderEntry persistentWorkingCopiesStorage = getPersistentWorkingCopiesStorage(projectPath);
            if (persistentWorkingCopiesStorage == null) {
                return null;
            }

            String workingCopyPath = toWorkingCopyPath(originalFilePath);
            return persistentWorkingCopiesStorage.getChild(workingCopyPath);
        } catch (ServerException e) {
            return null;
        }
    }

    private FolderEntry getPersistentWorkingCopiesStorage(String projectPath) {
        try {
            RegisteredProject project = projectManagerProvider.get().getProject(projectPath);
            FolderEntry baseFolder = project.getBaseFolder();
            if (baseFolder == null) {
                return null;
            }

            String tempDirectoryPath = baseFolder.getPath().toString() + WORKING_COPIES_DIR;
            return projectManagerProvider.get().asFolder(tempDirectoryPath);
        } catch (Exception e) {
            return null;
        }
    }

    private FolderEntry createWorkingCopiesStorage(String projectPath) {
        try {
            RegisteredProject project = projectManagerProvider.get().getProject(projectPath);
            FolderEntry baseFolder = project.getBaseFolder();
            if (baseFolder == null) {
                return null;
            }

            return baseFolder.createFolder(WORKING_COPIES_DIR);
        } catch (Exception e) {
            return null;
        }
    }

    private String toWorkingCopyPath(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        return path.replace('/', '.');
    }

    @PreDestroy
    private void unsubscribe() {
        eventService.unsubscribe(fileOperationEventSubscriber);
    }
}
