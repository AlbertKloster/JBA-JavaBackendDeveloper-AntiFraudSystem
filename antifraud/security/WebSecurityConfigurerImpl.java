package antifraud.security;

import antifraud.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableWebSecurity
public class WebSecurityConfigurerImpl extends WebSecurityConfigurerAdapter {

    private final UserDetailsService service;

    @Autowired
    public WebSecurityConfigurerImpl(UserDetailsService service) {
        this.service = service;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(service).passwordEncoder(getEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.httpBasic()
                .and()
                .csrf().disable().headers().frameOptions().disable()
                .and()
                .authorizeRequests()
                .antMatchers("/api/auth/user").permitAll()
                .antMatchers("/actuator/shutdown").permitAll() // needs to run test
                .mvcMatchers("/api/auth/user/*").hasAnyRole(Role.ADMINISTRATOR.name())
                .mvcMatchers("/api/auth/access").hasAnyRole(Role.ADMINISTRATOR.name())
                .mvcMatchers("/api/auth/role").hasAnyRole(Role.ADMINISTRATOR.name())
                .mvcMatchers("/api/auth/list").hasAnyRole(Role.ADMINISTRATOR.name(), Role.SUPPORT.name())
                .mvcMatchers(HttpMethod.POST, "/api/antifraud/transaction").hasAnyRole(Role.MERCHANT.name())
                .mvcMatchers(HttpMethod.PUT, "/api/antifraud/transaction").hasAnyRole(Role.SUPPORT.name())
                .mvcMatchers( "/api/antifraud/suspicious-ip").hasAnyRole(Role.SUPPORT.name())
                .mvcMatchers( "/api/antifraud/suspicious-ip/*").hasAnyRole(Role.SUPPORT.name())
                .mvcMatchers( "/api/antifraud/stolencard").hasAnyRole(Role.SUPPORT.name())
                .mvcMatchers( "/api/antifraud/history").hasAnyRole(Role.SUPPORT.name())
                .mvcMatchers( "/api/antifraud/history/*").hasAnyRole(Role.SUPPORT.name())
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Bean
    public PasswordEncoder getEncoder() {
        return new BCryptPasswordEncoder();
    }
}
