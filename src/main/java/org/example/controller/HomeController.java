package org.example.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;
@RestController
public class HomeController {

    @GetMapping("/home" )
    public String home() {
        return "Welcome Message";
    }

    @GetMapping("/userDetails")
    public String userDetails(@AuthenticationPrincipal(expression = "claims['name']") String name){
        return "logged in user details - " + name;
    }
    @RequestMapping(value = "/whoami")
    @ResponseBody
    public String whoami() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getPrincipal().toString();
        String roles = auth.getAuthorities().toString();
        return "user: " + username + System.lineSeparator() + ", roles: " + roles;
    }
}
