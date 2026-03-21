package cn.projectan.strix.config;

import cn.projectan.strix.core.ss.error.AccessDeniedHandlerImpl;
import cn.projectan.strix.core.ss.error.AuthenticationEntryPointImpl;
import cn.projectan.strix.core.ss.filter.SystemManagerAuthenticationTokenFilter;
import cn.projectan.strix.core.ss.filter.SystemUserAuthenticationTokenFilter;
import cn.projectan.strix.core.ss.handler.SystemManagerLogoutSuccessHandler;
import cn.projectan.strix.initializer.system.StrixSecurityRuleInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/**
 * Spring Security 配置类
 *
 * @author ProjectAn
 * @since 2023/2/24 23:17
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public RequestAttributeSecurityContextRepository getRequestAttributeSecurityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SystemManagerAuthenticationTokenFilter systemManagerAuthenticationTokenFilter,
                                                   SystemUserAuthenticationTokenFilter systemUserAuthenticationTokenFilter,
                                                   AccessDeniedHandlerImpl accessDeniedHandler,
                                                   AuthenticationEntryPointImpl authenticationEntryPoint,
                                                   StrixSecurityRuleInitializer strixSecurityRuleInitializer,
                                                   SystemManagerLogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用内置登录
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // 安全响应头
                .headers((headers) -> headers
                        // 禁止页面被嵌入 iframe（防点击劫持）
                        .frameOptions(frame -> frame.deny())
                        // 禁止 MIME 类型嗅探
                        .contentTypeOptions(Customizer.withDefaults())
                        // 启用 XSS 过滤
                        .xssProtection(xss -> xss.headerValue(
                                org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        // 控制 Referrer 信息泄露
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Content-Security-Policy
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; frame-ancestors 'none'"))
                        // 缓存控制
                        .cacheControl(Customizer.withDefaults())
                )
                // 无状态会话
                .sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorizeRequests) -> {
                    // 配置匿名访问的URL
                    strixSecurityRuleInitializer.getAnonymousUrlList().forEach(url -> authorizeRequests.requestMatchers(url).permitAll());
                    // WebSocket 端点允许匿名访问（在握手拦截器中做认证）
                    authorizeRequests.requestMatchers("/ws/**").permitAll();
                    // 配置需要指定角色才可访问的URL
                    strixSecurityRuleInitializer.getUrlRoleMap().forEach((url, role) -> authorizeRequests.requestMatchers(url).hasRole(role));
                    strixSecurityRuleInitializer.getUrlAnyRoleMap().forEach((url, role) -> authorizeRequests.requestMatchers(url).hasAnyRole(role.split(",")));
                    // 其他所有请求全部需要鉴权认证
                    authorizeRequests.anyRequest().authenticated();
                })
                .addFilterBefore(systemManagerAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(systemUserAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
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
