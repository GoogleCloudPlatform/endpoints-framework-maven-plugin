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

import com.google.api.server.spi.tools.EndpointsTool;
import com.google.api.server.spi.tools.GetClientLibAction;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Maven goal to generate client libraries (as zips)
 */
@Mojo(name = "clientLibs", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.PREPARE_PACKAGE)
public class ClientLibsMojo extends AbstractEndpointsWebAppMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /**
   * Output directory for client libraries
   */
  @Parameter(defaultValue = "${project.build.directory}/client-libs",
             property = "endpoints.clientLibDir", required = true)
  private File clientLibDir;

  /**
   * Default hostname of the Endpoint Host.
   */
  @Parameter(property = "endpoints.hostname")
  private String hostname;

  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!clientLibDir.exists()) {
      if (!clientLibDir.mkdirs()) {
        throw new MojoExecutionException(
            "Failed to create output directory: " + clientLibDir.getPath());
      }
    }
    try {
      String classpath = Joiner.on(File.pathSeparator)
          .join(project.getCompileClasspathElements(), classesDir);

      List<String> params = new ArrayList<>(Arrays.asList(
          GetClientLibAction.NAME,
          "-o", clientLibDir.getPath(),
          "-cp", classpath,
          "-l", "java",
          "-bs", "maven",
          "-w", webappDir.getPath()));
      if (!Strings.isNullOrEmpty(hostname)) {
        params.add("-h");
        params.add(hostname);
      }
      if (serviceClasses != null) {
        params.addAll(serviceClasses);
      }

      System.out.print(params);
      new EndpointsTool().execute(params.toArray(new String[params.size()]));

    } catch (Exception e) {
      throw new MojoExecutionException("Endpoints Tool Error", e);
    }
  }
}
