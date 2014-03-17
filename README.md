=================
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

(Original versioned artifact version is always local-SNAPSHOT. Name is updated only for build/deployment.)

| Branch		| Artifact version      | Description
|-----------------------|-----------------------|---
| develop		| develop-SNAPSHOT	| Snapshot build of main development branch
| feature/myfeature	| myfeature-SNAPSHOT	| Snapshot build of the feature branch
| release/1.0		| 1.0-SNAPSHOT		| Snapshot build of the release branch
| master		| 1.0-${build.id}	| Release build. `build.tag` must be defined.
| hotfix/1.0.1		| 1.0.1-SNAPSHOT	| Snapshot build of the release branch


Properties:

| Name | Description |
|------|-------------|
| MavenExtension        | Boolean. Enables the extension. Default: false
| MavenExtension.debug	| Turns on some debug output. Default: false
| build.commit
| build.commit.simple
| build.branch
| build.branch.simple
| build.tag		| Tag identifier for release build. See below
| build.id		| Build identifier provided by CI server. Defaults to random UUID. In TeamCity, this should be mapped to either global build ID (`%teamcity.buld.id%`) or a build configuration's build counter (`%build.counter%`).
| build.counter		| Build counter. Defaults to `build.id`. In TeamCity, this should be mapped to build configuration's build counter (`%build.counter%`).
| build.number
| build.version
| build.type 		| RELEASE or SNAPSHOT. 

### Tags and Releases

Release build can be trigerred by specifying `build.tag`. Extension is able to automatically detect the tag presence on `HEAD` of the `master` branch.

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

Installation
============

Grab latest distribution (ZIP) from Nexus and unpack it into $M2_HOME/lib/ext/.

Enable the extension using -DMavenExtension=true

Sample Output
=============

Snapshot build of the `develop` branch:

	$ mvn clean deploy -DMavenExtension=true
	[MavenExtension] Loading...
	[MavenExtension] Build type not specified. Autodetecting: SNAPSHOT
	[MavenExtension] build.id: 539b9c0543664e27ae20dd383bde42dd
	[MavenExtension] build.counter: 539b9c0543664e27ae20dd383bde42dd
	[MavenExtension] build.commit: 14a536847548f71d0f917c990e3dacf9ec3c5906
	[MavenExtension] build.commit.simple: 14a5368475
	[MavenExtension] build.branch: develop
	[MavenExtension] build.branch.simple: develop
	[MavenExtension] build.tag: null
	[MavenExtension] build.number: 539b9c0543664e27ae20dd383bde42dd
	[MavenExtension] build.type: SNAPSHOT
	[MavenExtension] build.version: develop
	[MavenExtension] project.version: develop-SNAPSHOT
	[MavenExtension] ##teamcity[buildNumber '539b9c0543664e27ae20dd383bde42dd']
	[INFO] Scanning for projects...
