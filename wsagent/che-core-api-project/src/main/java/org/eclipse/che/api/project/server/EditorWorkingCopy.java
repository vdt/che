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

/**
 * @author Roman Nikitenko
 */
public class EditorWorkingCopy {

    private String path;
    private String projectPath;
    private byte[]            content;

    public EditorWorkingCopy(String path, String projectPath, byte[] content) {
        this.path = path;
        this.projectPath = projectPath;
        this.content = content;
    }

    public byte[] getContentAsBytes() {
        return content;
    }

    public String getContentAsString() {
        return new String(content);
    }

    public EditorWorkingCopy updateContent(byte[] content) {
        this.content = content;
        return this;
    }

    public EditorWorkingCopy updateContent(String content) {
        this.content = content.getBytes();
        return this;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getProjectPath() {
        return projectPath;
    }
}
