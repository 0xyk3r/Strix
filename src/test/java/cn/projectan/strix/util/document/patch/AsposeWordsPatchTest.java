package cn.projectan.strix.util.document.patch;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

/**
 * @author ProjectAn
 * @since 2025/10/10 10:47
 */
public class AsposeWordsPatchTest {

    /**
     * 修补 Aspose.Words
     * <p>
     * 将输出文件覆盖进 jar 包中, 并删除 META-INF 目录下的签名文件 (RSA / SF) 即可
     */
    @Test
    @SneakyThrows
    public void patch() {
        String sourcePath = "Z:\\aspose-words-24.7-jdk17.jar";
        String targetPath = "Z:\\aspose-words\\";

        ClassPool.getDefault().insertClassPath(sourcePath);
        CtClass ctClass;
        CtMethod[] methods;

        ctClass = ClassPool.getDefault().getCtClass("com.aspose.words.zzA1");
        methods = ctClass.getDeclaredMethods();
        for (CtMethod method : methods) {
            CtClass[] types = method.getParameterTypes();
            if (types.length == 0 && "zzVUb".equals(method.getName())) {
                method.setBody("return 256;");
                break;
            }
        }
        ctClass.writeFile(targetPath);

        ctClass = ClassPool.getDefault().getCtClass("com.aspose.words.zzZN");
        methods = ctClass.getDeclaredMethods();
        for (CtMethod method : methods) {
            CtClass[] types = method.getParameterTypes();
            if (types.length == 0 && "zzYFB".equals(method.getName())) {
                method.setBody("{if (zzYLc == 0L) { zzYLc ^= zzZlz; } return com.aspose.words.zzX6d.zzYW8;}");
                break;
            }
        }
        ctClass.writeFile(targetPath);
    }

}
