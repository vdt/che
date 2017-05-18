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
package org.eclipse.che.plugin.maven.client.editor;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.editor.EditorWithErrors;
import org.eclipse.che.ide.api.editor.annotation.AnnotationModel;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.reconciler.DirtyRegion;
import org.eclipse.che.ide.api.editor.reconciler.ReconcilingStrategy;
import org.eclipse.che.ide.api.editor.text.Region;
import org.eclipse.che.ide.ext.java.client.editor.ProblemRequester;
import org.eclipse.che.ide.ext.java.client.editor.ReconcileOperationEvent;
import org.eclipse.che.ide.ext.java.client.editor.ReconcileOperationEvent.ReconcileOperationHandler;
import org.eclipse.che.ide.ext.java.shared.dto.Problem;
import org.eclipse.che.ide.ext.java.shared.dto.ReconcileError;
import org.eclipse.che.ide.ext.java.shared.dto.ReconcileResult;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.plugin.maven.client.service.MavenServerServiceClient;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Reconciling strategy for 'pom.xml'
 *
 * @author Evgen Vidolob
 */
public class PomReconcilingStrategy implements ReconcilingStrategy, ReconcileOperationHandler {

    private final AnnotationModel          annotationModel;
    private final EditorWithErrors         editor;
    private final MavenServerServiceClient client;

    private String pomPath;
    private HandlerRegistration reconcilerOperationHandlerRegistration;

    @Inject
    public PomReconcilingStrategy(@Assisted AnnotationModel annotationModel,
                                  @Assisted @NotNull final EditorWithErrors editor,
                                  MavenServerServiceClient client,
                                  EventBus eventBus) {
        this.annotationModel = annotationModel;
        this.editor = editor;
        this.client = client;
        Log.error(getClass(), "----------------------------------------- PomReconcilingStrategy ");
        reconcilerOperationHandlerRegistration = eventBus.addHandler(ReconcileOperationEvent.TYPE, this);
    }

    @Override
    public void setDocument(Document document) {
        pomPath = document.getFile().getLocation().toString();
    }


    @Override
    public void reconcile(DirtyRegion dirtyRegion, Region subRegion) {
        doReconcile();
    }

    @Override
    public void reconcile(Region partition) {
        doReconcile();
    }

    public void doReconcile() {
        Log.error(getClass(), "----------------------------------------- PomReconcilingStrategy sent request ");
        Promise<List<Problem>> promise = client.reconcilePom(pomPath);
        promise.then(new Operation<List<Problem>>() {
            @Override
            public void apply(List<Problem> arg) throws OperationException {
                Log.error(getClass(), "----------------------------------------- PomReconcilingStrategy sent request success ");
                doReconcile(arg);
            }
        });
    }

    private void doReconcile(final List<Problem> problems) {

        if (this.annotationModel == null) {
            return;
        }
        ProblemRequester problemRequester;
        if (this.annotationModel instanceof ProblemRequester) {
            problemRequester = (ProblemRequester)this.annotationModel;
            problemRequester.beginReporting();
        } else {
            editor.setErrorState(EditorWithErrors.EditorState.NONE);
            return;
        }
        try {
            boolean error = false;
            boolean warning = false;
            for (Problem problem : problems) {

                if (!error) {
                    error = problem.isError();
                }
                if (!warning) {
                    warning = problem.isWarning();
                }
                problemRequester.acceptProblem(problem);
            }
            if (error) {
                editor.setErrorState(EditorWithErrors.EditorState.ERROR);
            } else if (warning) {
                editor.setErrorState(EditorWithErrors.EditorState.WARNING);
            } else {
                editor.setErrorState(EditorWithErrors.EditorState.NONE);
            }
        } catch (final Exception e) {
            Log.error(getClass(), e);
        } finally {
            problemRequester.endReporting();
        }
    }

    @Override
    public void closeReconciler() {
        reconcilerOperationHandlerRegistration.removeHandler();
    }

    @Override
    public void onReconcileSuccess(ReconcileResult reconcileResult) {
        if (!pomPath.equals(reconcileResult.getFileLocation())) {
            Log.error(getClass(), "========= return " + reconcileResult.getFileLocation());
            return;
        }

        if (reconcileResult.getProblems().isEmpty()) {
            Log.error(getClass(), "========= is Empty ");
        } else {
            Log.error(getClass(), "========= not Empty " + reconcileResult.getProblems().get(0).getMessage());
        }
        doReconcile(reconcileResult.getProblems());
    }

    @Override
    public void onReconcileFailure(ReconcileError error) {

    }
}
