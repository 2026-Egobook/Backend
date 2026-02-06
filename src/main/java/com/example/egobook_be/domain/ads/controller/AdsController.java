package com.example.egobook_be.domain.ads.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdsController implements AdsControllerDocs{

    @Override
    public ResponseEntity<Void> callback(){
        return ResponseEntity.ok().build();
    }


}
