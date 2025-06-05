package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/jenkins")
public class JenkinsController {

    @Value("${jenkins.url}")
    private String url;

    @Value("${jenkins.username}")
    private String jenkinsUsername;

    @Value("${jenkins.token}")
    private String jenkinsToken;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/create-/{jobName}")
    public ResponseEntity<String> createJenkinsJob(@PathVariable String jobName) throws IOException {
        String jenkinsUrl = url + "/createItem?name="+jobName;
        String username = jenkinsUsername;
        String apiToken = jenkinsToken;

        String auth = username + ":" + apiToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.set("Authorization", "Basic " + encodedAuth);

        ClassPathResource resource = new ClassPathResource("jenkins/config.xml");
        String configXml = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        HttpEntity<String> requestEntity = new HttpEntity<>(configXml, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                jenkinsUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
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
}
