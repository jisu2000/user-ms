package com.hit.userms.controller;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.GetMapping;import org.springframework.web.bind.annotation.RequestMapping;import org.springframework.web.bind.annotation.RestController;@RestController@RequestMapping("/api/v1/secure")public class SecuredController {    @GetMapping("/test")    private String getMsg(){        return "Fine";    }}