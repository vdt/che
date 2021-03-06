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
export interface INamespace {
  id: string;
  label: string;
  location: string;
}

/**
 * Registry for maintaining system namespaces.
 *
 * @author Ann Shumilova
 */
export class CheNamespaceRegistry {
  private $q: ng.IQService;
  private fetchPromise: ng.IPromise<any>;
  private namespaces : INamespace[];
  private emptyMessage: string;
  private caption: string;
  private getAdditionalInfoFunction: Function;

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($q: ng.IQService) {
    this.$q = $q;
    this.namespaces = [];

    this.caption = 'Namespace';
  }

  /**
   * Store promise that resolves after namespaces are added.
   *
   * @param {ng.IPromise<any>} fetchPromise
   */
  setFetchPromise(fetchPromise: ng.IPromise<any>): void {
    this.fetchPromise = fetchPromise;
  }

  /**
   * Returns promise.
   *
   * @return {ng.IPromise<any>}
   */
  fetchNamespaces(): ng.IPromise<any> {
    if (!this.fetchPromise) {
      let defer = this.$q.defer();
      defer.resolve();
      return defer.promise;
    }

    return this.fetchPromise;
  }

  /**
   * Adds the list of namespaces.
   *
   * @param {INamespace[]} namespaces namespace to be added
   */
  addNamespaces(namespaces : INamespace[]) : void {
    this.namespaces = this.namespaces.concat(namespaces);
  }

  /**
   * Returns the list of available namespaces.
   *
   * @returns {INamespace[]} namespaces
   */
  getNamespaces() : INamespace[] {
    return this.namespaces;
  }

  /**
   * Set empty message (message is displayed, when no namespaces).
   *
   * @param message empty message
   */
  setEmptyMessage(message: string): void {
    this.emptyMessage = message;
  }

  /**
   * Returns empty message to display, when no namespaces.
   *
   * @returns {string}
   */
  getEmptyMessage(): string {
    return this.emptyMessage ? this.emptyMessage : null;
  }

  /**
   * Set display caption of the namespaces.
   *
   * @param caption namespaces caption
   */
  setCaption(caption: string): void {
    this.caption = caption;
  }

  /**
   * Returns the caption of the namespaces.
   *
   * @returns {string} namepsaces caption
   */
  getCaption(): string {
    return this.caption;
  }

  /**
   * Sets the function for retrieving available RAM for the namespaces.
   *
   * @param getAdditionalInfo additional information function
   */
  setGetAdditionalInfo(getAdditionalInfo: Function): void {
    this.getAdditionalInfoFunction = getAdditionalInfo;
  }

  /**
   * Returns function, that returns promise.
   *
   * @returns {Function}
   */
  getAdditionalInfo(): Function {
    return this.getAdditionalInfoFunction;
  }
}
