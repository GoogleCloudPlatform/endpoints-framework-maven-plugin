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
import com.google.common.io.CharStreams;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipFile;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.rules.TemporaryFolder;

public class ClientLibsMojoTest {

  private static final String DEFAULT_HOSTNAME = "myapi.appspot.com";
  private static final String DEFAULT_URL = "https://" + DEFAULT_HOSTNAME + "/_ah/api/";
  private static final String DEFAULT_URL_PREFIX = "public static final String DEFAULT_ROOT_URL = ";
  private static final String DEFAULT_URL_VARIABLE = DEFAULT_URL_PREFIX + "\"" + DEFAULT_URL + "\";";
  private static final String CLIENT_LIB_PATH = "target/client-libs/testApi-v1-java.zip";
  private static final String API_JAVA_FILE_PATH = "testApi/src/main/java/com/example/testApi/TestApi.java";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private void buildAndVerify(File projectDir) throws VerificationException {
    Verifier verifier = new Verifier(projectDir.getAbsolutePath());
    verifier.executeGoal("endpoints-framework:clientLibs");
    verifier.verifyErrorFreeLog();
    verifier.assertFilePresent(CLIENT_LIB_PATH);
  }

  @Test
  public void testDefault() throws IOException, VerificationException, XmlPullParserException {
    File testDir = new TestProject(tmpDir.getRoot(), "/projects/server").build();
    buildAndVerify(testDir);

    String apiJavaFile = getFileContentsInZip(new File(testDir, CLIENT_LIB_PATH), API_JAVA_FILE_PATH);
    Assert.assertThat(apiJavaFile, JUnitMatchers.containsString(DEFAULT_URL_VARIABLE));
  }

  @Test
  public void testApplicationId() throws IOException, VerificationException, XmlPullParserException {
    File testDir = new TestProject(tmpDir.getRoot(), "/projects/server").applicationId("maven-test").build();
    buildAndVerify(testDir);

    String apiJavaFile = getFileContentsInZip(new File(testDir, CLIENT_LIB_PATH), API_JAVA_FILE_PATH);
    Assert.assertThat(apiJavaFile, CoreMatchers.not(JUnitMatchers.containsString(DEFAULT_URL_VARIABLE)));
    Assert.assertThat(apiJavaFile, JUnitMatchers.containsString(DEFAULT_URL_PREFIX + "\"https://maven-test.appspot.com/_ah/api/\";"));
  }

  @Test
  public void testHostname() throws IOException, VerificationException, XmlPullParserException {
    File testDir = new TestProject(tmpDir.getRoot(), "/projects/server")
        .configuration("<configuration><hostname>my.hostname.com</hostname></configuration>")
        .build();
    buildAndVerify(testDir);

    String apiJavaFile = getFileContentsInZip(new File(testDir, CLIENT_LIB_PATH), API_JAVA_FILE_PATH);
    Assert.assertThat(apiJavaFile, CoreMatchers.not(JUnitMatchers.containsString(DEFAULT_URL_VARIABLE)));
    Assert.assertThat(apiJavaFile, JUnitMatchers.containsString(DEFAULT_URL_PREFIX + "\"https://my.hostname.com/_ah/api/\";"));
  }

  private String getFileContentsInZip(File zipFile, String path) throws IOException {
    ZipFile zip = new ZipFile(zipFile);
    InputStream is = zip.getInputStream(zip.getEntry(path));
    return CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
  }
}
