package cn.projectan.strix.config;

import cn.projectan.strix.core.ss.error.AccessDeniedHandlerImpl;
import cn.projectan.strix.core.ss.error.AuthenticationEntryPointImpl;
import cn.projectan.strix.core.ss.filter.SystemManagerAuthenticationTokenFilter;
import cn.projectan.strix.core.ss.filter.SystemUserAuthenticationTokenFilter;
import cn.projectan.strix.core.ss.provider.SystemManagerAuthenticationProvider;
import cn.projectan.strix.core.ss.provider.SystemUserAuthenticationProvider;
import cn.projectan.strix.initialize.AnonymousUrlInit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author 安炯奕
 * @date 2023/2/24 23:17
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SystemManagerAuthenticationTokenFilter systemManagerAuthenticationTokenFilter,
                                                   SystemUserAuthenticationTokenFilter systemUserAuthenticationTokenFilter,
                                                   SystemManagerAuthenticationProvider systemManagerAuthenticationProvider,
                                                   SystemUserAuthenticationProvider systemUserAuthenticationProvider,
                                                   AccessDeniedHandlerImpl accessDeniedHandler,
                                                   AuthenticationEntryPointImpl authenticationEntryPoint,
                                                   AnonymousUrlInit anonymousUrlInit) throws Exception {

        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry = http.authorizeRequests();
        anonymousUrlInit.getUrls().forEach(url -> registry.antMatchers(url).permitAll());

        http
                .csrf().disable()
                .formLogin().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/system/**").hasRole("SYSTEM_MANAGER")
                .antMatchers("/wechat/**").hasRole("SYSTEM_USER")
                // 所有请求全部需要鉴权认证
                .anyRequest().authenticated();

        // 禁止自动跳转登录页面
        http.httpBasic().disable();

        http.addFilterBefore(systemManagerAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(systemUserAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        http.authenticationProvider(systemManagerAuthenticationProvider);
        http.authenticationProvider(systemUserAuthenticationProvider);

        // 添加认证/权限异常处理
        http.exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint);

        // 允许跨域
        http.cors();

        return http.build();
    }

}
