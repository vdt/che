#!/usr/bin/env bats
# Copyright (c) 2012-2017 Red Hat, Inc
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Marian Labuda - Initial Implementation

source /dockerfiles/cli/tests/test_base.sh

@test "test 'init' and 'destroy --quiet' with existing dir" {

  #GIVEN
  tmp_path="${TESTRUN_DIR}"/init-destroy1
  container_tmp_path=""${CONTAINER_TESTRUN_DIR}""/init-destroy1
  mkdir -p "${tmp_path}"

  #WHEN
  docker run --rm -v "${SCRIPTS_DIR}":/scripts/base -v /var/run/docker.sock:/var/run/docker.sock -v "${tmp_path}":/data $CLI_IMAGE init --skip:nightly --skip:pull

  #THEN
  [[ -d "${container_tmp_path}"/docs ]]
  [[ -d "${container_tmp_path}"/instance ]]
  [[ -e "${container_tmp_path}"/che.env ]]
  [[ -e "${container_tmp_path}"/cli.log ]]

  #WHEN
  docker run --rm -v "${SCRIPTS_DIR}":/scripts/base -v /var/run/docker.sock:/var/run/docker.sock -v "${tmp_path}":/data $CLI_IMAGE destroy --quiet --skip:nightly --skip:pull
  
  #THEN
  [[ ! -d "${container_tmp_path}"/docs ]]
  [[ ! -d "${container_tmp_path}"/instance ]]
  [[ ! -e "${container_tmp_path}"/che.env ]]
  [[ -e "${container_tmp_path}"/cli.log ]]
  rm -rf "${container_tmp_path}"
}

@test "test 'init' and 'destroy --quiet' with non-existing dir" {

  #GIVEN
  tmp_path="${TESTRUN_DIR}"/init-destroy2
  container_tmp_path="${CONTAINER_TESTRUN_DIR}"/init-destroy2
 
  #WHEN  
  docker run --rm -v "${SCRIPTS_DIR}":/scripts/base -v /var/run/docker.sock:/var/run/docker.sock -v "${tmp_path}":/data $CLI_IMAGE init --skip:nightly --skip:pull 1>/dev/null

  #THEN
  [[ -e "${container_tmp_path}" ]]
  [[ -d "${container_tmp_path}"/docs ]]
  [[ -d "${container_tmp_path}"/instance ]]
  [[ -e "${container_tmp_path}"/che.env ]]
  [[ -e "${container_tmp_path}"/cli.log ]]

  #WHEN
  docker run --rm -v "${SCRIPTS_DIR}":/scripts/base -v /var/run/docker.sock:/var/run/docker.sock -v "${tmp_path}":/data $CLI_IMAGE destroy --skip:nightly --skip:pull --quiet 1>/dev/null
  
  #THEN
  [[ ! -d "${container_tmp_path}"/docs ]]
  [[ ! -d "${container_tmp_path}"/instance ]]
  [[ ! -e "${container_tmp_path}"/che.env ]]
  [[ -e "${container_tmp_path}"/cli.log ]]
  rm -rf "${container_tmp_path}"

}

@test "test 'init' and 'destroy --quiet --cli' with existing dir" {
  #GIVEN
  tmp_path="${TESTRUN_DIR}"/init-destroy3
  container_tmp_path="${CONTAINER_TESTRUN_DIR}"/init-destroy3

  mkdir -p "${tmp_path}"

  #WHEN
  docker run --rm -v "${SCRIPTS_DIR}":/scripts/base -v /var/run/docker.sock:/var/run/docker.sock -v "${tmp_path}":/data $CLI_IMAGE init --skip:nightly --skip:pull 1>/dev/null
  remove_named_container $CLI_CONTAINER

  #THEN
  [[ -d "${container_tmp_path}"/docs ]]
  [[ -d "${container_tmp_path}"/instance ]]
  [[ -e "${container_tmp_path}"/che.env ]]
  [[ -e "${container_tmp_path}"/cli.log ]]

  #WHEN
  docker run --rm -v "${SCRIPTS_DIR}":/scripts/base -v /var/run/docker.sock:/var/run/docker.sock -v "${tmp_path}":/data $CLI_IMAGE destroy --skip:nightly --skip:pull --quiet --cli 1>/dev/null
  
  #THEN
  [[ ! -d "${container_tmp_path}"/docs ]]
  [[ ! -d "${container_tmp_path}"/instance ]]
  [[ ! -e "${container_tmp_path}"/che.env ]]
  [[ ! -e "${container_tmp_path}"/cli.log ]]
  rm -rf "${container_tmp_path}"

}

@test "test 'init' and 'destroy --quiet --cli' with non-existing dir" {

  #GIVEN
  tmp_path="${TESTRUN_DIR}"/init-destroy4
  container_tmp_path="${CONTAINER_TESTRUN_DIR}"/init-destroy4

  #WHEN
  docker run --rm -v "${SCRIPTS_DIR}":/scripts/base -v /var/run/docker.sock:/var/run/docker.sock -v "${tmp_path}":/data $CLI_IMAGE init --skip:nightly --skip:pull 1>/dev/null

  #THEN
  [[ -d "${container_tmp_path}" ]]
  [[ -d "${container_tmp_path}"/docs ]]
  [[ -d "${container_tmp_path}"/instance ]]
  [[ -e "${container_tmp_path}"/che.env ]]
  [[ -e "${container_tmp_path}"/cli.log ]]

  #WHEN
  docker run --rm -v "${SCRIPTS_DIR}":/scripts/base -v /var/run/docker.sock:/var/run/docker.sock -v "${tmp_path}":/data $CLI_IMAGE destroy --skip:nightly --skip:pull --quiet --cli 1>/dev/null
  
  #THEN
  [[ ! -d "${container_tmp_path}"/docs ]]
  [[ ! -d "${container_tmp_path}"/instance ]]
  [[ ! -e "${container_tmp_path}"/che.env ]]
  [[ ! -e "${container_tmp_path}"/cli.log ]]
  rm -rf "${container_tmp_path}"

}

