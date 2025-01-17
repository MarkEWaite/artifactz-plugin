package io.iktech.jenkins.plugins.artifactz;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.artifactz.client.ServiceClient;
import io.artifactz.client.exception.ClientException;
import io.iktech.jenkins.plugins.artifactz.client.ServiceClientFactory;
import io.iktech.jenkins.plugins.artifactz.modules.ServiceClientFactoryModule;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.Set;

public class PublishArtifactStep extends Step {
    private static final Logger logger = LoggerFactory.getLogger(PublishArtifactStep.class);

    private String token;

    private String name;

    private String description;

    private String type;

    private String groupId;

    private String artifactId;

    private String stage;

    private String flow;

    private String stageDescription;

    private String version;

    private transient ServiceClientFactory serviceClientFactory;

    @DataBoundConstructor
    public PublishArtifactStep(String token, String name, String description, String type, String flow, String stage, String stageDescription, String groupId, String artifactId, String version) {
        this.token = token;
        this.name = name;
        this.description = description;
        this.type = type;
        this.flow = flow;
        this.stage = stage;
        this.stageDescription = stageDescription;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.serviceClientFactory = SingletonStore.getInstance();
    }

    public String getToken() {
        return token;
    }

    @DataBoundSetter
    public void setToken(String token) {
        this.token = token;
    }

    public String getStage() {
        return stage;
    }

    @DataBoundSetter
    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    @DataBoundSetter
    public void setType(String type) {
        this.type = type;
    }

    public String getGroupId() {
        return groupId;
    }

    @DataBoundSetter
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @DataBoundSetter
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getFlow() {
        return flow;
    }

    @DataBoundSetter
    public void setFlow(String flow) {
        this.flow = flow;
    }

    public String getStageDescription() {
        return stageDescription;
    }

    @DataBoundSetter
    public void setStageDescription(String stageDescription) {
        this.stageDescription = stageDescription;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new PublishArtifactStep.Execution(this.token, this.name, this.description, this.type, this.flow, this.stage, this.stageDescription, this.groupId, this.artifactId, this.version, context, this.serviceClientFactory);
    }

    private static final class Execution extends SynchronousNonBlockingStepExecution<Boolean> {
        private static final long serialVersionUID = 4829381492818317576L;

        private final String token;

        private final String name;

        private final String description;

        private final String type;

        private final String groupId;

        private final String artifactId;

        private final String stage;

        private final String flow;

        private final String stageDescription;

        private final String version;

        private final transient ServiceClientFactory serviceClientFactory;

        Execution(String token, String name, String description, String type, String flow, String stage, String stageDescription, String groupId, String artifactId, String version, StepContext context, ServiceClientFactory serviceClientFactory) {
            super(context);
            this.token = token;
            this.name = name;
            this.description = description;
            this.type = type;
            this.flow = flow;
            this.stage = stage;
            this.stageDescription = stageDescription;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.serviceClientFactory = serviceClientFactory;
        }

        @Override protected Boolean run() throws Exception {
            Run<?, ?> run = getContext().get(Run.class);
            TaskListener taskListener = getContext().get(TaskListener.class);

            try {
                ServiceClient client = this.serviceClientFactory.serviceClient(taskListener, ServiceHelper.getToken(run, taskListener, this.token));
                client.publishArtifact(this.stage, this.stageDescription, this.name, this.description, this.flow, this.type, this.groupId, this.artifactId, this.version);
            } catch (ClientException e) {
                logger.error("Error while publishing artifact", e);
                String errorMessage = "Error while publishing artifact: " + e.getMessage();
                ServiceHelper.interruptExecution(run, taskListener, "Error while publishing artifact version", e);
                throw new AbortException(errorMessage);
            }

            return true;
        }
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {
        @Override
        public String getFunctionName() {
            return "publishArtifact";
        }

        @Override
        public String getDisplayName() {
            return "Publish Artifact Version";
        }

        @Override
        public boolean isMetaStep() {
            return false;
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, FilePath.class, Launcher.class, TaskListener.class);
        }
    }
}
