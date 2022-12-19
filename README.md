# Project Loom using Spring Boot with Postgres
Virtual threads (JEP-425) are lightweight JVM-managed threads that facilitate the development of concurrent applications with high throughput in Java.

## Table of contents
* Platform Threads
* Virtual Threads
* Platform Threads vs. Virtual Threads
* Implementation and Performance Evaluation of Platform and Virtual Threads

## Platform Threads
In Java, a platform thread is a thread that is managed by the underlying operating system. These threads are created and managed using the Thread class in the Java standard library, and they correspond to a native thread in the operating system. Platform threads can be used to perform a variety of tasks, including running concurrent tasks and parallelizing work.
<br/>
Platform threads have always been simple to model, develop, and debug because they use the platform's unit of concurrency to represent the application's unit of concurrency. It is known as the thread-per-request pattern.

However, this pattern restricts the server's throughput because the number of concurrent requests (that server can handle) becomes directly proportional to the server's hardware performance. Thus, even in multi-core CPUs, the number of usable threads must be constrained.
<br/>
Latency is also a big issue for example in a microservices world where a service needs to communicate with another service synchronously, the platform thread running on the server is idle while the application waits for data from other servers. Then this is why reactive programming (Rx) was created.
Rx came to the rescue and solved the issue of platform threads waiting for responses from other systems.
<br/>
Rx uses asynchronous APIs (in which they do not wait for responses), the issue now is that it is very challenging to profile or debug async programs because they run in different threads.
In addition Rx uses functional programming model which differs from typical loops and conditional statements, which makes hard to understand existing code and also writing new ones becuase we have to chain multiple functions/services which also obviously does not help in code readability.

## Virtual Threads
virtual threads are a new feature that allows developers to create and manage a large number of lightweight threads that can be scheduled and run by the Java Virtual Machine (JVM). Virtual threads are designed to be used for tasks that do not require a lot of computing resources, such as tasks that are waiting for external events or performing I/O operations.

Without relying on the number of platform threads, we can create millions of virtual threads in an application. Since these virtual threads are controlled by the JVM and are stored in RAM as regular Java objects, they do not increase the overhead associated with context switching.
Similar to conventional threads, the application's code runs in a virtual thread for the entire time a request is made , but the virtual thread only uses an OS thread when the calculations are made on the CPU(i.e cpu bound tasks). While they are waiting or sleeping, they do not obstruct the OS thread.

With the same hardware setup, virtual threads assist in achieving the same high scaling and throughput as asynchronous APIs without increasing syntax complexity.

## Platform Threads vs. Virtual Threads
* Resource consumption: Virtual threads are designed to be lightweight and require fewer resources compared to platform threads. This makes it possible to create and manage a larger number of virtual threads without encountering performance issues.

* Scheduling: Virtual threads are managed by the JVM and are scheduled by the JVM's thread scheduler, while platform threads are managed by the operating system and are scheduled by the operating system's thread scheduler. This means that the behavior of virtual threads may be different from platform threads depending on the implementation of the JVM and the operating system.

* Compatibility: Virtual threads are a new feature and are not available in older versions of Java. They can only be used in Java 19 and later. Platform threads, on the other hand, are a longstanding feature of the Java standard library and are available in all versions of Java.

## Implementation and Performance Evaluation of Platform and Virtual Threads
To implement this we need java 19 with --enable-preview . We also need to configure spring boot with tomcat:
```
@Bean
AsyncTaskExecutor applicationTaskExecutor() {
       ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    return new TaskExecutorAdapter(executorService::execute);
}

@Bean
TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
    return protocolHandler -> {
        protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    };
}
```
Thread count of tomcat server is set to 1. That means our server can accept only one request at same time because of single thread.
```server.tomcat.threads.max=1```
<br/>
To actually implement this we establish a connection with postgres db and get some data, and aslo use the thread sleep mechanism. 2 secs is a long time, but we can more clearly see the advantages of virtual threads.
```
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
```
The next thing we need is a server load. To create server load, we can use Apache JMeter,due to the single thread on our server, responses cannot be created until the previous request has been fulfilled.
When virtual threads are not configured, the server can't handle concurrent requests since we set ```server.tomcat.threads.max=1```. 

Without virtual thread with a load of 500req/sec took 506.67 secs
<br/>
With virtual thread with a load of 500req/sec took 5.906 secs. The difference is clear. 
