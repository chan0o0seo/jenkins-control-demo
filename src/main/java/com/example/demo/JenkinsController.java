package com.example.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/jenkins")
public class JenkinsController {

    @Value("${jenkins.url}")
    private String url;

    @Value("${jenkins.username}")
    private String jenkinsUsername;

    @Value("${jenkins.token}")
    private String jenkinsToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ScriptTemplateService scriptTemplateService;

    /*@PostMapping("/create-job/{jobName}")
    public ResponseEntity<?> createPipelineJob(
            @PathVariable String jobName,@RequestBody Ec2DeployRequestDto dto,
            BindingResult br) {
        // 1) DTO 검증
        if (br.hasErrors()) {
            List<String> errors = br.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .toList();
            return ResponseEntity.badRequest().body(Map.of("error", "검증 실패", "details", errors));
        }
        // 2) 기본값 처리
        if (dto.getBranch() == null || dto.getBranch().isBlank()) {
            dto.setBranch("main");
        }
        // 3) Pipeline 스크립트 생성
        String pipelineScript;
        try {
            pipelineScript = scriptTemplateService.generateEc2JenkinsPipeline(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Jenkinsfile 생성 실패", "message", e.getMessage()));
        }
        // 4) config.xml 생성
        String configXml;
        try {
            configXml = scriptTemplateService.generateJenkinsJobConfig(jobName, pipelineScript);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Jenkins job config 생성 실패", "message", e.getMessage()));
        }
        // 5) Jenkins API 호출: Job 생성
        ResponseEntity<String> jenkinsResponse;
        try {
            jenkinsResponse = jenkinsClient.createJob(jobName, configXml);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Jenkins API 호출 실패", "message", e.getMessage()));
        }
        // 6) Jenkins 응답 처리
        if (jenkinsResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.ok(Map.of("message", "Jenkins job 생성 성공", "jenkinsResponse", jenkinsResponse.getBody()));
        } else {
            return ResponseEntity.status(jenkinsResponse.getStatusCode())
                    .body(Map.of("error", "Jenkins job 생성 실패", "jenkinsStatus", jenkinsResponse.getStatusCodeValue(), "jenkinsResponse", jenkinsResponse.getBody()));
        }
    }*/

    @PostMapping("/create-job/{jobName}")
    public ResponseEntity<String> createJenkinsJob(@PathVariable String jobName,@RequestBody Ec2DeployRequestDto dto) throws IOException {

        if (dto.getBranch() == null || dto.getBranch().isBlank()) {
            dto.setBranch("main");
        }

        String jenkinsUrl = url + "/createItem?name="+jobName;
        String username = jenkinsUsername;
        String apiToken = jenkinsToken;

        String auth = username + ":" + apiToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.set("Authorization", "Basic " + encodedAuth);

        ClassPathResource resource = new ClassPathResource("jenkins/config.xml");
        //String configXml = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        String pipelineScript;
        try {
            pipelineScript = scriptTemplateService.generateEc2DeployScript(dto);
            pipelineScript = pipelineScript.replace("\r\n", "\n").replace("\r", "");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        // 4) config.xml 생성
        String configXml="fef";
        try {
            configXml = scriptTemplateService.generateJenkinsJobConfig(jobName, pipelineScript);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        System.out.println(configXml);
        HttpEntity<String> requestEntity = new HttpEntity<>(configXml, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                jenkinsUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
       //return ResponseEntity.ok(configXml);
    }

    @PostMapping("/update-job/{jobName}")
    public ResponseEntity<String> updateJenkinsJob(@PathVariable String jobName) throws IOException {
        String jenkinsUrl = url + "/job/" + jobName + "/config.xml";

        String auth = jenkinsUsername + ":" + jenkinsToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.set("Authorization", "Basic " + encodedAuth);

        ClassPathResource resource = new ClassPathResource("jenkins/updated-config.xml");
        String configXml = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        HttpEntity<String> requestEntity = new HttpEntity<>(configXml, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                jenkinsUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode())
                .body("Updated job '" + jobName + "': " + response.getBody());
    }

    @DeleteMapping("/delete-job/{jobName}")
    public ResponseEntity<String> deleteJenkinsJob(@PathVariable String jobName) {
        String jenkinsUrl = url + "/job/" + jobName + "/doDelete";
        String username = jenkinsUsername;
        String apiToken = jenkinsToken;

        String auth = username + ":" + apiToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                jenkinsUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PostMapping("/build/{jobName}")
    public ResponseEntity<String> triggerBuild(@PathVariable String jobName) {
        String jenkinsUrl = url + "/job/" + jobName + "/build";
        String auth = jenkinsUsername + ":" + jenkinsToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                jenkinsUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode()).body("Triggered build for job: " + jobName);
    }

    @PostMapping("/create")
    public ResponseEntity<String> generateEc2Deploy(@RequestBody JobCreateDto dto) {

        String jenkinsUrl = url + "/createItem?name="+dto.getJobName();
        String username = jenkinsUsername;
        String apiToken = jenkinsToken;
        String auth = username + ":" + apiToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "xml", StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        // 4) config.xml 생성
        String configXml="fef";
        try {
            configXml = scriptTemplateService.generateConfig(dto, "pipeline");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(configXml, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                jenkinsUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}
