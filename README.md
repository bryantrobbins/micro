I'm playing around with microservices.

# Why

I recently developed an application in Grails. It works nicely, but is bloated in terms of dependencies, magic, and other
aspects. While I liked the "quick CRUD" aspects of Grails, I would like to explore whether its feasible to decouple some
of the baked-in controllers and  services from a Grails app to their own microservices which are callable from Grails and
elsewhere.

Instead of ripping my application apart in advance, I'd like to experiment with some of the features of Spring Boot,
which I've seen touted as a good framework for development of microservices. This starter project is an attempt to do that.

# Wish list

This is the kind of microservice I'd like to develop:

* Groovy code
* ReST API
* JSON formats
* Built-in serialization from JSON to Groovy
* LDAP-backed authentication
* Callable from Angular
* JAR-runnable (embedded servlet containers) and RPM-installable
* Gradle buildable
* (Optional) CRUD with Persistence (Optional because Grails is pretty straightforward for this, except for that GORM weirdness)

# Implementation Log

## Starter Project

I generated a Starter Project at http://start.spring.io with Web and JAX-RS.

Built and ran that sucker like this:

```
gradle build
java -jar build/libs/boot-micro-demo-0.0.1-SNAPSHOT.jar
```

## Added Security

I enabled Basic Auth security by adding a dependency:

```
compile("org.springframework.boot:spring-boot-starter-security")
```

With this on, you have to authenticate every call with the username "user"
and a password which is generated each time the Jar is run. The password
is printed to the console by default. You can use these from curl like
this:

```
curl -u user:some-big-long-string localhost:8080/someService
```

Of course, I don't have a service yet, so maybe this was a bit premature!

## Added Actuator

Actuator adds a number of health check and other management services as endpoints.

Just add the dependency like this:

```
compile("org.springframework.boot:spring-boot-starter-actuator")
```

Now I can do this:

```
curl localhost:8080/health
```

## Added a HelloController

I walked through the Getting Started guide here (https://spring.io/guides/gs/spring-boot/) and
added a simple service which returns a hard-coded String. Because security was enabled, I had
to call it like this (of course your password string will be different - look for it in the console
output of your java -jar command):

```
curl -u user:a602fe33-7ca4-4744-9b09-6c8393a90834 localhost:8080/hello-world
```

And it works!

## Added a JSON Greeting method to HelloController

I walked through a Getting Started guide that was labeled as a guide to Actuator but included a JSON controller example
as well, found here (https://spring.io/guides/gs/actuator-service/). This meant creating a Groovy class called Greeting
that the service would reply with, in JSON format. I added a method sayHello (seen in the tutorial) to my HelloController
and mapped it to the endpoint "greet", so that I can call it like this:

```
curl -u user:ab6a3bfa-6e51-4c4c-a0ff-6ed0787f7faa localhost:8080/greet
```

## Added LDAP Security

I walked through yet another tutorial to enable LDAP authentication within Spring Security, found here
(https://spring.io/guides/gs/authenticating-ldap/). There was a bit more code to write (or copy from an
example) to override default security configurations. The example is also using an embedded LDAP server,
which I guess is good for quick testing, but is not that realistic going forward.

With an LDIP file in place with user attributes (including usernames and hashed passwords), I can run
my greeting command like this now for user Ben:

```
curl -u ben:benspassword localhost:8080/greet #Uses default value of "Stranger" in greeting
curl -u ben:benspassword localhost:8080/greet?name=Ben # Uses provided name of "Ben"
```

What I'd like to do next is to stand up a separate LDAP server and authenticate against that. Hopefully
I can also figure out how to externalize the configuration of that server
(URL, managerDn, and managerPassword).


