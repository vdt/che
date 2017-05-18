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
package org.eclipse.che.plugin.languageserver.ide.editor;

import org.eclipse.che.api.languageserver.shared.model.ExtendedInitializeResult;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.editor.AsyncEditorProvider;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.EditorProvider;
import org.eclipse.che.ide.api.editor.defaulteditor.AbstractTextEditorProvider;
import org.eclipse.che.ide.api.editor.defaulteditor.EditorBuilder;
import org.eclipse.che.ide.api.editor.editorconfig.AutoSaveTextEditorConfiguration;
import org.eclipse.che.ide.api.editor.editorconfig.DefaultTextEditorConfiguration;
import org.eclipse.che.ide.api.editor.editorconfig.TextEditorConfiguration;
import org.eclipse.che.ide.api.editor.texteditor.TextEditor;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.api.resources.VirtualFile;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;
import org.eclipse.che.ide.ui.loaders.request.MessageLoader;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.plugin.languageserver.ide.registry.LanguageServerRegistry;

import javax.inject.Inject;

/**
 * Provide editor with LS support
 */
public class LanguageServerEditorProvider implements AsyncEditorProvider, EditorProvider {

    private final LanguageServerRegistry                   registry;
    private final LoaderFactory                            loaderFactory;
    private       LanguageServerEditorConfigurationFactory editorConfigurationFactory;
    @com.google.inject.Inject
    private EditorBuilder editorBuilder;

    @Inject
    public LanguageServerEditorProvider(LanguageServerEditorConfigurationFactory editorConfigurationFactory,
                                        LanguageServerRegistry registry,
                                        LoaderFactory loaderFactory) {
        this.editorConfigurationFactory = editorConfigurationFactory;
        this.registry = registry;
        this.loaderFactory = loaderFactory;
    }

    @Override
    public String getId() {
        return "LanguageServerEditor";
    }

    @Override
    public String getDescription() {
        return "Code Editor";
    }

    @Override
    public TextEditor getEditor() {
        if (editorBuilder == null) {
            Log.debug(AbstractTextEditorProvider.class, "No builder registered for default editor type - giving up.");
            return null;
        }

        final TextEditor editor = editorBuilder.buildEditor();
        editor.initialize(new DefaultTextEditorConfiguration());
        return editor;

    }

    @Override
    public Promise<EditorPartPresenter> createEditor(VirtualFile file) {
        if (file instanceof File) {
            File resource = (File)file;

            Promise<ExtendedInitializeResult> promise =
                    registry.getOrInitializeServer(resource.getRelatedProject().get().getPath(), resource.getExtension(),
                                                   resource.getLocation().toString());
            final MessageLoader loader = loaderFactory.newLoader("Initializing Language Server for " + resource.getExtension());
            loader.show();
            return promise.thenPromise(new Function<ExtendedInitializeResult, Promise<EditorPartPresenter>>() {
                @Override
                public Promise<EditorPartPresenter> apply(ExtendedInitializeResult arg) throws FunctionException {
                    loader.hide();
                    if (editorBuilder == null) {
                        Log.debug(AbstractTextEditorProvider.class, "No builder registered for default editor type - giving up.");
                        return Promises.<EditorPartPresenter>resolve(null);
                    }

                    final TextEditor editor = editorBuilder.buildEditor();
                    LanguageServerEditorConfiguration configuration = editorConfigurationFactory.build(editor, arg.getCapabilities());
                    editor.initialize(configuration);
                    return Promises.<EditorPartPresenter>resolve(editor);
                }
            });

        }
        return null;
    }
}
