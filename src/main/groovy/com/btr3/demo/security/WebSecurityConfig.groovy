package com.btr3.demo.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity

@Configuration
@EnableWebMvcSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .anyRequest().fullyAuthenticated()
                .and()
                .httpBasic()
    }

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
}
