package com.diaw.loginpuc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller 
public class LoginController {
    
    @GetMapping("/login")
    public String getLogin(){
        return "login";
    }

    @GetMapping("/home")
    public String postLogin(){
        return "home";
    }

    @GetMapping("/register")
    public String getRegister(){
        return "register";
    }
}
