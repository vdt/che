/*
 * Copyright (c) 2015-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 */
'use strict';

/**
 * Defines a directive for creating a container for list of tags.
 * @author Oleksii Kurinnyi
 */
export class CheChipsList {
  restrict: string = 'E';
  transclude: boolean = true;
  require: string = 'ngModel';

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor() { }

  template() {
    return `<md-chips class="che-chips-list">
      <md-chip-template >
        <div class="tag-text">{{$chip}}</div>
        <div class="tag-btn">
          <i class="fa fa-close"></i>
        </div>
      </md-chip-template>
    </md-chips>`;
  }

  compile(element, attrs) {
    let keys = Object.keys(attrs);

    // search the md-chips element
    let mdChipsElement = element.find('md-chips');

    keys.forEach((key: string) => {

      // don't reapply internal properties
      if (key.indexOf('$') === 0) {
        return;
      }
      // don't reapply internal element properties
      if (key.indexOf('che') === 0) {
        return;
      }
      let value = attrs[key];

      // handle empty values as boolean
      if (value === '') {
        value = 'true';
      }

      // set the value of the attribute
      mdChipsElement.attr(attrs.$attr[key], value);

      // it needs to use DOM element method to set custom value of boolean attribute
      // because jQuery's 'attr' method substitutes this value with name of attribute
      if (key === 'readonly') {
        mdChipsElement[0].setAttribute(key, value);
      }

      element.removeAttr(attrs.$attr[key]);

    });
  }

}
