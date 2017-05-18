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
package org.eclipse.che.ide.ext.java.client.editor;

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.jsonrpc.commons.RequestHandlerConfigurator;
import org.eclipse.che.ide.ext.java.shared.dto.ReconcileError;
import org.eclipse.che.ide.ext.java.shared.dto.ReconcileResult;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Roman Nikitenko
 */
@Singleton
public class ReconcileUpdateOperation {
    private static final String INCOMING_METHOD = "event:java-reconcile-state-changed";

    private final EventBus eventBus;

    @Inject
    public ReconcileUpdateOperation(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Inject
    public void configureHandler(RequestHandlerConfigurator configurator) {
        configurator.newConfiguration()
                    .methodName(INCOMING_METHOD)
                    .paramsAsDto(ReconcileError.class)
                    .noResult()
                    .withConsumer(this::onError);

        configurator.newConfiguration()
                    .methodName(INCOMING_METHOD)
                    .paramsAsDto(ReconcileResult.class)
                    .noResult()
                    .withConsumer(this::onSuccess);
    }

    private void onSuccess(String s, ReconcileResult reconcileResult) {
        eventBus.fireEvent(new ReconcileOperationEvent(reconcileResult));
    }

    public void onError(String endpointId, ReconcileError reconcileError) {
        eventBus.fireEvent(new ReconcileOperationEvent(reconcileError));
    }
}
