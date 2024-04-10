package cn.projectan.strix.core.module.sms;

import cn.projectan.strix.model.db.SmsLog;
import cn.projectan.strix.model.dict.StrixSmsLogStatus;
import cn.projectan.strix.model.dict.StrixSmsSignStatus;
import cn.projectan.strix.model.dict.StrixSmsTemplateStatus;
import cn.projectan.strix.model.dict.StrixSmsTemplateType;
import cn.projectan.strix.model.other.module.sms.StrixSmsSign;
import cn.projectan.strix.model.other.module.sms.StrixSmsTemplate;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author ProjectAn
 * @date 2023/5/20 16:34
 */
@Slf4j
public class AliyunSmsClient extends StrixSmsClient {

    protected IAcsClient client;

    public AliyunSmsClient(IAcsClient client) {
        super();
        this.client = client;
    }

    @Override
    public IAcsClient get() {
        return client;
    }

    @Override
    public void send(SmsLog sms) {
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumbers(sms.getPhoneNumber());
        request.setSignName(sms.getSignName());
        request.setTemplateCode(sms.getTemplateCode());
        request.setTemplateParam(sms.getTemplateParam());

        try {
            SendSmsResponse response = client.getAcsResponse(request);

            sms.setStatus("OK".equalsIgnoreCase(response.getCode()) ? StrixSmsLogStatus.SUCCESS : StrixSmsLogStatus.FAIL);
            sms.setPlatformResponse(response.getMessage());
        } catch (Exception e) {
            log.error("Strix SMS: 发送短信失败. (发送短信时发生异常)", e);
            sms.setStatus(StrixSmsLogStatus.FAIL);
            sms.setPlatformResponse(e.getMessage());
        }
    }

    private final static Map<String, Integer> SIGN_STATUS_MAP = Map.of(
            "AUDIT_STATE_INIT", StrixSmsSignStatus.INIT,
            "AUDIT_STATE_PASS", StrixSmsSignStatus.PASS,
            "AUDIT_STATE_NOT_PASS", StrixSmsSignStatus.NOT_PASS,
            "AUDIT_STATE_CANCEL", StrixSmsSignStatus.CANCEL
    );

    @Override
    public List<StrixSmsSign> getSignList() {
        List<QuerySmsSignListResponse.QuerySmsSignDTO> signListPrivate = getSignListPrivate(1);

        return Optional.ofNullable(signListPrivate).orElse(Collections.emptyList()).stream().map(s ->
                new StrixSmsSign(
                        s.getSignName(),
                        SIGN_STATUS_MAP.get(s.getAuditStatus()),
                        LocalDateTime.parse(s.getCreateDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )).toList();
    }

    private final static Map<String, Integer> TEMPLATE_STATUS_MAP = Map.of(
            "AUDIT_STATE_INIT", StrixSmsTemplateStatus.INIT,
            "AUDIT_STATE_PASS", StrixSmsTemplateStatus.PASS,
            "AUDIT_STATE_NOT_PASS", StrixSmsTemplateStatus.NOT_PASS,
            "AUDIT_STATE_CANCEL", StrixSmsTemplateStatus.CANCEL,
            "AUDIT_SATE_CANCEL", StrixSmsTemplateStatus.CANCEL
    );
    private final static Map<Integer, Integer> TEMPLATE_TYPE_MAP = Map.of(
            2, StrixSmsTemplateType.VERIFICATION_CODE,
            0, StrixSmsTemplateType.NOTIFICATION,
            1, StrixSmsTemplateType.MARKETING,
            6, StrixSmsTemplateType.INTERNATIONAL,
            7, StrixSmsTemplateType.DIGITAL
    );

    @Override
    public List<StrixSmsTemplate> getTemplateList() {
        List<QuerySmsTemplateListResponse.SmsStatsResultDTO> templateListPrivate = getTemplateListPrivate(1);

        return Optional.ofNullable(templateListPrivate).orElse(Collections.emptyList()).stream().map(t ->
                new StrixSmsTemplate(
                        t.getTemplateCode(),
                        t.getTemplateName(),
                        TEMPLATE_TYPE_MAP.get(t.getTemplateType()),
                        TEMPLATE_STATUS_MAP.get(t.getAuditStatus()),
                        t.getTemplateContent(),
                        LocalDateTime.parse(t.getCreateDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )).toList();
    }

    @Override
    public void close() {
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }

    private List<QuerySmsSignListResponse.QuerySmsSignDTO> getSignListPrivate(int index) {
        QuerySmsSignListRequest request = new QuerySmsSignListRequest();
        request.setPageSize(50);
        request.setPageIndex(index);

        try {
            QuerySmsSignListResponse response = client.getAcsResponse(request);

            List<QuerySmsSignListResponse.QuerySmsSignDTO> signList = response.getSmsSignList();

            if (!CollectionUtils.isEmpty(signList)) {
                signList.addAll(Optional.ofNullable(getSignListPrivate(index + 1)).orElse(Collections.emptyList()));
                return signList;
            }
            return signList;
        } catch (Exception e) {
            log.error("Strix SMS: 获取签名列表失败. (获取签名列表时发生异常)", e);
            return null;
        }
    }

    private List<QuerySmsTemplateListResponse.SmsStatsResultDTO> getTemplateListPrivate(int index) {
        QuerySmsTemplateListRequest request = new QuerySmsTemplateListRequest();
        request.setPageSize(50);
        request.setPageIndex(index);

        try {
            QuerySmsTemplateListResponse response = client.getAcsResponse(request);

            List<QuerySmsTemplateListResponse.SmsStatsResultDTO> templateList = response.getSmsTemplateList();

            if (!CollectionUtils.isEmpty(templateList)) {
                templateList.addAll(Optional.ofNullable(getTemplateListPrivate(index + 1)).orElse(Collections.emptyList()));
                return templateList;
            }
            return templateList;
        } catch (Exception e) {
            log.error("Strix SMS: 获取签名列表失败. (获取签名列表时发生异常)", e);
            return null;
        }
    }


}
