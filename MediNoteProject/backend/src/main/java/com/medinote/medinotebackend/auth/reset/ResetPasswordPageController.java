package com.medinote.medinotebackend.auth.reset;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ResetPasswordPageController {

    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        return "reset-password";
    }
}
