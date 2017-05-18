/*
 * Copyright (c) 2015-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 */
'use strict';

// components
import {ApiConfig} from './api/che-api-config';
import {AttributeConfig} from './attribute/attribute-config';
import {FilterConfig} from './filter/filter-config';
import {CheBrandingConfig} from './branding/che-branding-config';
import {CodeMirrorConstant} from './codemirror/codemirror';
import {GitHubService} from './github/github-service';
import {CheIdeFetcherConfig} from './ide-fetcher/che-ide-fetcher-config';
import {CheUIElementsInjectorConfig} from './injector/che-ui-elements-injector-config';
import {CheNotificationConfig} from './notification/che-notification-config';
import {RoutingConfig} from './routing/routing-config';
import {ValidatorConfig} from './validator/validator-config';
import {WidgetConfig} from './widget/widget-config';

import {CheStepsContainer} from './steps-container/steps-container.directive';
import {CheErrorMessagesConfig} from './error-messages/che-error-messages-config';
import {ServiceConfig} from './service/service-config';

export class ComponentsConfig {

  constructor(register: che.IRegisterService) {
    new ApiConfig(register);
    new AttributeConfig(register);
    new FilterConfig(register);
    new CheBrandingConfig(register);
    new CodeMirrorConstant(register);
    new GitHubService(register);
    new CheIdeFetcherConfig(register);
    new CheUIElementsInjectorConfig(register);
    new CheNotificationConfig(register);
    new RoutingConfig(register);
    new ValidatorConfig(register);
    new WidgetConfig(register);
    new CheErrorMessagesConfig(register);
    new ServiceConfig(register);

    register.directive('cheStepsContainer', CheStepsContainer);
  }
}
