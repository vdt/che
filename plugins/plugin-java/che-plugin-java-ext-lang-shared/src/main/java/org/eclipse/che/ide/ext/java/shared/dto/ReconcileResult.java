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
package org.eclipse.che.ide.ext.java.shared.dto;

import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author Evgen Vidolob
 */
@DTO
public interface ReconcileResult {

    List<Problem> getProblems();

    void setProblems(List<Problem> problems);

    ReconcileResult withProblems(List<Problem> problems);

    List<HighlightedPosition> getHighlightedPositions();

    void setHighlightedPositions(List<HighlightedPosition> positions);

    ReconcileResult withHighlightedPositions(List<HighlightedPosition> positions);

    String getFileLocation();

    void setFileLocation(String path);

    ReconcileResult withFileLocation(String path);
}
