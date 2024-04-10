package cn.projectan.strix.config;

/**
 * Undertow 配置
 *
 * @author ProjectAn
 * @date 2023/11/26 14:03
 */
//@Configuration
//public class UndertowConfig {
//
//    @Component
//    public static class UndertowWebServerFactoryCustomizer implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {
//        @Override
//        public void customize(UndertowServletWebServerFactory factory) {
//            WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
//            webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(true, 1024));
//            factory.addBuilderCustomizers(builder -> builder.setServerOption(io.undertow.UndertowOptions.ENABLE_HTTP2, true));
//            factory.addDeploymentInfoCustomizers(deploymentInfo -> deploymentInfo.addServletContextAttribute("io.undertow.websockets.jsr.WebSocketDeploymentInfo", webSocketDeploymentInfo));
//        }
//    }
//
//}
