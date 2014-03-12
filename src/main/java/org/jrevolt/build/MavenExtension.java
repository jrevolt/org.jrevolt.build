package org.jrevolt.build;

import static org.apache.commons.lang.StringUtils.trimToNull;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.stax2.XMLInputFactory2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 * @version $Id$
 */
@Component(role = AbstractMavenLifecycleParticipant.class, hint = "setup")
public class MavenExtension extends AbstractMavenLifecycleParticipant {

    static {
        log("Loading...");
    }

    static public final String P_ENABLE         = "MavenExtension";
    static public final String P_DEBUG          = "MavenExtension.debug";
    static public final String P_COMMIT         = "build.commit";
    static public final String P_COMMIT_SIMPLE  = "build.commit.simple";
    static public final String P_BRANCH         = "build.branch";
    static public final String P_BRANCH_SIMPLE  = "build.branch.simple";
    static public final String P_TAG            = "build.tag";
    static public final String P_BUILD_ID       = "build.id";           // unique build identifier
    static public final String P_BUILD_COUNTER  = "build.counter";      // configuration-specific build counter
    static public final String P_TC_BUILDNUM    = "build.number";       // TC visual build identifier ('buildNumber' parameter)
    static public final String P_VERSION        = "build.version";
    static public final String P_TYPE           = "build.type";

    static public final String POM_BACKUP_FNAME = ".pom.xml";

    static enum BuildType { SNAPSHOT, RELEASE }

    static public boolean DEBUG = false;

    List<Runnable> cleanup = new LinkedList<Runnable>();

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        if (isEnabled(session)) return;

        DEBUG = Boolean.valueOf(getProperty(session, P_DEBUG, "false"));

        String buildId = getProperty(session, P_BUILD_ID, UUID.randomUUID().toString().replace("-", ""));
        String buildCounter = getProperty(session, P_BUILD_COUNTER, buildId);

        String detectedCommitId = detectGitCommit();
        String detectedBranch = detectGitBranch();
        String detectedTag = detectReleaseTag(detectedCommitId);

        String commitId = getProperty(session, P_COMMIT, detectedCommitId);
        String shortCommitId = getProperty(session, P_COMMIT_SIMPLE,  StringUtils.left(commitId, 10));
        String branch   = getProperty(session, P_BRANCH, detectedBranch);
        String tag = getProperty(session, P_TAG, detectedTag);

        // simple branch name is here to support GitFlow's feature/*, hotfix/* and release/* branches
        // we cannot use full branch name in artifact's version
        String simpleBranchName = getProperty(session, P_BRANCH_SIMPLE,
                branch != null ? branch.replaceFirst(".*/", "") : null);

        String type = getProperty(session, P_TYPE);
        BuildType buildType = (type != null) ? BuildType.valueOf(type) : null;

        if (buildType == null) {
            // warning: hardcoded assumption: release branch name == 'master'
            buildType = ("master".equals(simpleBranchName) && StringUtils.trimToNull(tag) != null)
                    ? BuildType.RELEASE
                    : BuildType.SNAPSHOT;
            type = getProperty(session, P_TYPE, buildType.name());
            log("Build type not specified. Autodetecting: %s", buildType);
        }

        boolean isDevelopment = branch.matches("^(develop|feature/.*)$");
        boolean isBugfix = branch.matches("^(release|hotfix)/.*$");
        boolean isRelease = buildType.equals(BuildType.RELEASE);

        if (isRelease && tag == null) {
            throw new MavenExecutionException("RELEASE build requires tag but none has been detected nor provided.", (Throwable) null);
        }
        
        String version = getProperty(session, P_VERSION, (isRelease) ? tag : simpleBranchName);

        String projectVersion
                = (isRelease) ? String.format("%s-%s", tag, buildId)
                : String.format("%s-SNAPSHOT", simpleBranchName);

        String buildNumber
                = (isDevelopment) ? buildId
                : (isBugfix) ? String.format("%s.%s", projectVersion, buildId)
                : (isRelease) ? projectVersion
                : buildId;

        log("%s: %s", P_BUILD_ID, buildId);
        log("%s: %s", P_BUILD_COUNTER, buildCounter);
        log("%s: %s", P_COMMIT, commitId);
        log("%s: %s", P_COMMIT_SIMPLE, shortCommitId);
        log("%s: %s", P_BRANCH, branch);
        log("%s: %s", P_BRANCH_SIMPLE, simpleBranchName);
        log("%s: %s", P_TAG, tag);
        log("%s: %s", P_TC_BUILDNUM, buildNumber);
        log("%s: %s", P_TYPE, type);
        log("%s: %s", P_VERSION, version);

        log("project.version: %s", projectVersion);

        // this is for teamcity; it will parse this line and extract the build number
        log("##teamcity[buildNumber '%s']", buildNumber);

        Model model;
        try {
            File fpom = new File(session.getExecutionRootDirectory(), "pom.xml");
            ModifiedPomXMLEventReader pom = newModifiedPomXER(PomHelper.readXmlFile(fpom));
            model = PomHelper.getRawModel(pom);
        } catch (MojoExecutionException e) {
            throw new UnsupportedOperationException(e);
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        }

        List<File> poms = new LinkedList<File>();
        findPoms(new File(session.getExecutionRootDirectory()), poms);

        for (File pom : poms) {
            rewritePOM(session, pom, model, projectVersion);
        }

        MavenExecutionListener.forSession(session).addListener(new AbstractExecutionListener() {
            @Override
            public void sessionEnded(ExecutionEvent event) {
                restorePoms();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                restorePoms();
            }
        });
    }

    ///

    private boolean isEnabled(MavenSession session) {
        return !getProperty(session, P_ENABLE, Boolean.toString(false)).equals(Boolean.toString(true));
    }

    void findPoms(final File dir, final List<File> poms) {
        final Set<String> ignores = new HashSet<String>(Arrays.asList("src", "target"));
        dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                // do not scan out directories; warning: QDH: assuming name 'target'
                if (pathname.isDirectory() && !ignores.contains(pathname.getName())) {
                    findPoms(pathname, poms);
                } else if (pathname.isFile() && pathname.getName().equals("pom.xml")) {
                    poms.add(pathname);
                }
                return false;
            }
        });
    }

    synchronized void restorePoms() {
        for (Runnable r : cleanup) {
            r.run();
        }
        cleanup.clear();
    }

    String detectGitBranch() {
        String name = trimToNull(execute("git rev-parse --abbrev-ref HEAD"));
        return (name == null || name.equals("HEAD")) ? null : name;
    }

    String detectReleaseTag(String commitId) {
        String out = trimToNull(execute("git show-ref --tags -d"));
		  if (out == null) { return null; }

        List<String> matching = new LinkedList<String>();

        Pattern ptag = Pattern.compile(".* refs/tags/(.*)");
        Pattern pbuild = Pattern.compile("^build-.*");
        Pattern prelease = Pattern.compile("^[0-9].*");

        LineIterator it = IOUtils.lineIterator(new StringReader(out));
        while (it.hasNext()) {
            String line = it.nextLine();
            if (line.startsWith(commitId)) {
                Matcher m = ptag.matcher(line);
                if (!m.matches()) { continue; }

                String tag = m.group(1).replace("^{}", "");

                if (pbuild.matcher(tag).matches()) { continue; }
                if (!prelease.matcher(tag).matches()) { continue; }

                matching.add(tag);
            }
        }

        if (matching.isEmpty()) { return null; }

        if (matching.size() > 1) {
            log("Multiple matching tags: %s. Selecting last one", matching);
        }

        return matching.get(matching.size() - 1);
    }

    String detectGitCommit() {
        return trimToNull(execute("git rev-parse HEAD"));
    }

    String execute(String commandline) {
        try {
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            PumpStreamHandler psh = new PumpStreamHandler(stdout, stderr);
            CommandLine cl = CommandLine.parse(commandline);
            DefaultExecutor exec = new DefaultExecutor();
            exec.setStreamHandler(psh);
            exec.execute(cl);
            return stdout.toString().trim();
        } catch (ExecuteException e) {
            debug("%s => %s", commandline, e.toString());
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error executing: "+commandline, e);
        }
    }

    ///

    String getProperty(MavenSession session, String name) {
        return getProperty(session, name, null);
    }

    String getProperty(MavenSession session, String name, String dflt) {
        String value = session.getUserProperties().getProperty(name,
                session.getSystemProperties().getProperty(name,
                        System.getProperty(name, dflt)));
        if (value != null && value.equals(dflt)) {
            session.getUserProperties().setProperty(name, value);
            session.getSystemProperties().setProperty(name, value);
            System.setProperty(name, value);
        }
        return value;
    }

    void rewritePOM(final MavenSession session, final File file, Model root, String projectVersion) {
        try {
            // get current pom.xml and read it
            ModifiedPomXMLEventReader pom = newModifiedPomXER(PomHelper.readXmlFile(file));

            Model model = PomHelper.getRawModel(pom);

            boolean modified = false;

            // update project version if specified and matches main project
            if (model.getVersion() != null && model.getVersion().equals(root.getVersion())) {
                PomHelper.setProjectVersion(pom, projectVersion);
                modified = true;
            }

            // update parent version if specified and matches main project
            if (model.getParent() != null && model.getParent().getVersion().equals(root.getVersion())) {
                PomHelper.setProjectParentVersion(pom, projectVersion);
                modified = true;
            }

            if (!modified) {
                return;
            }

            // create backup
            File bak = new File(file.getParentFile(), POM_BACKUP_FNAME);
            FileUtils.copyFile(file, bak);
            bak.setLastModified(file.lastModified());

            debug("Updated POM: %s. Versions: root: %s, project: %s, project.parent: %s, new version: %s",
                    file, root.getVersion(),
                    model.getVersion(),
                    model.getParent() != null ? model.getParent().getVersion() : null,
                    projectVersion);

            writeFile(file, pom.asStringBuilder());

            cleanup.add(new Runnable() {
                @Override
                public void run() {
                    try {
                        debug("Restoring original POM: %s", file);
                        File bak = new File(file.getParentFile(), POM_BACKUP_FNAME);
                        if (!bak.exists()) { return; }

                        FileUtils.copyFile(bak, file);
                        FileUtils.deleteQuietly(bak);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    protected final ModifiedPomXMLEventReader newModifiedPomXER( StringBuilder input ) throws
            MojoExecutionException {
        try {
            XMLInputFactory inputFactory = XMLInputFactory2.newInstance();
            inputFactory.setProperty(XMLInputFactory2.P_PRESERVE_LOCATION, Boolean.TRUE);
            ModifiedPomXMLEventReader newPom = new ModifiedPomXMLEventReader(input, inputFactory);
            return newPom;
        } catch (XMLStreamException e) {
            throw new MojoExecutionException("error reading input", e);
        }
    }

    protected final void writeFile( File outFile, StringBuilder input ) throws IOException {
        Writer writer = null;
        try {
            outFile.getParentFile().mkdirs();
            writer = WriterFactory.newXmlWriter(outFile);
            IOUtil.copy(input.toString(), writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    static void log(String message, Object ... args) {
        System.out.printf("[%s] %s%n", MavenExtension.class.getSimpleName(), String.format(message, args));
    }

    static void debug(String message, Object ... args) {
        if (!DEBUG) { return; }
        System.out.printf("[%s] %s%n", MavenExtension.class.getSimpleName(), String.format(message, args));
    }

}
