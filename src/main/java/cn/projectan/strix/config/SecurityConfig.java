package cn.projectan.strix.config;

import cn.projectan.strix.core.ss.error.AccessDeniedHandlerImpl;
import cn.projectan.strix.core.ss.error.AuthenticationEntryPointImpl;
import cn.projectan.strix.core.ss.filter.SystemManagerAuthenticationTokenFilter;
import cn.projectan.strix.core.ss.filter.SystemUserAuthenticationTokenFilter;
import cn.projectan.strix.core.ss.handler.LogoutSuccessHandlerImpl;
import cn.projectan.strix.core.ss.provider.SystemManagerAuthenticationProvider;
import cn.projectan.strix.core.ss.provider.SystemUserAuthenticationProvider;
import cn.projectan.strix.initialize.AnonymousUrlInit;
import cn.projectan.strix.initialize.RoleUrlInit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @SuppressWarnings("deprecation")
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
                                                   AnonymousUrlInit anonymousUrlInit,
                                                   RoleUrlInit roleUrlInit,
                                                   LogoutSuccessHandlerImpl logoutSuccessHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // 禁止自动跳转登录页面
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorizeRequests) -> {
                    // 加载匿名访问的URL (form DB & annotation)
                    anonymousUrlInit.getUrls().forEach(url -> authorizeRequests.requestMatchers(url).permitAll());
                    // 加载访问URL需要的角色 (form DB)
                    roleUrlInit.getUrlRoleMap().forEach((url, role) -> authorizeRequests.requestMatchers(url).hasRole(role));
                    roleUrlInit.getUrlAnyRoleMap().forEach((url, role) -> authorizeRequests.requestMatchers(url).hasAnyRole(role.split(",")));
                    // 所有请求全部需要鉴权认证
                    authorizeRequests.anyRequest().authenticated();
                })
                .addFilterBefore(systemManagerAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(systemUserAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(systemManagerAuthenticationProvider)
                .authenticationProvider(systemUserAuthenticationProvider)
                .exceptionHandling((exceptionHandling) -> exceptionHandling
                        // 添加认证/权限异常处理
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint))
                // 允许跨域
                .cors(Customizer.withDefaults())
                .logout((logout) -> logout
                        // 登出URL
                        .logoutUrl("/system/logout")
                        // 登出成功处理
                        .logoutSuccessHandler(logoutSuccessHandler));

        return http.build();
    }

}
