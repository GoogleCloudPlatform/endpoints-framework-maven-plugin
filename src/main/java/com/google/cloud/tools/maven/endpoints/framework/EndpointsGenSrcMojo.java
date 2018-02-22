/*
 * Copyright (c) 2016 Google Inc.
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

import com.google.api.server.spi.tools.EndpointsTool;
import com.google.api.server.spi.tools.GenClientLibAction;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/** Maven goal to create generated source dir from endpoints. */
@Mojo(
  name = "generateSrc",
  requiresDependencyResolution = ResolutionScope.COMPILE,
  defaultPhase = LifecyclePhase.GENERATE_SOURCES
)
public class EndpointsGenSrcMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /** Output directory for generated sources. */
  @Parameter(
    defaultValue = "${project.build.directory}/generated-sources/endpoints",
    property = "endpoints.generatedSrcDir",
    required = true
  )
  private File generatedSrcDir;

  @Parameter(property = "endpoints.discoveryDocs", required = true)
  private List<File> discoveryDocs;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!generatedSrcDir.exists() && !generatedSrcDir.mkdirs()) {
      throw new MojoExecutionException(
          "Failed to create output directory: " + generatedSrcDir.getAbsolutePath());
    }
    project.addCompileSourceRoot(generatedSrcDir.getAbsolutePath());
    File tempDir = Files.createTempDir();
    File tempZipsDir = new File(tempDir, "zips");
    tempZipsDir.mkdirs();

    tempDir.deleteOnExit();
    tempZipsDir.deleteOnExit();

    for (File discoveryDoc : discoveryDocs) {
      try {
        runEndpointsTools(discoveryDoc, tempZipsDir);
      } catch (Exception e) {
        throw new MojoExecutionException("EndpointsTool threw an exception : ", e);
      }
    }

    File[] zips =
        tempZipsDir.listFiles(
            new FileFilter() {
              @Override
              public boolean accept(File pathname) {
                // mark all files in tempZips for cleanup
                pathname.deleteOnExit();
                return pathname.getName().toLowerCase().endsWith(".zip");
              }
            });

    for (File zip : zips) {
      try {
        unzipSrcDirs(zip, generatedSrcDir);
      } catch (IOException e) {
        throw new MojoExecutionException("Exception when unzipping : " + zip.getAbsolutePath(), e);
      }
    }

    getLog().info(tempDir.getAbsolutePath());
  }

  private void runEndpointsTools(File discoveryDoc, File outputDir) throws Exception {
    List<String> params =
        new ArrayList<>(
            Arrays.asList(
                GenClientLibAction.NAME,
                "-l",
                "java",
                "-bs",
                "maven",
                "-o",
                outputDir.getAbsolutePath()));

    params.add(discoveryDoc.getAbsolutePath());
    getLog().info("Endpoints Tool params : " + params.toString());
    new EndpointsTool().execute(params.toArray(new String[params.size()]));
  }

  // Unzip out the <api-name>/src/main/java directories out from the zip
  // this method is very dependant on the endpoints archive following a convention
  private void unzipSrcDirs(File archive, File destinationDir) throws IOException {

    try (ZipFile zipFile = new ZipFile(archive)) {
      Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

      if (!zipEntries.hasMoreElements()) {
        return;
      }

      // this appears to be dependant on the zip file generator
      // (presumably our generator always uses "/"), but if it fails on
      // Windows, this is probably your culprit
      String srcMainRoot = zipEntries.nextElement().getName() + "src/main/java";

      while (zipEntries.hasMoreElements()) {
        ZipEntry zipEntry = zipEntries.nextElement();

        if (!zipEntry.isDirectory()) {
          if (zipEntry.getName().startsWith(srcMainRoot)) {

            File zipEntryDestination =
                new File(destinationDir, zipEntry.getName().substring(srcMainRoot.length()));

            if (!zipEntryDestination.exists()) {
              Files.createParentDirs(zipEntryDestination);
              java.nio.file.Files.copy(
                  zipFile.getInputStream(zipEntry),
                  zipEntryDestination.toPath(),
                  StandardCopyOption.REPLACE_EXISTING);
            }
          }
        }
      }
    }
  }
}
