package com.ajisegiri.projectloom;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("api")
public class UserController {
    private final UserRepository repository;
    @PostMapping("user")
    public ResponseEntity register(@RequestBody UserEntity userEntity){
        repository.save(userEntity);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
    @GetMapping("user")
    public ResponseEntity get() throws InterruptedException {
        var result=repository.findAll();
        doSomething();
        doSomething2();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void doSomething() throws InterruptedException {
        System.out.println("Print Thread name for doSomething "+ Thread.currentThread());
        Thread.sleep(Duration.ofSeconds(2));
    }
    private void doSomething2() {
        System.out.println("Print Thread name for doSomething2 "+ Thread.currentThread());
    }
}
