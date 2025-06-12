package com.example.demo;

import lombok.Data;

@Data
public class JobCreateDto {

    private String jobName;
    private String description;
    private String url;
    private Boolean githubtrigger;
}
