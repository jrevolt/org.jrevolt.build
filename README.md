org.jrevolt.build
=================

Extensions
==========

MavenExtension
--------------

Provides for dynamic artifact version assignment without the need for explicit commit before/after build (like
maven-release-plugin does it). Artifact version is irrelevant during development, and it is computed automatically
for current build.

Features:

 - rewrites artifact version for current build
 - provides build with Git repository metadata like commit ID, branch name, git tag, build number, etc
 - propagates dynamically computed build identifier to TeamCity CI server
 - uses GitFlow semantics to infer missing information

Example:

(Real artifact version is always DEV-SNAPSHOT.)

| Branch		| Artifact version      | Description
|-----------------------|-----------------------|---
| develop		| develop-SNAPSHOT	| Snapshot build of main development branch
| feature/myfeature	| myfeature-SNAPSHOT	| Snapshot build of the feature branch
| release/1.0		| 1.0-SNAPSHOT		| Snapshot build of the release branch
| master		| 1.0			| Release build
| hotfix/		| 1.0-SNAPSHOT		| Snapshot build of the release branch



Properties:

| Name | Description |
|------|-------------|
| MavenExtension        | Boolean. Enables the extension. Default: false
| MavenExtension.debug	| Turns on some debug output. Default: false
| build.commit
| build.commit.simple
| build.branch
| build.branch.simple
| build.tag
| build.id
| build.counter
| build.number
| build.version
| build.type 		| RELEASE or SNAPSHOT. 


Goals
=====


org.jrevolt.build:run
---------------------

Takes a main artifact as a classpath root, lets Maven resolve all runtime dependencies and executes specified main
class.

Parameters:

| Parameter    | Property         | Description                |
|--------------|------------------|----------------------------|
| mainArtifact | run.mainArtifact | groupId:artifactId:version:packaging |
| mainClass    | run.mainClass    | fully qualified class name |

Example:

	mvn org.jrevolt:org.jrevolt.build:1.0:run \
		-Drun.mainArtifact=com.example:com.example.artifact:1.0:jar \
		-Drun.mainClass=com.example.artifact.Main \
		-Dany.number.of.properties.like.this=value



See: [exec-maven-plugin](http://mojo.codehaus.org/exec-maven-plugin/java-mojo.html)

Distribution
============

 - [Snapshots](https://nexus.greenhorn.sk/content/repositories/snapshots/org/jrevolt/org.jrevolt.build/)
 - [Releases](https://nexus.greenhorn.sk/content/repositories/releases/org/jrevolt/org.jrevolt.build/)


```xml
<dependency>
	<groupId>org.jrevolt</groupId>
	<artifactId>org.jrevolt.build</artifactId>
	<version>1-SNAPSHOT</version>
</dependency>
```
