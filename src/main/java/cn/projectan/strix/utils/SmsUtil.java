package cn.projectan.strix.utils;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.config.AliyunSmsConfig;
import cn.projectan.strix.model.db.SystemSmsLog;
import cn.projectan.strix.service.SystemSmsLogService;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 短信发送工具
 *
 * @author 安炯奕
 * @date 2021/8/30 19:29
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "strix", name = "use-sms-aliyun", havingValue = "true")
public class SmsUtil {

    @Autowired
    private SystemSmsLogService systemSmsLogService;
    @Autowired
    private AliyunSmsConfig aliyunSmsConfig;

    public void send(SystemSmsLog log) {
        IAcsClient client = aliyunSmsConfig.getInstance(log.getSmsConfigId());

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", "cn-hangzhou");
        request.putQueryParameter("PhoneNumbers", log.getPhoneNumber());
        request.putQueryParameter("SignName", log.getSmsSignName());
        request.putQueryParameter("TemplateCode", log.getSmsTemplateCode());
        request.putQueryParameter("TemplateParam", log.getSmsTemplateParam());
        try {
            CommonResponse response = client.getCommonResponse(request);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> resultMap = objectMapper.readValue(response.getData(), new TypeReference<>() {
            });

            String resultCode = MapUtil.getStr(resultMap, "Code");
            if (resultCode != null) {
                if ("OK".equalsIgnoreCase(resultCode)) {
                    log.setSmsSendStatus(1);
                } else {
                    log.setSmsSendStatus(2);
                }
            }
            log.setSmsPlatformResponse(response.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }
        systemSmsLogService.save(log);
    }

}
