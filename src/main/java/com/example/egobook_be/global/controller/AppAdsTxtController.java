package com.example.egobook_be.global.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppAdsTxtController {

    @GetMapping(value = "/app-ads.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> appAdsTxt() {
        return ResponseEntity.ok("google.com, pub-7304061200076771, DIRECT, f08c47fec0942fa0");
    }

    @GetMapping(value = "/google-site-verification", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> googleSiteVerification() {
        return ResponseEntity.ok("google-site-verification: Z9OYphMMuoGMhcYamomvVRdiRwxv-Fd9nyNEQQV8hDk");
    }
}