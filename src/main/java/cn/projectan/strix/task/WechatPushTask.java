package cn.projectan.strix.task;

import cn.hutool.core.map.MapUtil;
import cn.projectan.strix.config.GlobalWechatConfig;
import cn.projectan.strix.model.db.WechatPush;
import cn.projectan.strix.model.wechat.WechatConfigBean;
import cn.projectan.strix.service.WechatPushService;
import cn.projectan.strix.utils.OkHttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author ProjectAn
 * @date 2022/3/27 12:17
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "strix.module", name = "push", havingValue = "true")
@RequiredArgsConstructor
public class WechatPushTask {

    private final WechatPushService wechatPushService;
    private final GlobalWechatConfig globalWechatConfig;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0/10 * * * * ?")
    public void sendWxPush() {
        try {
            QueryWrapper<WechatPush> wechatPushQueryWrapper = new QueryWrapper<>();
            wechatPushQueryWrapper.eq("push_status", 1);
            List<WechatPush> wechatPushList = wechatPushService.list(wechatPushQueryWrapper);

            for (WechatPush wp : wechatPushList) {
                send(wp);
                break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void send(WechatPush wp) {
        OkHttpClient httpClient = OkHttpUtil.getInstance();

        WechatConfigBean instance = globalWechatConfig.getInstance(wp.getConfigId());
        if (instance == null) {
            return;
        }

        RequestBody requestBody = RequestBody.create(wp.getTemplateBody(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + instance.getAccessToken()).post(requestBody).build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error(e.getMessage(), e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                wp.setResultBody(result);
                Map<String, Object> resultMap = objectMapper.readValue(result, new TypeReference<>() {
                });

                Integer errCode = MapUtil.getInt(resultMap, "errcode", -1);

                if (errCode == 0) {
                    // 推送成功
                    wp.setPushStatus(2);
                } else if (errCode == 40001) {
                    // accessToken失效 不做任何处理 自行重试
                    log.info("推送消息失败，AccessToken失效");
                    return;
                } else if (result.contains("block")) {
                    log.info("推送消息失败，block");
                    wp.setPushStatus(3);
                } else if (result.contains("failed")) {
                    log.info("推送消息失败，failed");
                    wp.setPushStatus(3);
                } else {
                    log.info("推送消息失败，其他错误");
                    wp.setPushStatus(3);
                }
                wechatPushService.updateById(wp);
            }
        });

    }

}
