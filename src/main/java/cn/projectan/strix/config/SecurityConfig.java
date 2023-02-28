package cn.projectan.strix.config;

import cn.projectan.strix.core.ss.error.AccessDeniedHandlerImpl;
import cn.projectan.strix.core.ss.error.AuthenticationEntryPointImpl;
import cn.projectan.strix.core.ss.filter.SystemManagerAuthenticationTokenFilter;
import cn.projectan.strix.core.ss.filter.SystemUserAuthenticationTokenFilter;
import cn.projectan.strix.core.ss.provider.SystemManagerAuthenticationProvider;
import cn.projectan.strix.core.ss.provider.SystemUserAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
                                                   AuthenticationEntryPointImpl authenticationEntryPoint) throws Exception {
        http
                .csrf().disable()
                .formLogin().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // 管理端策略
                .antMatchers("/captcha/get").anonymous()
                .antMatchers("/captcha/check").anonymous()
                .antMatchers("/system/login").anonymous()
                .antMatchers("/system/logout").anonymous()
                .antMatchers("/system/**").hasRole("SYSTEM_MANAGER")
                // 用户端策略
                .antMatchers("/wechat/*/jump/*").anonymous()
                .antMatchers("/wechat/*/auth").anonymous()
                .antMatchers("/wechat/*/config").anonymous()
                .antMatchers("/wechat/*/giveMeSessionTokenOnDevMode").anonymous()
                .antMatchers("/wechat/file/get/**").anonymous()
                .antMatchers("/wechat/*/file/get/**").anonymous()
                .antMatchers("/wechat/**").hasRole("SYSTEM_USER")
                // 除上面外的所有请求全部需要鉴权认证
                .anyRequest().authenticated();

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
