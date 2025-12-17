package cn.projectan.strix.core.module.sms;

import cn.projectan.strix.model.db.SmsLog;
import cn.projectan.strix.model.dict.StrixSmsLogStatus;
import cn.projectan.strix.model.dict.StrixSmsSignStatus;
import cn.projectan.strix.model.dict.StrixSmsTemplateStatus;
import cn.projectan.strix.model.dict.StrixSmsTemplateType;
import cn.projectan.strix.model.other.module.sms.StrixSmsSign;
import cn.projectan.strix.model.other.module.sms.StrixSmsTemplate;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.*;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 阿里云 SMS 客户端
 *
 * @author ProjectAn
 * @since 2023/5/20 16:34
 */
@Slf4j
public class AliyunSmsClient extends StrixSmsClient {

    protected Client client;

    public AliyunSmsClient(Client client) {
        super();
        this.client = client;
    }

    @Override
    public Client get() {
        return client;
    }

    @Override
    public void send(SmsLog sms) {
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(sms.getPhoneNumber())
                .setSignName(sms.getSignName())
                .setTemplateCode(sms.getTemplateCode())
                .setTemplateParam(sms.getTemplateParam());

        try {
            RuntimeOptions runtime = new RuntimeOptions();
            SendSmsResponse response = client.sendSmsWithOptions(request, runtime);
            SendSmsResponseBody body = response.getBody();

            sms.setStatus("OK".equalsIgnoreCase(body.getCode()) ? StrixSmsLogStatus.SUCCESS : StrixSmsLogStatus.FAIL);
            sms.setPlatformResponse(body.getMessage());
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
        List<QuerySmsSignListResponseBody.QuerySmsSignListResponseBodySmsSignList> signList = getSignListPrivate(1);

        return Optional.ofNullable(signList).orElse(Collections.emptyList()).stream().map(s ->
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
        List<QuerySmsTemplateListResponseBody.QuerySmsTemplateListResponseBodySmsTemplateList> templateList = getTemplateListPrivate(1);

        return Optional.ofNullable(templateList).orElse(Collections.emptyList()).stream().map(t ->
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
    }

    private List<QuerySmsSignListResponseBody.QuerySmsSignListResponseBodySmsSignList> getSignListPrivate(int index) {
        QuerySmsSignListRequest request = new QuerySmsSignListRequest();
        request.setPageSize(50);
        request.setPageIndex(index);

        try {
            RuntimeOptions runtime = new RuntimeOptions();
            QuerySmsSignListResponse response = client.querySmsSignListWithOptions(request, runtime);
            QuerySmsSignListResponseBody body = response.getBody();
            List<QuerySmsSignListResponseBody.QuerySmsSignListResponseBodySmsSignList> signList = body.getSmsSignList();

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

    private List<QuerySmsTemplateListResponseBody.QuerySmsTemplateListResponseBodySmsTemplateList> getTemplateListPrivate(int index) {
        QuerySmsTemplateListRequest request = new QuerySmsTemplateListRequest();
        request.setPageSize(50);
        request.setPageIndex(index);

        try {
            RuntimeOptions runtime = new RuntimeOptions();
            QuerySmsTemplateListResponse response = client.querySmsTemplateListWithOptions(request, runtime);
            QuerySmsTemplateListResponseBody body = response.getBody();
            List<QuerySmsTemplateListResponseBody.QuerySmsTemplateListResponseBodySmsTemplateList> templateList = body.getSmsTemplateList();

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
