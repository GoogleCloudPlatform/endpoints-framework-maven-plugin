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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class TestProject {

  private static boolean environmentPrepared = false;
  private static String pluginVersion = null;

  // this only needs to be done once, maven doesn't play well with junit suites, so just do it
  // here in the fixture.
  private static void prepareEnvironment()
      throws VerificationException, IOException, XmlPullParserException {
    if (!environmentPrepared) {
      Verifier verifier = new Verifier(".", true);
      // clean is causing some issues with appveyor builds
      verifier.setAutoclean(false);
      verifier.addCliOption("-DskipTests");
      verifier.executeGoal("install");

      MavenXpp3Reader reader = new MavenXpp3Reader();
      Model model = reader.read(new FileReader("pom.xml"));
      pluginVersion = model.getVersion();

      environmentPrepared = true;
    }
  }

  private final File testDir;
  private final String projectPathInResources;

  private String configuration;
  private String application;

  public TestProject(File testDir, String projectPathInResources) {
    this.testDir = testDir;
    this.projectPathInResources = projectPathInResources;
  }

  public TestProject configuration(String configuration) {
    this.configuration = configuration;
    return this;
  }

  public TestProject applicationId(String applicationId) {
    this.application = applicationId;
    return this;
  }

  public File build() throws IOException, VerificationException, XmlPullParserException {
    prepareEnvironment();
    File root = copyProject();
    if (application != null) {
      injectApplicationId(root, application);
    }
    if (configuration != null) {
      injectConfiguration(root, configuration);
    }
    return root;
  }

  private File copyProject() throws IOException {
    File projectRoot = ResourceExtractor
        .extractResourcePath(TestProject.class, projectPathInResources, testDir, true);

    File pom = new File(projectRoot, "pom.xml");
    String pomContents = FileUtils.fileRead(pom);
    pomContents = pomContents.replaceAll("@@PluginVersion@@", pluginVersion);
    FileUtils.fileWrite(pom, pomContents);

    return projectRoot;
  }

  // inject an endpoints plugin configuration into the pom.xml
  private void injectConfiguration(File projectRoot, String configuration) throws IOException {
    File pom = new File(projectRoot, "pom.xml");
    String pomContents = FileUtils.fileRead(pom);
    pomContents = pomContents.replaceAll("<!--endpoints-plugin-configuration-->", configuration);
    FileUtils.fileWrite(pom, pomContents);
  }

  // inject an application tag into the appengine-web.xml
  private void injectApplicationId(File projectRoot, String application) throws IOException {
    File app = new File(projectRoot, "src/main/webapp/WEB-INF/appengine-web.xml");
    String appContents = FileUtils.fileRead(app);
    appContents = appContents.replaceAll("<!--application-->", "<application>" + application + "</application>");
    FileUtils.fileWrite(app, appContents);
  }
}
