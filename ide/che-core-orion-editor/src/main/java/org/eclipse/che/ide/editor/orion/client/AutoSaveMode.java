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

import org.eclipse.che.ide.api.editor.document.UseDocumentHandle;
import org.eclipse.che.ide.api.editor.events.DocumentChangeHandler;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.editor.preferences.editorproperties.EditorProperties;

/**
 * Auto save functionality.
 *
 * @author Roman Nikitenko
 */
public interface AutoSaveMode extends DocumentChangeHandler, UseDocumentHandle {

    /**
     * Installs auto save mode on the given editor.
     */
    void install(TextEditor editor);

    /**
     * Removes auto save mode from editor.
     */
    void uninstall();

    /**
     * Suspends auto save mode for editor content.
     */
    void suspend();

    /**
     * Resumes auto save mode for editor content.
     * Note: If option {@link EditorProperties#ENABLE_AUTO_SAVE} in editor preferences is disabled - do nothing.
     */
    void resume();

    /**
     * Return true if auto save mode is activated, false otherwise.
     */
    boolean isActivated();

    enum Mode {
        ACTIVATED,
        SUSPENDED,
        DEACTIVATED
    }
}
