![project status image](https://img.shields.io/badge/stability-experimental-orange.svg)
# Endpoints Framework Maven plugin

This Maven plugin provides goals and configurations to build Endpoints Framework projects.

# Requirements

Maven is required to build the plugin. To download Maven, follow the [instructions](http://maven.apache.org/).

The remaining dependencies are specified in the pom.xml file and should be automatically downloaded when the plugin is built.

# How to use

In your Maven App Engine Java app, add the following plugin to your pom.xml:

```XML
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>endpoints-framework-maven-plugin</artifactId>
    <version>0.1.0</version>
</plugin>
```

