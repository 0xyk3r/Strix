package cn.projectan.strix.util.document.patch;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.jupiter.api.Test;

/**
 * @author ProjectAn
 * @since 2025/8/1 14:54
 */
public class AsposeSlidesPatchTest {

    /**
     * 修补 Aspose.Words
     * <p>
     * 将输出文件覆盖进 jar 包中, 并删除 META-INF 目录下的签名文件 (RSA / SF) 即可
     */
    @Test
    public void patch() {
        String sourcePath = "Z:\\aspose-slides-23.1-jdk16.jar";
        String targetPath = "Z:\\aspose-slides\\";

        try {
            ClassPool.getDefault().insertClassPath(sourcePath);
            CtClass zzZJJClass = ClassPool.getDefault().getCtClass("com.aspose.slides.internal.oh.public");
            CtMethod[] methodA = zzZJJClass.getDeclaredMethods();
            for (CtMethod ctMethod : methodA) {
                CtClass[] ps = ctMethod.getParameterTypes();
                if (ps.length == 3 && ctMethod.getName().equals("do")) {
                    System.out.println("ps[0].getName==" + ps[0].getName());
                    ctMethod.setBody("{}");
                }
            }
            zzZJJClass.writeFile(targetPath);
        } catch (Exception e) {
            System.out.println("错误==" + e);
        }
    }

}
