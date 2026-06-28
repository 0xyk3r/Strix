package cn.projectan.strix.util.document.patch;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.jupiter.api.Test;

/**
 * @author ProjectAn
 * @since 2026/6/28 12:16
 */
public class AsposeCellsPatchTest {

    /**
     * 修补 Aspose.Words
     * <p>
     * 将输出文件覆盖进 jar 包中, 并删除 META-INF 目录下的签名文件 (RSA / SF) 即可
     */
    @Test
    public void patch() {
        String sourcePath = "Z:\\aspose-cells-23.2.jar";
        String targetPath = "Z:\\aspose-cells\\";

        try {
            ClassPool.getDefault().insertClassPath(sourcePath);
            CtClass zzZJJClass = ClassPool.getDefault().getCtClass("com.aspose.cells.p0h");
            CtMethod[] methodA = zzZJJClass.getDeclaredMethods();
            for (CtMethod ctMethod : methodA) {
                CtClass[] ps = ctMethod.getParameterTypes();
                if (ps.length == 1 && ctMethod.getName().equals("a") && ps[0].getName().equals("org.w3c.dom.Document")) {
                    System.out.println("ps[0].getName==" + ps[0].getName());
                    ctMethod.setBody("{a = this;com.aspose.cells.r84.a();}");
                }
            }
            zzZJJClass.writeFile(targetPath);
        } catch (Exception e) {
            System.out.println("错误==" + e);
        }
    }

}
