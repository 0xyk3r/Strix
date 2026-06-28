package cn.projectan.strix.util.document.patch;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.jupiter.api.Test;

/**
 * @author ProjectAn
 * @since 2025/8/1 14:54
 */
public class AsposePdfPatchTest {

    /**
     * 修补 Aspose.Words
     * <p>
     * 将输出文件覆盖进 jar 包中, 并删除 META-INF 目录下的签名文件 (RSA / SF) 即可
     */
    @Test
    public void patch() {
        String sourcePath = "Z:\\aspose-pdf-23.2.jar";
        String targetPath = "Z:\\aspose-pdf\\";

        try {
            ClassPool.getDefault().insertClassPath(sourcePath);
            CtClass zzZJJClass = ClassPool.getDefault().getCtClass("com.aspose.pdf.l10k");
            CtMethod[] methodA = zzZJJClass.getDeclaredMethods();
            for (CtMethod ctMethod : methodA) {
                CtClass[] ps = ctMethod.getParameterTypes();
                if (ps.length == 1 && ctMethod.getName().equals("lI") && ps[0].getName().equals("java.io.InputStream")) {
                    System.out.println("ps[0].getName==" + ps[0].getName());
                    ctMethod.setBody("{lI(this);com.aspose.pdf.internal.imaging.internal.p71.Helper.help1();this.l0v = com.aspose.pdf.l11if.lf;lI=true;}");
                }
            }
            zzZJJClass.writeFile(targetPath);
        } catch (Exception e) {
            System.out.println("错误==" + e);
        }
    }

}
