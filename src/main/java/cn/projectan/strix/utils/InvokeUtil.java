package cn.projectan.strix.utils;

import cn.projectan.strix.model.annotation.StrixJob;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * 任务执行工具
 */
@Slf4j
public class InvokeUtil {

    public static boolean valid(String invokeTarget) {
        if (StringUtils.isEmpty(invokeTarget)) {
            return false;
        }
        if (StringUtils.isNotEmpty(invokeTarget)) {
            String beanName = getBeanName(invokeTarget);
            if (!isValidClassName(beanName)) {
                Object bean = SpringUtil.getBean(beanName);
                Class<?> aClass = bean.getClass();
                return aClass.isAnnotationPresent(StrixJob.class);
            }
        }
        return false;
    }

    /**
     * 执行方法
     *
     * @param job 任务对象
     */
    public static void invokeMethod(String invokeTarget) {
        if (StringUtils.isEmpty(invokeTarget)) {
            return;
        }
        String beanName = getBeanName(invokeTarget);
        String methodName = getMethodName(invokeTarget);
        List<Object[]> methodParams = getMethodParams(invokeTarget);

        if (valid(invokeTarget)) {
            Object bean = SpringUtil.getBean(beanName);
            invokeMethod(bean, methodName, methodParams);
        } else {
            log.warn("调用目标：" + invokeTarget + "未启动成功，请检查是否配置正确！");
        }
    }

    /**
     * 调用任务方法
     *
     * @param bean         目标对象
     * @param methodName   方法名称
     * @param methodParams 方法参数
     */
    private static void invokeMethod(Object bean, String methodName, List<Object[]> methodParams) {
        try {
            if (methodParams != null && !methodParams.isEmpty()) {
                Method method = bean.getClass().getMethod(methodName, getMethodParamsType(methodParams));
                method.invoke(bean, getMethodParamsValue(methodParams));
            } else {
                Method method = bean.getClass().getMethod(methodName);
                method.invoke(bean);
            }
        } catch (Exception e) {
            throw new RuntimeException("执行目标：" + bean.getClass().getName() + "." + methodName + "失败", e);
        }
    }

    /**
     * 校验是否为为class包名
     *
     * @param invokeTarget 名称
     * @return true是 false否
     */
    private static boolean isValidClassName(String invokeTarget) {
        return StringUtils.countMatches(invokeTarget, ".") > 1;
    }

    /**
     * 获取bean名称
     *
     * @param invokeTarget 目标字符串
     * @return bean名称
     */
    private static String getBeanName(String invokeTarget) {
        String beanName = StringUtil.substringBefore(invokeTarget, '(');
        return StringUtils.substringBeforeLast(beanName, ".");
    }

    /**
     * 获取bean方法
     *
     * @param invokeTarget 目标字符串
     * @return method方法
     */
    private static String getMethodName(String invokeTarget) {
        String methodName = StringUtils.substringBefore(invokeTarget, '(');
        return StringUtils.substringAfterLast(methodName, ".");
    }

    /**
     * 获取method方法参数相关列表
     *
     * @param invokeTarget 目标字符串
     * @return method方法相关参数列表
     */
    private static List<Object[]> getMethodParams(String invokeTarget) {
        String methodStr = StringUtils.substringBetween(invokeTarget, "(", ")");
        if (StringUtils.isEmpty(methodStr)) {
            return null;
        }
        String[] methodParams = methodStr.split(",(?=([^\"']*[\"'][^\"']*[\"'])*[^\"']*$)");
        List<Object[]> classList = new LinkedList<>();
        for (String methodParam : methodParams) {
            String str = StringUtils.trimToEmpty(methodParam);
            // String，以'或"开头
            if (StringUtils.startsWithAny(str, "'", "\"")) {
                classList.add(new Object[]{StringUtils.substring(str, 1, str.length() - 1), String.class});
            }
            // boolean，等于true或者false
            else if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)) {
                classList.add(new Object[]{Boolean.valueOf(str), Boolean.class});
            }
            // long，以L结尾
            else if (StringUtils.endsWith(str, "L")) {
                classList.add(new Object[]{Long.valueOf(StringUtils.substring(str, 0, str.length() - 1)), Long.class});
            }
            // double，以D结尾
            else if (StringUtils.endsWith(str, "D")) {
                classList.add(new Object[]{Double.valueOf(StringUtils.substring(str, 0, str.length() - 1)), Double.class});
            }
            // 其他类型归类为 int
            else {
                classList.add(new Object[]{Integer.valueOf(str), Integer.class});
            }
        }
        return classList;
    }

    /**
     * 获取参数类型
     *
     * @param methodParams 参数相关列表
     * @return 参数类型列表
     */
    private static Class<?>[] getMethodParamsType(List<Object[]> methodParams) {
        Class<?>[] classList = new Class<?>[methodParams.size()];
        int index = 0;
        for (Object[] os : methodParams) {
            classList[index] = (Class<?>) os[1];
            index++;
        }
        return classList;
    }

    /**
     * 获取参数值
     *
     * @param methodParams 参数相关列表
     * @return 参数值列表
     */
    private static Object[] getMethodParamsValue(List<Object[]> methodParams) {
        Object[] classList = new Object[methodParams.size()];
        int index = 0;
        for (Object[] os : methodParams) {
            classList[index] = os[0];
            index++;
        }
        return classList;
    }

}
