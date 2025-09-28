package com.example.redpandaapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // /redpandas にリダイレクトする
        return "redirect:/redpandas";
    }
}
