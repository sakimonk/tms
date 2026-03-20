package com.test.tms.controller;

/**
 * @author max
 * @date 2026/3/20 22:36
 */

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("task")
public class TaskController {

    @GetMapping
    public String test() {
        return "hello world";
    }
}
