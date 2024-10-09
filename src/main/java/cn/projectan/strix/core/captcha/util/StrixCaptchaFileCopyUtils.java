package cn.projectan.strix.core.captcha.util;

import java.io.*;
import java.nio.file.Files;

/**
 * Strix Captcha 文件拷贝工具类
 *
 * @author ProjectAn
 * @since 2024/3/30 13:00
 */
public abstract class StrixCaptchaFileCopyUtils {

    public static final int BUFFER_SIZE = 4096;

    public StrixCaptchaFileCopyUtils() {
    }

    public static int copy(File in, File out) throws IOException {
        return copy(Files.newInputStream(in.toPath()), Files.newOutputStream(out.toPath()));
    }

    public static void copy(byte[] in, File out) throws IOException {
        copy(new ByteArrayInputStream(in), Files.newOutputStream(out.toPath()));
    }

    public static byte[] copyToByteArray(File in) throws IOException {
        return copyToByteArray(Files.newInputStream(in.toPath()));
    }

    public static int copy(InputStream in, OutputStream out) throws IOException {
        int var2;
        try (in; out) {
            var2 = StrixCaptchaStreamUtils.copy(in, out);
        }
        return var2;
    }

    public static void copy(byte[] in, OutputStream out) throws IOException {
        try (out) {
            out.write(in);
        }
    }

    public static byte[] copyToByteArray(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        copy(in, out);
        return out.toByteArray();
    }

    public static int copy(Reader in, Writer out) throws IOException {
        try (in; out) {
            int byteCount = 0;
            char[] buffer = new char[4096];
            int bytesRead;
            for (boolean var4 = true; (bytesRead = in.read(buffer)) != -1; byteCount += bytesRead) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            return byteCount;
        }
    }

    public static void copy(String in, Writer out) throws IOException {
        try (out) {
            out.write(in);
        }
    }

    public static String copyToString(Reader in) throws IOException {
        if (in == null) {
            return "";
        } else {
            StringWriter out = new StringWriter();
            copy(in, out);
            return out.toString();
        }
    }

}

