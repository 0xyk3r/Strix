package cn.projectan.strix.aot;


import cn.projectan.strix.StrixApplication;
import cn.projectan.strix.controller.system.SystemController;
import cn.projectan.strix.controller.system.SystemDictController;
import cn.projectan.strix.controller.system.SystemMenuController;
import cn.projectan.strix.controller.system.base.BaseSystemController;
import cn.projectan.strix.controller.system.common.DictController;
import cn.projectan.strix.controller.system.module.job.JobController;
import cn.projectan.strix.controller.system.module.oss.OssController;
import cn.projectan.strix.controller.system.module.sms.SmsController;
import cn.projectan.strix.controller.system.monitor.LogController;
import cn.projectan.strix.controller.system.tool.PopularityController;
import cn.projectan.strix.service.impl.*;
import cn.projectan.strix.utils.PopularityUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeSerialization;

/**
 * Lambda 表达式注册特性
 *
 * @author ProjectAn
 * @date 2024/3/24 15:57
 */
public class LambdaRegistrationFeature implements Feature {

    @Override
    public void duringSetup(DuringSetupAccess access) {
        // TODO 这里需要将 lambda 表达式所使用的成员类都注册上来,一般扫描 @Controller 和 @Service.
        RuntimeSerialization.registerLambdaCapturingClass(StrixApplication.class);

        RuntimeSerialization.registerLambdaCapturingClass(BaseSystemController.class);
        RuntimeSerialization.registerLambdaCapturingClass(SystemController.class);
        RuntimeSerialization.registerLambdaCapturingClass(DictController.class);
        RuntimeSerialization.registerLambdaCapturingClass(JobController.class);
        RuntimeSerialization.registerLambdaCapturingClass(OssController.class);
        RuntimeSerialization.registerLambdaCapturingClass(SmsController.class);
        RuntimeSerialization.registerLambdaCapturingClass(LogController.class);
        RuntimeSerialization.registerLambdaCapturingClass(PopularityController.class);
        RuntimeSerialization.registerLambdaCapturingClass(SystemMenuController.class);
        RuntimeSerialization.registerLambdaCapturingClass(SystemDictController.class);

        RuntimeSerialization.registerLambdaCapturingClass(ServiceImpl.class);
        RuntimeSerialization.registerLambdaCapturingClass(SystemManagerServiceImpl.class);
        RuntimeSerialization.registerLambdaCapturingClass(SystemMenuServiceImpl.class);
        RuntimeSerialization.registerLambdaCapturingClass(DictServiceImpl.class);
        RuntimeSerialization.registerLambdaCapturingClass(DictDataServiceImpl.class);
        RuntimeSerialization.registerLambdaCapturingClass(OssBucketServiceImpl.class);
        RuntimeSerialization.registerLambdaCapturingClass(OssFileGroupServiceImpl.class);
        RuntimeSerialization.registerLambdaCapturingClass(PayConfigServiceImpl.class);
        RuntimeSerialization.registerLambdaCapturingClass(SmsSignServiceImpl.class);
        RuntimeSerialization.registerLambdaCapturingClass(SmsTemplateServiceImpl.class);

        RuntimeSerialization.registerLambdaCapturingClass(PopularityUtil.class);
    }

}
