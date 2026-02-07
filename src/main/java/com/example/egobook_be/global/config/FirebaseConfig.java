package com.example.egobook_be.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            System.out.println("GOOGLE_APPLICATION_CREDENTIALS = "
                    + System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));

            // Firebase 초기화
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized successfully");
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Firebase 초기화 실패 - GOOGLE_APPLICATION_CREDENTIALS 환경변수를 확인하세요.",
                    e
            );
        }
    }
}