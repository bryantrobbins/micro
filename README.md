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
