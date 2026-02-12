package com.medinote.medinotebackend.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PrivateController {

    @GetMapping("/private")
    public String privateEndpoint() {
        return "You are authenticated";
    }
}
