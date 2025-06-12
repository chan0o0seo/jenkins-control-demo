package com.example.demo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Ec2DeployRequestDto {
    private String scriptType; // "ec2DeployJob" 고정값 검증 로직 추가 가능

    private String repoUrl;

    private String branch; // default: main

    private List<String> buildCommands;

    private String artifactPath;

    private Ec2Info ec2;

    @Data @Builder
    public static class Ec2Info {
        private String host;
        private String user;
        private String sshKeySecretName;
        private String targetDir;
        private List<String> preDeployCommands;
        private List<String> postDeployCommands;
    }
}
