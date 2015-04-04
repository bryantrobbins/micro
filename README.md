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

## Accessing user details and roles

This took some trial-and-error and creative Google-ing, but both of these features are actually fairly straightforward with the
Default Spring LDAP implementation. I updated the HelloController to have two separate functions at different request
paths: /greet/any and /greet/dev. Both of these inject the UserDetails object for the currently logged in user into
the method as a parameter, utilizing the AuthenticationPrincipal object annotation:

```
@RequestMapping("/greet/any")
public Greeting sayHello(@AuthenticationPrincipal UserDetails customUser) {
    return new Greeting(counter.incrementAndGet(), String.format(template, customUser.getUsername(), "employee"));
}
```

I also updated the template to take a second argument, so that the template now reads:

```
private static final String template = "Hello, %s, you fantastic %s!";
```

Then I tried to apply a role to the /greet/dev endpoint, requiring the user to be in the "developers" LDAP role in order
to access:

```
@Secured("ROLE_DEVELOPERS")
@RequestMapping("/greet/dev")
public Greeting sayHelloDev(@AuthenticationPrincipal UserDetails customUser) {
    return new Greeting(counter.incrementAndGet(), String.format(template, customUser.getUsername(), "developer"));
}
```

I was surprised that this didn't work, because I had seen Secured used this way in several tutorials. My problem, as
far as I understand it, is that the Secured annotation is not enabled by default in Controllers. I enabled this with
an annotation in my WebSecurityConfig (the same class where other security details are configured) like this:

```
@Configuration
@EnableWebMvcSecurity
@EnableGlobalMethodSecurity(securedEnabled = true) // Enable Secured annotation
```

Finally, now I can call the two endpoints with various users ("ben" is a developer, "joe" is not):

```
bryans-mbp:micro bryan$ curl -u ben:benspassword localhost:8080/greet/any
{"id":2,"content":"Hello, ben, you fantastic employee!"}

bryans-mbp:micro bryan$ curl -u ben:benspassword localhost:8080/greet/dev
{"id":3,"content":"Hello, ben, you fantastic developer!"}

bryans-mbp:micro bryan$ curl -u joe:joespassword localhost:8080/greet/any
{"id":4,"content":"Hello, joe, you fantastic employee!"}

bryans-mbp:micro bryan$ curl -u joe:joespassword localhost:8080/greet/dev
{"timestamp":1428174667000,"status":403,"error":"Forbidden","exception":"org.springframework.security.access.AccessDeniedException","message":"Access is denied","path":"/greet/dev"}
```

Based on my AuthenticationConfiguration implementation in WebSecurityConfig, roles are being loaded from any "ou=groups" location
in LDAP (see groupSearchBase):

```
auth
	.ldapAuthentication()
	.userDnPatterns("uid={0},ou=people")
	.groupSearchBase("ou=groups")
	.contextSource()
	.ldif("classpath:test-server.ldif")
```

## Added Logging

This is a pretty simple one. I found out that Spring Boot defaults to Logback for logging
(http://docs.spring.io/spring-boot/docs/current/reference/html/howto-logging.html), and added some logging
to HelloController. I configured a base level for all com.btr3.demo classes to be "DEBUG" for now. When running,
I get a message like this in the logs:

```
2015-04-04 15:40:36.930  INFO 2286 --- [nio-8080-exec-3] c.btr3.demo.controllers.HelloController  : Replying with a generic greeting
```

## Added external properties and beans

This phase started off simple enough. I found 
out (http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)
that the Jar generated by Boot looks for application.properties in a number of
locations on the filesystem (relative to the current directory) and on the classpath. I experimented by setting the
com.btr3.demo logging level within an external application.properties file. I chose to use the "config/application.properties"
location on the filesystem for my external file, and moving my logging level setting from the Jar-included
application.properties to this file actually worked. (Note: Since I was previously using INFO logging, I downgraded
to DEBUG to ensure that the custom setting was being picked up).

Now came something a bit more interesting. In working with Grails, I really liked the idea of the BeanBuilder DSL. It
turns out that recent Spring versions include a port of the Groovy BeanBuilder DSL, so all I needed to do was to
tell my application to load a given set of bean definitions from a Groovy file. I did this by switching to the "fluent"
builder version for SpringApplication in my main method
(http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-spring-application.html), and specifying 
both my primary application class and the new Groovy file as Bean sources:

```
new SpringApplicationBuilder()
		.showBanner(false)
		.sources(BootMicroDemoApplication.class, "file:config/beans.groovy")
		.run(args);
```

My bean file currently looks like this, which is setting a new field "companyName" in my existing HelloController::

```
// Define beans here using the BeanBuilder DSL

import com.btr3.demo.controllers.HelloController

beans {
    helloController(HelloController) {
      companyName = "Happysoft"
    }
}
```

Now the greetings look like this:

```
bryans-mbp:micro bryan$ curl -u joe:joespassword localhost:8080/greet/any
{"id":1,"content":"Hello, joe, you fantastic employee at Happysoft!"}
```


