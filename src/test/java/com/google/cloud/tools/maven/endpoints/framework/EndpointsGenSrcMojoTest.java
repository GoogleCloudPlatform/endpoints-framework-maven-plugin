/*
 * Copyright (c) 2017 Google Inc.
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
import java.io.IOException;
import java.util.Collections;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EndpointsGenSrcMojoTest {

  @Rule public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void testClientGeneratedSourceCreation()
      throws XmlPullParserException, IOException, VerificationException {
    File testDir = new TestProject(tmpDir.getRoot(), "/projects/client").build();

    Verifier verifier = new Verifier(testDir.getAbsolutePath());
    verifier.executeGoals(Collections.singletonList("compile"));
    verifier.verifyErrorFreeLog();
    verifier.assertFilePresent(
        "target/generated-sources/endpoints/com/example/testApi/TestApi.java");
  }
}
