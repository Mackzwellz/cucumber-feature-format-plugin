package sdimkov.cucumber;


import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.*;


@Mojo(name = "restrict", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class FeatureRestrictorMojo extends AbstractMojo {

    private final String[] featureExtensions = new String[]{"feature"};

    @Parameter(property = "applyRestrictions", defaultValue = "true")
    private boolean applyRestrictions;

    @Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
    private File baseDir;

    private final Set<String> featureFileNames = new HashSet<>();
    private final Set<String> featureNames = new HashSet<>();
    private final Set<String> backgroundNames = new HashSet<>();
    private final Set<String> ruleNames = new HashSet<>();
    private final Set<String> scenarioNames = new HashSet<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Iterator<File> iterator = FileUtils.iterateFiles(baseDir, featureExtensions, true);
        boolean hasRestrictionIssues = false;
        List<String> restrictorIssues = new ArrayList<>();

        while (iterator.hasNext()) {
            File featureFile = iterator.next();
            try {
                getLog().debug("Processing " + featureFile.getAbsolutePath());

                if (applyRestrictions) {
                    doFeatureRestrict(featureFile);
                }

            } catch (IOException e) {
                getLog().error("Unable to process " + featureFile.getAbsolutePath(), e);
            } catch (IllegalStateException t) {
                String restrictorError = t.getMessage();
                getLog().error("Feature restrictor found an issue: " + restrictorError);
                restrictorIssues.add(restrictorError);
                hasRestrictionIssues = true;
            } catch (Throwable t) {
                getLog().error("Unhandled exception:", t);
            }
        }

        if (hasRestrictionIssues) {
            throw new MojoFailureException("Some files contain issues, fix them to proceed! List of issues:\n" + restrictorIssues);
        }
    }

    private void doFeatureRestrict(File featureFile)
            throws IOException {
        new FluentFeatureRestrictor(featureFile)
                .setFileNameRestrictionFor(featureFileNames)
                .setNameRestrictionFor(
                        Arrays.asList("Feature:", "Background:", "Rule:", "Scenario:", "Scenario Outline:"),
                        featureNames, backgroundNames, ruleNames, scenarioNames);
    }

}
