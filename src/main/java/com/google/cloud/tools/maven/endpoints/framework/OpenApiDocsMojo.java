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

import com.google.api.server.spi.tools.EndpointsTool;
import com.google.api.server.spi.tools.GetOpenApiDocAction;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Goal which generates openapi docs
 */
@Mojo(name = "openApiDocs", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.PREPARE_PACKAGE)
public class OpenApiDocsMojo extends AbstractEndpointsWebAppMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /**
   * Output directory for openapi docs
   */
  @Parameter(defaultValue = "${project.build.directory}/openapi-docs",
             property = "endpoints.openApiDocDir", required = true)
  private File openApiDocDir;

  /**
   * Default hostname of the endpoint host.
   */
  @Parameter(property = "endpoints.hostname", required = false)
  private String hostname;

  /**
   * Default basePath of the endpoint host.
   */
  @Parameter(property = "endpoints.basePath", required = false)
  private String basePath;

  public void execute() throws MojoExecutionException {
    try {
      if (!openApiDocDir.exists() && !openApiDocDir.mkdirs()) {
        throw new MojoExecutionException(
            "Failed to create output directory: " + openApiDocDir.getAbsolutePath());
      }
      String classpath = Joiner.on(File.pathSeparator).join(project.getRuntimeClasspathElements());
      classpath += File.pathSeparator + classesDir;

      List<String> params = new ArrayList<>(Arrays.asList(
          GetOpenApiDocAction.NAME,
          "-o", computeOpenApiDocPath(),
          "-cp", classpath,
          "-w", webappDir.getAbsolutePath()));
      if (!Strings.isNullOrEmpty(hostname)) {
        params.add("-h");
        params.add(hostname);
      }
      if (!Strings.isNullOrEmpty(basePath)) {
        params.add("-p");
        params.add(basePath);
      }
      if (serviceClasses != null) {
        params.addAll(serviceClasses);
      }

      getLog().info("Endpoints Tool params : " + params.toString());
      new EndpointsTool().execute(params.toArray(new String[params.size()]));


    } catch (Exception e) {
      throw new MojoExecutionException("Endpoints Tool Error", e);
    }
  }

  private String computeOpenApiDocPath() {
    return new File(openApiDocDir, "openapi.json").getAbsolutePath();
  }
}
