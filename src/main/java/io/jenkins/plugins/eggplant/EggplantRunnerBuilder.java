package io.jenkins.plugins.eggplant;

import hudson.Launcher;
import hudson.Proc;
import hudson.Launcher.ProcStarter;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.eggplant.common.LogLevel;
import io.jenkins.plugins.eggplant.common.OperatingSystem;
import io.jenkins.plugins.eggplant.exception.BuilderException;
import io.jenkins.plugins.eggplant.exception.CLIExitException;
import io.jenkins.plugins.eggplant.utils.CLIRunnerHelper;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class EggplantRunnerBuilder extends Builder implements SimpleBuildStep {
    private String serverURL;
    private String testConfigId;
    private String testConfigName;
    private String modelName;
    private String suiteName;    
    private String clientId;
    private Secret clientSecret;
    private LogLevel logLevel;
    private String CACertPath;
    private String testResultPath;
    private String pollInterval;
    private String requestTimeout;
    private String requestRetries;
    private String testEnvironmentTimeout;
    private String backoffFactor;
    private Boolean dryRun;
    private String eggplantRunnerPath;
    private String testConfig;
    private String testConfigType;

    @DataBoundConstructor
    public EggplantRunnerBuilder() {
    }

    public String getServerURL() {
        return serverURL;
    }
    public String getTestConfigId() {
        return testConfigId;
    }
    public String getTestConfigName() {
        return testConfigName;
    }
    public String getModelName() {
        return modelName;
    }
    public String getSuiteName() {
        return suiteName;
    }
    public String getClientId() {
        return clientId;
    }
    public Secret getClientSecret() {
        return clientSecret;
    }
    public LogLevel getLogLevel() {
        return logLevel;
    }
    public String getCACertPath() {
        return CACertPath;
    }
    public String getTestResultPath() {
        return testResultPath;
    }
    public String getPollInterval() {
        return pollInterval;
    }
    public String getRequestTimeout() {
        return requestTimeout;
    }
    public String getRequestRetries() {
        return requestRetries;
    }
    public String getTestEnvironmentTimeout() {
        return testEnvironmentTimeout;
    }
    public String getBackoffFactor() {
        return backoffFactor;
    }
    public String getEggplantRunnerPath() {
        return eggplantRunnerPath;
    }  
    
    public Boolean getDryRun() {
        return dryRun;
    } 

    public String getTestConfig() {
        // Could return currently configured/saved item here to initialized form with this data
        // return null;
        return testConfig;
    }

    public String getTestConfigType() {
        return testConfigType;
    }

    @DataBoundSetter
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    @DataBoundSetter
    public void setTestConfigId(String testConfigId) {
        this.testConfigId = testConfigId;
    }

    @DataBoundSetter
    public void setTestConfigName(String testConfigName) {
        this.testConfigName = testConfigName;
    }

    @DataBoundSetter
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    @DataBoundSetter
    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    @DataBoundSetter
    public void setClientId(String clientId) {
        this.clientId= clientId;
    }

    @DataBoundSetter
    public void setClientSecret(Secret clientSecret) {
        this.clientSecret = clientSecret;
    }

    @DataBoundSetter
    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    @DataBoundSetter
    public void setCACertPath(String CACertPath) {
        this.CACertPath = CACertPath;
    }

    @DataBoundSetter
    public void setTestResultPath(String testResultPath) {
        this.testResultPath = testResultPath;
    }

    @DataBoundSetter
    public void setPollInterval(String pollInterval) {
        this.pollInterval = pollInterval;
    }

    @DataBoundSetter
    public void setRequestTimeout(String requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    @DataBoundSetter
    public void setRequestRetries(String requestRetries) {
        this.requestRetries = requestRetries;
    }

    @DataBoundSetter
    public void setTestEnvironmentTimeout(String testEnvironmentTimeout) {
        this.testEnvironmentTimeout = testEnvironmentTimeout;
    }

    @DataBoundSetter
    public void setBackoffFactor(String backoffFactor) {
        this.backoffFactor = backoffFactor;
    }   

    @DataBoundSetter
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @DataBoundSetter
    public void setEggplantRunnerPath(String eggplantRunnerPath) {
        this.eggplantRunnerPath = eggplantRunnerPath;
    }

    @DataBoundSetter
    public void setTestConfig(String testConfig) {
        this.testConfig = testConfig;
    }  

    @DataBoundSetter
    public void setTestConfigType(String testConfigType) {
        this.testConfigType = testConfigType;
    }    

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        String buildId = run.getId();
        String localeString = "";
        OperatingSystem os = this.getOperatingSystem(workspace, launcher);
        FilePath uniqueWorkspace = workspace.child(buildId);
        uniqueWorkspace.mkdirs(); 

        logger.println("Exported locale: " +  localeString);
        EnvVars envVars = new EnvVars();
        envVars.put("LC_ALL", localeString);
        envVars.put("LANG",  localeString);
        
        CLIRunnerHelper CLIRunnerHelper = new CLIRunnerHelper(workspace, os, logger);
        if(this.eggplantRunnerPath != null && !this.eggplantRunnerPath.equals(""))
            CLIRunnerHelper.copyRunnerFrom(this.eggplantRunnerPath);
        else
            CLIRunnerHelper.downloadRunner(env.get("gitlabAccessToken"));

        FilePath cliRunnerPath = CLIRunnerHelper.getFilePath();
        String[] command = this.getCommand(cliRunnerPath, env);
        logger.println("command: " + Arrays.toString(command));

        cliRunnerPath.chmod(0755);
        logger.println(">> Executing " + cliRunnerPath);
        
        ProcStarter procStarter = launcher.launch();
        Proc process = procStarter.pwd(uniqueWorkspace).cmds(command).envs(envVars).quiet(false).stderr(logger).stdout(logger).start();
        int exitCode = process.join();
        if (exitCode != 0) throw new CLIExitException(exitCode);
    }

    private OperatingSystem getOperatingSystem(FilePath workspace, Launcher launcher) throws IOException, InterruptedException {
        
        // If not Unix, then it is Windows
        if (!launcher.isUnix()) 
            return OperatingSystem.WINDOWS;

        ProcStarter procStarter = launcher.launch();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Proc process = procStarter.pwd(workspace).cmds("uname").quiet(true).stdout(outputStream).start();
        process.join();
        String output = outputStream.toString("UTF-8");

        // MacOS
        if (output.startsWith("Darwin")) 
            return OperatingSystem.MACOS;
        
        // Linux
        return OperatingSystem.LINUX;
      }

    
    private String[] getCommand(FilePath cliFile, EnvVars env) throws BuilderException {
        List<String> commandList = new ArrayList<String>();

        //commandList.add("./" + cliFile.getName()); // cliRunnerPath
        commandList.add(cliFile.getRemote()); // cliRunnerPath

         TestConfigCommand cmd = new TestConfigCommand();
         cmd.serverURL = this.serverURL;

        if(this.testConfig != null)
        {
            if(this.testConfig.equals("BY_ID"))
            {
               cmd.testConfigId = this.testConfigId;
            }
            else if(this.testConfig.equals("BY_NAME"))
            {
               cmd.testConfigName = this.testConfigName;
               
               if(this.testConfigType != null)
                {
                    if(this.testConfigType.equals("MODEL"))
                        cmd.modelName = this.modelName;
                    else if (this.testConfigType.equals("SCRIPT"))
                        cmd.suiteName = this.suiteName;
                }
            }
        }
        else
        {
            // pipeline 
            cmd.testConfigId = this.testConfigId;
            cmd.testConfigName = this.testConfigName;
            cmd.modelName = this.modelName;
            cmd.suiteName = this.suiteName;
        }
        
        commandList.addAll(cmd.getCommand());

        if (this.clientId != null && !this.clientId.equals("")) // clientIdArg
            commandList.add(String.format("--client-id=%s", this.clientId)); 

        // clientSecretArg
        if (this.clientSecret != null && !this.clientSecret.getPlainText().isEmpty()) 
            commandList.add(String.format("--client-secret=%s", this.clientSecret));
        else if (env.get("DAI_CLIENT_SECRET") != null && !env.get("DAI_CLIENT_SECRET").equals("")) 
            commandList.add(String.format("--client-secret=%s", env.get("DAI_CLIENT_SECRET")));

        if (this.logLevel != null) // logLevelArg
            commandList.add(String.format("--log-level=%s", this.logLevel)); 
        if (this.CACertPath != null && !this.CACertPath.equals("")) // CACertPathArg
            commandList.add(String.format("--ca-cert-path=%s", this.CACertPath));
        if (this.testResultPath != null && !this.testResultPath.equals("")) // testResultPathArg
            commandList.add(String.format("--test-result-path=%s", this.testResultPath)); 
        if (this.pollInterval != null && !this.pollInterval.equals("")) // pollIntervalArg
            commandList.add(String.format("--poll-interval=%s", this.pollInterval)); 
        if (this.requestTimeout != null && !this.requestTimeout.equals("")) // requestTimeoutArg
            commandList.add(String.format("--request-timeout=%s", this.requestTimeout)); 
        if (this.requestRetries != null && !this.requestRetries.equals("")) // requestRetriesArg
            commandList.add(String.format("--request-retries=%s", this.requestRetries)); 
        if (this.testEnvironmentTimeout != null && !this.testEnvironmentTimeout.equals("")) // testEnvironmentTimeoutArg
            commandList.add(String.format("--test-environment-timeout=%s", this.testEnvironmentTimeout)); 
        if (this.dryRun != null && this.dryRun) // dryRunArg
            commandList.add("--dry-run");
        if (this.backoffFactor != null && !this.backoffFactor.equals("")) // backoffFactorArg
            commandList.add(String.format("--backoff-factor=%s", this.backoffFactor));             

        return commandList.toArray(new String[0]);
    }

    public class TestConfigCommand
    {
        public String serverURL = null;
        public String testConfigId = null;
        public String testConfigName = null;
        public String modelName = null;
        public String suiteName = null;

        TestConfigCommand(){

        }
 
        public List<String> getCommand() throws BuilderException
        {
            List<String> commandList = new ArrayList<String>();
          
            if(this.testConfigId != null)
            {
                commandList.add(this.serverURL);
                commandList.add(this.testConfigId);
            }
            else if(this.testConfigName != null)
            {
                if(this.modelName != null && this.suiteName != null)
                    throw new BuilderException("Error: modelName and suiteName found,  Use testConfigName with only suiteName or modelName to continue.");

                if(this.modelName != null)
                {
                    commandList.add("modelbased");
                    commandList.add(this.serverURL);
                    commandList.add(String.format("--test-config-name=%s", this.testConfigName));
                    commandList.add(String.format("--model-name=%s", this.modelName));
                }
                else if(this.suiteName != null)
                {
                    commandList.add("scriptbased");
                    commandList.add(this.serverURL);
                    commandList.add(String.format("--test-config-name=%s", this.testConfigName));
                    commandList.add(String.format("--suite-name=%s", this.suiteName));
                }
                else
                    throw new BuilderException("Error: testConfigName found, suiteName or modelName is required.");
            }
            else
                throw new BuilderException("Error:  testConfigId and testConfigName not found. Use only testConfigId or testConfigName (with modelName or suiteName) to continue.");
        
            return commandList;
        }
    }
    
    @Symbol("eggplantRunner")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Eggplant Runner";
        }

        public FormValidation doCheckServerURL(@QueryParameter String value) throws IOException{
            if(value.isEmpty()) {
                return FormValidation.error("Server URL cannot be empty.");
            }
            else if(!isValidURL(value)){
                return FormValidation.error("Invalid server url.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestConfigId(@QueryParameter String value) throws IOException {
            if(value.isEmpty()) {
                return FormValidation.error("Test Config Id cannot be empty.");
            }
            else if(!isValidUuid(value)){
                return FormValidation.error("Invalid test configuration id.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestConfigName(@QueryParameter String value) throws IOException {
            if(value.isEmpty()) {
                return FormValidation.error("Test Config Name cannot be empty.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckSuiteName(@QueryParameter String value) throws IOException {
            if(value.isEmpty()) {
                return FormValidation.error("Suite Name cannot be empty.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckModelName(@QueryParameter String value) throws IOException {
            if(value.isEmpty()) {
                return FormValidation.error("Model Name cannot be empty.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckClientId(@QueryParameter String value) throws IOException {
            if(value.isEmpty()) {
                return FormValidation.error("Client Id cannot be empty.");
            }
            return FormValidation.ok();
        }
       
        public FormValidation doCheckClientSecret(@QueryParameter String value) throws IOException {
            if(value.isEmpty()) {
                return FormValidation.error("Client Secret cannot be empty.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestResultPath(@QueryParameter String value) throws IOException {
            if(!value.isEmpty()&&(!isValidPath(value,"xml"))){
                return FormValidation.error("Invalid Test Result Path.");
            }
            else
                return FormValidation.ok();
        }

        public FormValidation doCheckPollInterval(@QueryParameter String value) throws IOException {
            if(!value.isEmpty()&&!isValidNumeric(value)){
                return FormValidation.error("Invalid Poll Interval.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckRequestTimeout(@QueryParameter String value) throws IOException {
            if(!value.isEmpty()&&!isValidNumeric(value)){
                return FormValidation.error("Invalid Request Timeout.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckRequestRetries(@QueryParameter String value) throws IOException {
            if(!value.isEmpty()&&!isValidNumeric(value)){
                return FormValidation.error("Invalid Request Retries.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestEnvironmentTimeout(@QueryParameter String value) throws IOException {
            if(!value.isEmpty()&&!isValidNumeric(value)){
                return FormValidation.error("Invalid Test Environment Timeout.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckBackoffFactor(@QueryParameter String value) throws IOException {
            if(!value.isEmpty()&&!isValidDecimal(value)){
                return FormValidation.error("Invalid Backoff Factor.");
            }
            return FormValidation.ok();
        }

        private Boolean isValidURL(String value){
            Pattern p = Pattern.compile("^https?:\\/\\/([a-zA-Z0-9]+)((\\-|\\.)[a-zA-Z0-9]+)*(:[0-9]+)?(\\/?)$");
            Boolean isMatch=p.matcher(value).matches();
            if(isMatch)
                return true;
            else
                return false;
        }

        private Boolean isValidUuid(String value){
            Pattern p = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
            Boolean isMatch=p.matcher(value).matches();
            if(isMatch)
                return true;
            else
                return false;
        }

        private Boolean isValidNumeric(String value){
            Pattern p = Pattern.compile("^\\d+$");
            Boolean isMatch=p.matcher(value).matches();
            if(isMatch)
                return true;
            else
                return false;
        }

        private Boolean isValidDecimal(String value){
            Pattern p = Pattern.compile("^\\d+\\.?\\d*$");
            Boolean isMatch=p.matcher(value).matches();
            if(isMatch)
                return true;
            else
                return false;
        }
        
        public static boolean isValidPath(String path, String extension) {
            Pattern p = Pattern.compile("[^?\"*<>|]+."+extension+"$");
            Boolean isMatch=p.matcher(path).matches();
            if(isMatch)
                return true;
            else
                return false;
        }

    }
    
}
