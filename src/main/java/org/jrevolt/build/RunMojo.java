package org.jrevolt.build;

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.LinkedList;

/**
 * Allows executing any class from any artifact in repository, without any project required.
 *
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 * @version $Id$
 *
 * @goal run
 * @requiresProject false
 * @requiresDirectInvocation true
 */
public class RunMojo extends AbstractMojo {

    /**
     * The project currently being build.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The current Maven session.
     *
     * @parameter default-value="${session}"
     * @parameter required
     * @readonly
     */
    MavenSession session;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    BuildPluginManager pluginManager;

    /**
     * @parameter default-value="${run.mainClass}"
     * @required
     */
    String mainClass;

    /**
     * @parameter default-value="${run.mainArtifact}"
     * @required
     */
    String mainArtifact;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        MojoExecutor.ExecutionEnvironment env = executionEnvironment(project, session, pluginManager);

        Dependency dependency = new Dependency(); {
            String[] tokens = mainArtifact.split(":");
            if (tokens.length != 4) {
                throw new MojoFailureException("Invalid artifact definition: \""+mainArtifact+"\". Expected: groupId:artifactId:version:packaging");
            }

            dependency.setGroupId(tokens[0]);
            dependency.setArtifactId(tokens[1]);
            dependency.setVersion(tokens[2]);
            dependency.setType(tokens[3]);
        }

        Plugin exec = plugin("org.codehaus.mojo", "exec-maven-plugin", "1.2.1");
        // Maven 3.1 update: original dependency list is immutable, must build a new one
        exec.setDependencies(new LinkedList<Dependency>(exec.getDependencies()));
        exec.addDependency(dependency);

        executeMojo(
                exec,
                goal("java"),
                configuration(
                        element(name("mainClass"), mainClass),
                        element(name("includeProjectDependencies"), "false"),
                        element(name("includeProjectDependencies"), "false"),
                        element(name("includePluginDependencies"), "true")
                ),
                env);
    }

}

