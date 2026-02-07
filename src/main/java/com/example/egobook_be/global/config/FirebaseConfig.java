package com.example.egobook_be.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials;

                // 1순위: 환경변수 경로
                String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                if (credPath != null && !credPath.isEmpty()) {
                    File credFile = new File(credPath);
                    if (credFile.exists()) {
                        credentials = GoogleCredentials.fromStream(new FileInputStream(credFile));
                        System.out.println("Firebase initialized from: " + credPath);
                    } else {
                        throw new IOException("Firebase 키 파일 없음: " + credPath);
                    }
                } else {
                    // 2순위: 홈 디렉토리
                    File homeFile = new File(System.getProperty("user.home") + "/firebase-service-account.json");
                    if (homeFile.exists()) {
                        credentials = GoogleCredentials.fromStream(new FileInputStream(homeFile));
                        System.out.println("Firebase from home: " + homeFile.getAbsolutePath());
                    } else {
                        // 3순위: classpath (로컬)
                        ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
                        credentials = GoogleCredentials.fromStream(resource.getInputStream());
                        System.out.println("Firebase from classpath");
                    }
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            System.err.println("Firebase 초기화 실패: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Firebase 초기화 실패", e);
        }
    }
}