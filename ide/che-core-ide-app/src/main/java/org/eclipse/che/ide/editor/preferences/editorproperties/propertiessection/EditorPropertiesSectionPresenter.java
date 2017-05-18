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
package org.eclipse.che.ide.editor.preferences.editorproperties.propertiessection;

import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.eclipse.che.ide.editor.preferences.EditorPreferenceSection;
import org.eclipse.che.ide.editor.preferences.EditorPreferencesManager;

import java.util.List;
import java.util.Map;

/**
 * Presenter for the editor properties section in the 'Preferences' menu.
 *
 * @author Roman Nikitenko
 */
public class EditorPropertiesSectionPresenter implements EditorPreferenceSection, EditorPropertiesSectionView.ActionDelegate {
    /** The preference page presenter. */
    private       EditorPreferenceSection.ParentPresenter parentPresenter;
    private final EditorPropertiesSectionView             view;
    private final EditorPreferencesManager                editorPreferencesManager;
    private final List<String>                            properties;

    @AssistedInject
    public EditorPropertiesSectionPresenter(@Assisted String title,
                                            @Assisted List<String> properties,
                                            final EditorPropertiesSectionView view,
                                            final EditorPreferencesManager editorPreferencesManager) {
        this.view = view;
        this.view.setSectionTitle(title);
        this.view.setDelegate(this);
        this.properties = properties;
        this.editorPreferencesManager = editorPreferencesManager;
    }

    @Override
    public void storeChanges() {
        Map<String, JSONValue> editorPreferences = editorPreferencesManager.getEditorPreferences();
        editorPreferences.keySet()
                         .forEach(property -> {
                             JSONValue actualValue = view.getPropertyValueById(property);
                             actualValue = actualValue != null ? actualValue : editorPreferences.get(property);
                             editorPreferences.put(property, actualValue);
                         });

        editorPreferencesManager.storeEditorPreferences(editorPreferences);
    }

    @Override
    public void refresh() {
        addProperties();
    }

    @Override
    public boolean isDirty() {
        Map<String, JSONValue> editorPreferences = editorPreferencesManager.getEditorPreferences();
        return editorPreferences.keySet()
                                .stream()
                                .anyMatch(property -> {
                                    JSONValue actualValue = view.getPropertyValueById(property);
                                    return actualValue != null && !actualValue.equals(editorPreferences.get(property));
                                });
    }

    @Override
    public void go(final AcceptsOneWidget container) {
        addProperties();
        container.setWidget(view);
    }

    @Override
    public void setParent(final ParentPresenter parent) {
        this.parentPresenter = parent;
    }

    @Override
    public void onPropertyChanged() {
        parentPresenter.signalDirtyState();
    }

    private void addProperties() {
        Map<String, JSONValue> editorPreferences = editorPreferencesManager.getEditorPreferences();

        properties.forEach(property -> {
            JSONValue value = editorPreferences.get(property);
            view.addProperty(property, value);
        });
    }
}
