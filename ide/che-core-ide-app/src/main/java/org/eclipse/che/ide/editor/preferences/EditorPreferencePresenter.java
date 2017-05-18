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
package org.eclipse.che.ide.editor.preferences;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.preferences.AbstractPreferencePagePresenter;

import java.util.Set;

/** Preference page presenter for the editors. */
@Singleton
public class EditorPreferencePresenter extends AbstractPreferencePagePresenter implements EditorPreferenceSection.ParentPresenter {

    /** The editor preferences page view. */
    private final EditorPreferenceView view;
    private final Set<EditorPreferenceSection> editorPreferenceSections;

    @Inject
    public EditorPreferencePresenter(final EditorPreferenceView view,
                                     final EditorPrefLocalizationConstant constant,
                                     final Set<EditorPreferenceSection> editorPreferenceSections) {
        super(constant.editorTypeTitle(), constant.editorTypeCategory());

        this.view = view;
        this.editorPreferenceSections = editorPreferenceSections;

        editorPreferenceSections.forEach(section -> section.setParent(this));
    }

    @Override
    public boolean isDirty() {
        return editorPreferenceSections.stream().anyMatch(EditorPreferenceSection::isDirty);
    }

    @Override
    public void go(final AcceptsOneWidget container) {
        AcceptsOneWidget preferencesContainer = view.getEditorPreferencesContainer();

        editorPreferenceSections.forEach(section -> section.go(preferencesContainer));

        container.setWidget(view);
    }

    @Override
    public void signalDirtyState() {
        delegate.onDirtyChanged();
    }

    @Override
    public void storeChanges() {
        editorPreferenceSections.stream()
                                .filter(EditorPreferenceSection::isDirty)
                                .forEach(EditorPreferenceSection::storeChanges);
    }

    @Override
    public void revertChanges() {
        editorPreferenceSections.stream().forEach(EditorPreferenceSection::refresh);
        signalDirtyState();
    }
}
