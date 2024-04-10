package cn.projectan.strix.config;

import cn.projectan.strix.core.ss.error.AccessDeniedHandlerImpl;
import cn.projectan.strix.core.ss.error.AuthenticationEntryPointImpl;
import cn.projectan.strix.core.ss.filter.SystemManagerAuthenticationTokenFilter;
import cn.projectan.strix.core.ss.filter.SystemUserAuthenticationTokenFilter;
import cn.projectan.strix.core.ss.handler.SystemManagerLogoutSuccessHandler;
import cn.projectan.strix.core.ss.provider.SystemManagerAuthenticationProvider;
import cn.projectan.strix.core.ss.provider.SystemUserAuthenticationProvider;
import cn.projectan.strix.initialize.SecurityRuleInit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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
 * Spring Security 配置类
 *
 * @author ProjectAn
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SystemManagerAuthenticationProvider systemManagerAuthenticationProvider,
                                                   SystemUserAuthenticationProvider systemUserAuthenticationProvider,
                                                   SystemManagerAuthenticationTokenFilter systemManagerAuthenticationTokenFilter,
                                                   SystemUserAuthenticationTokenFilter systemUserAuthenticationTokenFilter,
                                                   AccessDeniedHandlerImpl accessDeniedHandler,
                                                   AuthenticationEntryPointImpl authenticationEntryPoint,
                                                   SecurityRuleInit securityRuleInit,
                                                   SystemManagerLogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用内置登录
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
//                .formLogin((formLogin) -> formLogin
//                        .loginProcessingUrl("/system/login")
//                        .usernameParameter("username")
//                        .passwordParameter("password")
//                        .successHandler(loginSuccessHandler)
//                        .failureHandler(loginFailureHandler)
//                )
                // 无状态会话
                .sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorizeRequests) -> {
                    // 加载匿名访问的URL (form DB & annotation)
                    securityRuleInit.getAnonymousUrlList().forEach(url -> authorizeRequests.requestMatchers(url).permitAll());
                    // 加载访问URL需要的角色 (form DB)
                    securityRuleInit.getUrlRoleMap().forEach((url, role) -> authorizeRequests.requestMatchers(url).hasRole(role));
                    securityRuleInit.getUrlAnyRoleMap().forEach((url, role) -> authorizeRequests.requestMatchers(url).hasAnyRole(role.split(",")));
                    // 所有请求全部需要鉴权认证
                    authorizeRequests.anyRequest().authenticated();
                })
                .addFilterBefore(systemManagerAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(systemUserAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(systemManagerAuthenticationProvider)
                .authenticationProvider(systemUserAuthenticationProvider)
                .exceptionHandling((exceptionHandling) -> exceptionHandling
                        // 异常处理
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint))
                .logout((logout) -> logout
                        // 登出URL
                        .logoutUrl("/system/logout")
                        // 登出成功处理
                        .logoutSuccessHandler(logoutSuccessHandler));

        return http.build();
    }

}
