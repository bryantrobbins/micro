package com.btr3.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class BootMicroDemoApplication {

 	static void main(String[] args) {
        new SpringApplicationBuilder()
                .showBanner(false)
                .sources(BootMicroDemoApplication.class, "file:config/beans.groovy")
                .run(args);
	}
}
