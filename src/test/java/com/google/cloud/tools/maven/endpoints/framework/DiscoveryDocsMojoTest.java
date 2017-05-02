/*
 * Copyright (c) 2017 Google Inc. All Right Reserved.
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

package com.google.cloud.tools.maven.endpoints.framework;


import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.rules.TemporaryFolder;

public class DiscoveryDocsMojoTest {

  private static final String DEFAULT_HOSTNAME = "myapi.appspot.com";
  private static final String DEFAULT_URL = "https://" + DEFAULT_HOSTNAME + "/_ah/api/";
  private static final String DISCOVERY_DOC_PATH = "target/discovery-docs/testApi-v1-rest.discovery";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private void buildAndVerify(File projectDir) throws VerificationException {
    Verifier verifier = new Verifier(projectDir.getAbsolutePath());
    verifier.executeGoal("endpoints-framework:discoveryDocs");
    verifier.verifyErrorFreeLog();
    verifier.assertFilePresent(DISCOVERY_DOC_PATH);
  }

  @Test
  public void testDefault() throws IOException, VerificationException, XmlPullParserException {
    File testDir = new TestProject(tmpDir.getRoot(), "/projects/server").build();
    buildAndVerify(testDir);

    String discovery = Files.toString(new File(testDir, DISCOVERY_DOC_PATH), Charsets.UTF_8);
    Assert.assertThat(discovery, JUnitMatchers.containsString(DEFAULT_URL));
  }

  @Test
  public void testApplicationId()
      throws IOException, VerificationException, XmlPullParserException {
    File testDir = new TestProject(tmpDir.getRoot(), "/projects/server")
        .applicationId("maven-test")
        .build();
    buildAndVerify(testDir);

    String discovery = Files.toString(new File(testDir, DISCOVERY_DOC_PATH), Charsets.UTF_8);
    Assert.assertThat(discovery, CoreMatchers.not(JUnitMatchers.containsString(DEFAULT_URL)));
    Assert.assertThat(discovery,
        JUnitMatchers.containsString("https://maven-test.appspot.com/_ah/api"));

  }

  @Test
  public void testHostname() throws IOException, VerificationException, XmlPullParserException {
    File testDir = new TestProject(tmpDir.getRoot(), "/projects/server")
        .configuration("<configuration><hostname>my.hostname.com</hostname></configuration>")
        .build();
    buildAndVerify(testDir);

    String discovery = Files.toString(new File(testDir, DISCOVERY_DOC_PATH), Charsets.UTF_8);
    Assert.assertThat(discovery, CoreMatchers.not(JUnitMatchers.containsString(DEFAULT_URL)));
    Assert.assertThat(discovery, JUnitMatchers.containsString("https://my.hostname.com/_ah/api"));
  }

}

