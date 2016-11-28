/*
 * Copyright (c) 2016 Google Inc. All Right Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.cloud.tools.endpoints.framework;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ProjectTests {

  @BeforeClass
  public static void setUp() throws VerificationException {
    Verifier verifier = new Verifier(".", true);
    verifier.addCliOption("-DskipTests");
    verifier.executeGoal("install");
  }

  @Test
  public void testServerArtifactCreation() throws IOException, VerificationException {
    File testDir = ResourceExtractor.simpleExtractResources(ProjectTests.class, "/projects/server");

    Verifier verifier = new Verifier(testDir.getAbsolutePath());
    verifier.executeGoals(
        Arrays.asList("endpoints-framework:clientLibs", "endpoints-framework:discoveryDocs"));
    verifier.verifyErrorFreeLog();
    verifier.assertFilePresent("target/client-libs/testApi-v1-java.zip");
    verifier.assertFilePresent("target/discovery-docs/testApi-v1-rest.discovery");
  }

  @Test
  public void testClientGeneratedSourceCreation() throws IOException, VerificationException {
    File testDir = ResourceExtractor.simpleExtractResources(ProjectTests.class, "/projects/client");

    Verifier verifier = new Verifier(testDir.getAbsolutePath());
    verifier.executeGoals(Arrays.asList("compile"));
    verifier.verifyErrorFreeLog();
    verifier.assertFilePresent("target/generated-sources/endpoints/com/example/testApi/TestApi.java");
  }
}
