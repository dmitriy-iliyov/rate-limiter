package io.github.dmitriyiliyov.ratelimiter.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-rate-limiter")
public class TestController {

    @GetMapping("/guarded")
    public String guardedCall() {
        return "Accessed to guarded endpoint";
    }

    @GetMapping("/non-guarded")
    public String nonGuardedCall() {
        return "Accessed to non-guarded endpoint";
    }
}
