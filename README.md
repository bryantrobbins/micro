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

## Added External Properties and Beans

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

## Enabled SSL for Tomcat

Since this system is just using basic auth, I thought it would be good to investigate how complicated
it might be to enable HTTPS. It turns out that this too is pretty straightforward. I added a few lines
to the application.properties bundled with jar:

```
server.port=8443
server.ssl.key-store=file:config/keystore.jks
server.ssl.key-store-password=changeit
server.ssl.key-password=changeit
```

This also requires me to generate a Java Key Store (above, I've specified that it will live at
"config/keystore.jks" on the filesystem). This can be done on any system with Java installed,
using a tool called "keytool" from JAVA_HOME/bin. I ran the following command to generate a
keystore and self-signed certificate:

```
keytool -genkey -keyalg RSA -alias tomcat -keystore config/keystore.jks
```

With that in place, the app starts to serve content over HTTPS instead of HTTP. For now, I tell
cURL to ignore the errors its getting from self-signed certificates:

```
bryans-mbp:micro bryan$ curl -k -u joe:joespassword https://localhost:8443/greet/any
{"id":4,"content":"Hello, joe, you fantastic employee at Happysoft!"}
```

But, to prove to myself that the certificate is actually being applied, I decided to
export the cert from the newly generated app keystore and add it to the standard Java
cacerts location on my machine. This will allow me to write a simple ReST client script
for testing on my local machine. I first wrote a simple client on my local machine in Groovy
(this is client.groovy in the repo above):

```
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.2' )

import groovyx.net.http.RESTClient

def client = new RESTClient('https://localhost:8443/')
client.headers['Authorization'] = 'Basic '+"joe:joespassword".bytes.encodeBase64()

def result = client.get( path : 'greet/any' )
assert result.status == 200
println result.getData()
```

Before telling my Java installation about the certificate, I get errors:

```
bryans-mbp:micro bryan$ groovy client.groovy 
Caught: javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
	at org.apache.http.conn.ssl.AbstractVerifier.verify(AbstractVerifier.java:128)
	at org.apache.http.conn.ssl.SSLSocketFactory.connectSocket(SSLSocketFactory.java:572)
	at org.apache.http.impl.conn.DefaultClientConnectionOperator.openConnection(DefaultClientConnectionOperator.java:180)
	at org.apache.http.impl.conn.ManagedClientConnectionImpl.open(ManagedClientConnectionImpl.java:294)
	at org.apache.http.impl.client.DefaultRequestDirector.tryConnect(DefaultRequestDirector.java:640)
	at org.apache.http.impl.client.DefaultRequestDirector.execute(DefaultRequestDirector.java:479)
	at org.apache.http.impl.client.AbstractHttpClient.execute(AbstractHttpClient.java:906)
	at org.apache.http.impl.client.AbstractHttpClient.execute(AbstractHttpClient.java:1066)
	at org.apache.http.impl.client.AbstractHttpClient.execute(AbstractHttpClient.java:1044)
	at groovyx.net.http.HTTPBuilder.doRequest(HTTPBuilder.java:515)
	at groovyx.net.http.RESTClient.get(RESTClient.java:119)
	at groovyx.net.http.RESTClient$get.call(Unknown Source)
	at client.run(client.groovy:8)
```

So I exported the certificate and imported to the base Java location as planned:

```
keytool -export -alias tomcat -file localhost.crt -keystore config/keystore.jks
sudo keytool -import -trustcacerts -alias localhost -file localhost.crt -keystore $JAVA_HOME/jre/lib/security/cacerts
```

And all is well:

```
bryans-mbp:micro bryan$ groovy client.groovy 
[content:Hello, joe, you fantastic employee at Happysoft!, id:7]
```

## External LDAP Server and External LDAP-related Properties

I wanted to see how portable the LDAP implementation(s) were in the default application. Once again, things are
pretty configurable.

First, I downloaded and installed Apache Directory Server, an open-source LDAP server implementation. I also installed
Apache Directory Studio, which gives a point-and-click UI for manipulating server settings and LDAP entries. The schema
checking in ApacheDS was much more strict than the embedded LDAP of Spring, so I had to make some small modifications to
the LDIF and re-import (this was all by iterative trial-and-error).

Eventually, I had my LDAP server running on localhost port 10389 (unsecured).

When it came to external properties, I was a little disappointed that I couldn't figure out BeanBuilder
configurations for LDAP properties. I settled for using the Value annotation instead, so that I could
add these properties to my external (config/) application.properties:

```
url=ldap://localhost:10389
users=uid={0},ou=people,dc=springframework,dc=org
groups =ou=groups,dc=springframework,dc=org
groupMember=member={0}
```

These require corresponding annotations in WebSecurityConfig:

```
@Configuration
protected static class AuthenticationConfiguration extends
        GlobalAuthenticationConfigurerAdapter {

    @Value('${users}')
    String userPattern

    @Value('${groups}')
    String groupBase

    @Value('${groupMember}')
    String groupFilter

    @Value('${url}')
    String url

    @Override
    public void init(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .ldapAuthentication()
                .userDnPatterns(userPattern)
                .groupSearchBase(groupBase)
                .groupSearchFilter(groupFilter)
                .contextSource().url(url)
    }
}
```

