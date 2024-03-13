package cn.projectan.strix.config;

import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Undertow 配置
 *
 * @author ProjectAn
 * @date 2023/11/26 14:03
 */
@Configuration
public class UndertowConfig {

    @Component
    public static class UndertowWebServerFactoryCustomizer implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {
        @Override
        public void customize(UndertowServletWebServerFactory factory) {
            WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
            webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(true, 1024));
            factory.addBuilderCustomizers(builder -> builder.setServerOption(io.undertow.UndertowOptions.ENABLE_HTTP2, true));
            factory.addDeploymentInfoCustomizers(deploymentInfo -> deploymentInfo.addServletContextAttribute("io.undertow.websockets.jsr.WebSocketDeploymentInfo", webSocketDeploymentInfo));
        }
    }

}
