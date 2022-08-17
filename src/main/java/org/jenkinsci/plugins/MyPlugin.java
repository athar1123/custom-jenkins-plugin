package org.jenkinsci.plugins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

public class MyPlugin extends BuildWrapper {
    private static final String REPORT_TEMPLATE_PATH = "/stats.html";
    private static final String PROJECT_NAME_VAR = "$PROJECT_NAME$";
    private static final String CLASSES_NUMBER_VAR = "$CLASSES_NUMBER$";
    private static final String LINES_NUMBER_VAR = "$LINES_NUMBER$";

    @DataBoundConstructor
    public MyPlugin() {
    }

    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener) {
        return new Environment() {
            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException
            {
                ProjectStats stats = buildStats();
                String report = generateReport(build.getProject().getDisplayName(), stats);
                File artifactsDir = build.getArtifactsDir();
                if (!artifactsDir.isDirectory()) {
                    boolean success = artifactsDir.mkdirs();
                    if (!success) {
                        listener.getLogger().println("Can't create artifacts directory at "
                                + artifactsDir.getAbsolutePath());
                    }
                }
                String path = artifactsDir.getCanonicalPath() + REPORT_TEMPLATE_PATH;
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path),
                        StandardCharsets.UTF_8))) {
                    writer.write(report);
                }
                return super.tearDown(build, listener);
            }
        };
    }

    private static ProjectStats buildStats() throws IOException, InterruptedException {

        return new ProjectStats(100, 10000);
    }



    private static String generateReport(String projectName, ProjectStats stats) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        try (InputStream in = MyPlugin.class.getResourceAsStream(REPORT_TEMPLATE_PATH)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                bOut.write(buffer, 0, read);
            }
        }
        String content = new String(bOut.toByteArray(), StandardCharsets.UTF_8);
        content = content.replace(PROJECT_NAME_VAR, projectName);
        content = content.replace(CLASSES_NUMBER_VAR, String.valueOf(stats.getClassesNumber()));
        content = content.replace(LINES_NUMBER_VAR, String.valueOf(stats.getLinesNumber()));
        return content;
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Construct project Stats HTML during build - TIM VIRAL MOHAMMAD";
        }

    }

}