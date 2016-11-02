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

package com.google.cloud.tools;

import com.google.api.server.spi.tools.EndpointsTool;
import com.google.api.server.spi.tools.GetDiscoveryDocAction;
import com.google.common.base.Joiner;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Goal which generates discovery docs
 */
@Mojo(name = "discoveryDocs", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.PREPARE_PACKAGE)
public class DiscoveryDocsMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  MavenProject project;

  @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
  private File classesDir;

  @Parameter(defaultValue = "${project.build.directory}/discovery-docs", property = "endpoints.discoveryDocDir", required = true)
  private File discoveryDocDir;

  @Parameter(property = "endpoints.serviceClasses", required = false)
  private List<String> serviceClasses;

  @Parameter(defaultValue = "rest", property = "endpoints.format", required = true)
  private String format;

  @Parameter(defaultValue = "${basedir}/src/main/webapp", property = "endpoints.webappDir", required = true)
  private File webappDir;

  public void execute() throws MojoExecutionException {
    try {
      if (!discoveryDocDir.exists()) {
        discoveryDocDir.mkdirs();
      }
      String classpath = Joiner.on(System.getProperty("path.separator")).join(project.getCompileClasspathElements(), classesDir);
      List<String> params = new ArrayList<>(Arrays.asList(
          GetDiscoveryDocAction.NAME,
          "-o", discoveryDocDir.getPath(),
          "-f", format,
          "-cp", classpath,
          "-w", webappDir.getPath()));
      if (serviceClasses != null) {
        params.addAll(serviceClasses);
      }
      new EndpointsTool().execute(params.toArray(new String[params.size()]));

    } catch (Exception e) {
      throw new MojoExecutionException("Endpoints Tool Error", e);
    }
  }
}
