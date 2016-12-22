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

package com.google.cloud.tools.maven.endpoints.framework;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class ProjectTest {

  public static String pluginVersion = null;

  @Rule
  public TemporaryFolder testRoot = new TemporaryFolder();

  @BeforeClass
  public static void setUp() throws VerificationException, IOException, XmlPullParserException {
    Verifier verifier = new Verifier(".", true);
    // clean is causing some issues with appveyor builds
    verifier.setAutoclean(false);
    verifier.addCliOption("-DskipTests");
    verifier.executeGoal("install");

    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model model = reader.read(new FileReader("pom.xml"));

    pluginVersion = model.getVersion();
  }

  @Test
  public void testServerArtifactCreation() throws IOException, VerificationException {
    File testDir = loadProject("/projects/server");

    Verifier verifier = new Verifier(testDir.getAbsolutePath());
    verifier.executeGoals(
        Arrays.asList("endpoints-framework:clientLibs", "endpoints-framework:discoveryDocs"));
    verifier.verifyErrorFreeLog();
    verifier.assertFilePresent("target/client-libs/testApi-v1-java.zip");
    verifier.assertFilePresent("target/discovery-docs/testApi-v1-rest.discovery");
  }

  @Test
  public void testClientGeneratedSourceCreation() throws IOException, VerificationException {
    File testDir = loadProject("/projects/client");

    Verifier verifier = new Verifier(testDir.getAbsolutePath());
    verifier.executeGoals(Collections.singletonList("compile"));
    verifier.verifyErrorFreeLog();
    verifier.assertFilePresent("target/generated-sources/endpoints/com/example/testApi/TestApi.java");
  }

  private File loadProject(String path) throws IOException {
    File dir = ResourceExtractor.extractResourcePath(ProjectTest.class, path, testRoot.getRoot());

    File pom = new File(dir, "pom.xml");
    String pomContents = FileUtils.fileRead(pom);
    pomContents = pomContents.replaceAll("@@PluginVersion@@", pluginVersion);
    FileUtils.fileWrite(pom, pomContents);

    return dir;
  }
}
