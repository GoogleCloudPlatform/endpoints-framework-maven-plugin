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

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.ZipFile;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.rules.TemporaryFolder;

public class ProjectTest {

  private static final String DEFAULT_URL = "https://myapi.appspot.com/_ah/api";
  private static final String DEFAULT_URL_PREFIX = "public static final String DEFAULT_ROOT_URL = ";
  private static final String DEFAULT_URL_VARIABLE = DEFAULT_URL_PREFIX + "\"https://myapi.appspot.com/_ah/api/\";";
  private static final String CLIENT_LIB_PATH = "target/client-libs/testApi-v1-java.zip";
  private static final String DISC_DOC_PATH = "target/discovery-docs/testApi-v1-rest.discovery";
  private static final String API_JAVA_FILE_PATH = "testApi/src/main/java/com/example/testApi/TestApi.java";
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
    verifier.assertFilePresent(CLIENT_LIB_PATH);
    verifier.assertFilePresent(DISC_DOC_PATH);

    String apiJavaFile = getFileContentsInZip(new File(testDir, CLIENT_LIB_PATH), API_JAVA_FILE_PATH);
    Assert.assertThat(apiJavaFile, JUnitMatchers.containsString(DEFAULT_URL_VARIABLE));

    String discovery = Files.toString(new File(testDir, DISC_DOC_PATH), Charsets.UTF_8);
    Assert.assertThat(discovery, JUnitMatchers.containsString(DEFAULT_URL));


  }

  @Test
  public void testApplicationId() throws IOException, VerificationException {
    File testDir = injectApplicationId(loadProject("/projects/server"), "<application>maven-test</application>");

    Verifier verifier = new Verifier(testDir.getAbsolutePath());
    verifier.executeGoals(
        Arrays.asList("endpoints-framework:clientLibs", "endpoints-framework:discoveryDocs"));
    verifier.verifyErrorFreeLog();

    String apiJavaFile = getFileContentsInZip(new File(testDir, CLIENT_LIB_PATH), API_JAVA_FILE_PATH);
    Assert.assertThat(apiJavaFile, JUnitMatchers.containsString(DEFAULT_URL_PREFIX + "\"https://maven-test.appspot.com/_ah/api/\";"));

    String discovery = Files.toString(new File(testDir, DISC_DOC_PATH), Charsets.UTF_8);
    Assert.assertThat(discovery, CoreMatchers.not(JUnitMatchers.containsString(DEFAULT_URL)));
    Assert.assertThat(discovery, JUnitMatchers.containsString("https://maven-test.appspot.com/_ah/api"));
  }

  @Test
  public void testHostnameAddon() throws IOException, VerificationException {
    File testDir = injectConfiguration(loadProject("/projects/server"),
        "<configuration><hostname>my.hostname.com</hostname></configuration>");

    Verifier verifier = new Verifier(testDir.getAbsolutePath());
    verifier.executeGoals(
        Arrays.asList("endpoints-framework:clientLibs", "endpoints-framework:discoveryDocs"));
    verifier.verifyErrorFreeLog();

    String apiJavaFile = getFileContentsInZip(new File(testDir, CLIENT_LIB_PATH), API_JAVA_FILE_PATH);
    Assert.assertThat(apiJavaFile, JUnitMatchers.containsString(DEFAULT_URL_PREFIX + "\"https://my.hostname.com/_ah/api/\";"));

    String discovery = Files.toString(new File(testDir, DISC_DOC_PATH), Charsets.UTF_8);
    Assert.assertThat(discovery, CoreMatchers.not(JUnitMatchers.containsString(DEFAULT_URL)));
    Assert.assertThat(discovery, JUnitMatchers.containsString("https://my.hostname.com/_ah/api"));
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
    File dir = ResourceExtractor.extractResourcePath(ProjectTest.class, path, testRoot.getRoot(), true);

    File pom = new File(dir, "pom.xml");
    String pomContents = FileUtils.fileRead(pom);
    pomContents = pomContents.replaceAll("@@PluginVersion@@", pluginVersion);
    FileUtils.fileWrite(pom, pomContents);

    return dir;
  }

  // inject a endpoints plugin configuration into the pom.xml
  private File injectConfiguration(File root, String configuration) throws IOException {
    File pom = new File(root, "pom.xml");
    String pomContents = FileUtils.fileRead(pom);
    pomContents = pomContents.replaceAll("<!--endpoints-plugin-configuration-->", configuration);
    FileUtils.fileWrite(pom, pomContents);
    return root;
  }

  // inject an application tag into the appengine-web.xml
  private File injectApplicationId(File root, String application) throws IOException {
    File app = new File(root, "src/main/webapp/WEB-INF/appengine-web.xml");
    String appContents = FileUtils.fileRead(app);
    appContents = appContents.replaceAll("<!--application-->", application);
    FileUtils.fileWrite(app, appContents);
    return root;
  }

  private String getFileContentsInZip(File zipFile, String path) throws IOException {
    ZipFile zip = new ZipFile(zipFile);
    InputStream is = zip.getInputStream(zip.getEntry(path));
    return CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
  }
}
