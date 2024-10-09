package cn.projectan.strix.aot;


import cn.hutool.core.util.ClassUtil;
import cn.projectan.strix.StrixApplication;
import cn.projectan.strix.core.cache.SystemMenuCache;
import cn.projectan.strix.initializer.DictSyncInitializer;
import cn.projectan.strix.initializer.SecurityRuleInitializer;
import cn.projectan.strix.task.StrixOAuthPushTask;
import cn.projectan.strix.util.PopularityUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeSerialization;

import java.util.Set;

/**
 * Lambda 表达式注册特性
 *
 * @author ProjectAn
 * @since 2024/3/24 15:57
 */
public class LambdaRegistrationFeature implements Feature {

    @Override
    public void duringSetup(DuringSetupAccess access) {
        // 这里需要将 Lambda 表达式所使用的成员类都注册, 目前扫描了 Controller 和 Service.
        RuntimeSerialization.registerLambdaCapturingClass(StrixApplication.class);

        // 扫描 BaseController 的子类
        Set<Class<?>> controllerClazzSet = ClassUtil.scanPackage(
                "cn.projectan.strix.controller",
                clazz -> ClassUtil.isAssignable(cn.projectan.strix.controller.BaseController.class, clazz)
        );
        controllerClazzSet.forEach(RuntimeSerialization::registerLambdaCapturingClass);

        // 扫描 ServiceImpl 的子类
        Set<Class<?>> serviceImplClazzSet = ClassUtil.scanPackage(
                "cn.projectan.strix.service.impl",
                clazz -> ClassUtil.isAssignable(ServiceImpl.class, clazz)
        );
        serviceImplClazzSet.forEach(RuntimeSerialization::registerLambdaCapturingClass);

        // TODO 在此补充其他使用了 Lambda 表达式的类
        RuntimeSerialization.registerLambdaCapturingClass(DictSyncInitializer.class);
        RuntimeSerialization.registerLambdaCapturingClass(SecurityRuleInitializer.class);
        RuntimeSerialization.registerLambdaCapturingClass(SystemMenuCache.class);
        RuntimeSerialization.registerLambdaCapturingClass(PopularityUtil.class);
        RuntimeSerialization.registerLambdaCapturingClass(StrixOAuthPushTask.class);
    }

}
