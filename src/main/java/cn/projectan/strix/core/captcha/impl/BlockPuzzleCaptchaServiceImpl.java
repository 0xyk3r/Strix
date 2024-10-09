package cn.projectan.strix.core.captcha.impl;


import cn.projectan.strix.core.captcha.AbstractCaptchaService;
import cn.projectan.strix.core.captcha.util.StrixCaptchaAESUtil;
import cn.projectan.strix.core.captcha.util.StrixCaptchaImageUtils;
import cn.projectan.strix.core.captcha.util.StrixCaptchaJsonUtil;
import cn.projectan.strix.core.captcha.util.StrixCaptchaRandomUtils;
import cn.projectan.strix.model.enums.CaptchaRepCodeEnum;
import cn.projectan.strix.model.enums.CaptchaTypeEnum;
import cn.projectan.strix.model.other.captcha.CaptchaInfoVO;
import cn.projectan.strix.model.other.captcha.CaptchaPointVO;
import cn.projectan.strix.model.response.module.captcha.StrixCaptchaResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;

/**
 * 滑动验证码
 *
 * @author ProjectAn
 * @since 2024/3/30 13:00
 */
@Slf4j
@Service
public class BlockPuzzleCaptchaServiceImpl extends AbstractCaptchaService {

    @Override
    public void init(Properties config) {
        super.init(config);
    }

    @Override
    public void destroy(Properties config) {
//        logger.info("start-clear-history-data-", captchaType());
    }

    @Override
    public String captchaType() {
        return CaptchaTypeEnum.BLOCKPUZZLE.getCodeValue();
    }

    @Override
    public StrixCaptchaResp get(CaptchaInfoVO captchaInfoVO) {
        StrixCaptchaResp r = super.get(captchaInfoVO);
        if (!validatedReq(r)) {
            return r;
        }
        // 原生图片
        BufferedImage originalImage = StrixCaptchaImageUtils.getOriginal();
        if (null == originalImage) {
            log.error("Strix Captcha: 滑动底图未初始化成功，请检查路径");
            return StrixCaptchaResp.errorMsg(CaptchaRepCodeEnum.API_CAPTCHA_BASEMAP_NULL);
        }
        // 设置水印
//        Graphics backgroundGraphics = originalImage.getGraphics();
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
//        backgroundGraphics.setFont(waterMarkFont);
//        backgroundGraphics.setColor(Color.white);
//        backgroundGraphics.drawString(waterMark, width - getEnOrChLength(waterMark), height - (HAN_ZI_SIZE / 2) + 7);

        // 抠图图片
        String jigsawImageBase64 = StrixCaptchaImageUtils.getSlidingBlock();
        BufferedImage jigsawImage = StrixCaptchaImageUtils.getBase64StrToImage(jigsawImageBase64);
        if (null == jigsawImage) {
            log.error("Strix Captcha: 滑动底图未初始化成功，请检查路径");
            return StrixCaptchaResp.errorMsg(CaptchaRepCodeEnum.API_CAPTCHA_BASEMAP_NULL);
        }
        CaptchaInfoVO captcha = pictureTemplatesCut(originalImage, jigsawImage, jigsawImageBase64);
        if (captcha == null
                || !StringUtils.hasText(captcha.getJigsawImageBase64())
                || !StringUtils.hasText(captcha.getOriginalImageBase64())) {
            return StrixCaptchaResp.errorMsg(CaptchaRepCodeEnum.API_CAPTCHA_ERROR);
        }
        return StrixCaptchaResp.successData(captcha);
    }

    @Override
    public StrixCaptchaResp check(CaptchaInfoVO captchaInfoVO) {
        StrixCaptchaResp r = super.check(captchaInfoVO);
        if (!validatedReq(r)) {
            return r;
        }
        // 取坐标信息
        String codeKey = String.format(REDIS_CAPTCHA_KEY, captchaInfoVO.getToken());
        if (!CaptchaServiceFactory.getCache(cacheType).exists(codeKey)) {
            return StrixCaptchaResp.errorMsg(CaptchaRepCodeEnum.API_CAPTCHA_INVALID);
        }
        String s = CaptchaServiceFactory.getCache(cacheType).get(codeKey);
        // 验证码只用一次，即刻失效
        CaptchaServiceFactory.getCache(cacheType).delete(codeKey);
        CaptchaPointVO point;
        CaptchaPointVO point1;
        String pointJson;
        try {
            point = StrixCaptchaJsonUtil.parseObject(s, CaptchaPointVO.class);
            //aes解密
            pointJson = decrypt(captchaInfoVO.getPointJson(), point.getSecretKey());
            point1 = StrixCaptchaJsonUtil.parseObject(pointJson, CaptchaPointVO.class);
        } catch (Exception e) {
            log.error("Strix Captcha: 验证码坐标解析失败", e);
            afterValidateFail(captchaInfoVO);
            return StrixCaptchaResp.errorMsg(e.getMessage());
        }
        if (point.x - Integer.parseInt(slipOffset) > point1.x
                || point1.x > point.x + Integer.parseInt(slipOffset)
                || point.y != point1.y) {
            afterValidateFail(captchaInfoVO);
            return StrixCaptchaResp.errorMsg(CaptchaRepCodeEnum.API_CAPTCHA_COORDINATE_ERROR);
        }
        // 校验成功，将信息存入缓存
        String secretKey = point.getSecretKey();
        String value;
        try {
            value = StrixCaptchaAESUtil.aesEncrypt(captchaInfoVO.getToken().concat("---").concat(pointJson), secretKey);
        } catch (Exception e) {
            log.error("Strix Captcha: AES加密失败", e);
            afterValidateFail(captchaInfoVO);
            return StrixCaptchaResp.errorMsg(e.getMessage());
        }
        String secondKey = String.format(REDIS_SECOND_CAPTCHA_KEY, value);
        CaptchaServiceFactory.getCache(cacheType).set(secondKey, captchaInfoVO.getToken(), EXPIRESIN_THREE);
        captchaInfoVO.setResult(true);
        captchaInfoVO.resetClientFlag();
        return StrixCaptchaResp.successData(captchaInfoVO);
    }

    @Override
    public StrixCaptchaResp verification(CaptchaInfoVO captchaInfoVO) {
        StrixCaptchaResp r = super.verification(captchaInfoVO);
        if (!validatedReq(r)) {
            return r;
        }
        try {
            String codeKey = String.format(REDIS_SECOND_CAPTCHA_KEY, captchaInfoVO.getCaptchaVerification());
            if (!CaptchaServiceFactory.getCache(cacheType).exists(codeKey)) {
                return StrixCaptchaResp.errorMsg(CaptchaRepCodeEnum.API_CAPTCHA_INVALID);
            }
            // 二次校验取值后，即刻失效
            CaptchaServiceFactory.getCache(cacheType).delete(codeKey);
        } catch (Exception e) {
            log.error("Strix Captcha: 验证码坐标解析失败", e);
            return StrixCaptchaResp.errorMsg(e.getMessage());
        }
        return StrixCaptchaResp.success();
    }

    /**
     * 根据模板切图
     */
    public CaptchaInfoVO pictureTemplatesCut(BufferedImage originalImage, BufferedImage jigsawImage, String jigsawImageBase64) {
        try {
            CaptchaInfoVO dataVO = new CaptchaInfoVO();

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int jigsawWidth = jigsawImage.getWidth();
            int jigsawHeight = jigsawImage.getHeight();

            //随机生成拼图坐标
            CaptchaPointVO point = generateJigsawPoint(originalWidth, originalHeight, jigsawWidth, jigsawHeight);
            int x = point.getX();
            int y = point.getY();

            //生成新的拼图图像
            BufferedImage newJigsawImage = new BufferedImage(jigsawWidth, jigsawHeight, jigsawImage.getType());
            Graphics2D graphics = newJigsawImage.createGraphics();

            int bold = 5;
            //如果需要生成RGB格式，需要做如下配置,Transparency 设置透明
            newJigsawImage = graphics.getDeviceConfiguration().createCompatibleImage(jigsawWidth, jigsawHeight, Transparency.TRANSLUCENT);
            // 新建的图像根据模板颜色赋值,源图生成遮罩
            cutByTemplate(originalImage, jigsawImage, newJigsawImage, x, 0);
            if (captchaInterferenceOptions > 0) {
                int position;
                if (originalWidth - x - 5 > jigsawWidth * 2) {
                    //在原扣图右边插入干扰图
                    position = StrixCaptchaRandomUtils.getRandomInt(x + jigsawWidth + 5, originalWidth - jigsawWidth);
                } else {
                    //在原扣图左边插入干扰图
                    position = StrixCaptchaRandomUtils.getRandomInt(100, x - jigsawWidth - 5);
                }
                while (true) {
                    String s = StrixCaptchaImageUtils.getSlidingBlock();
                    if (!jigsawImageBase64.equals(s)) {
                        interferenceByTemplate(originalImage, Objects.requireNonNull(StrixCaptchaImageUtils.getBase64StrToImage(s)), position, 0);
                        break;
                    }
                }
            }
            if (captchaInterferenceOptions > 1) {
                while (true) {
                    String s = StrixCaptchaImageUtils.getSlidingBlock();
                    if (!jigsawImageBase64.equals(s)) {
                        Integer randomInt = StrixCaptchaRandomUtils.getRandomInt(jigsawWidth, 100 - jigsawWidth);
                        interferenceByTemplate(originalImage, Objects.requireNonNull(StrixCaptchaImageUtils.getBase64StrToImage(s)),
                                randomInt, 0);
                        break;
                    }
                }
            }


            // 设置“抗锯齿”的属性
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            graphics.drawImage(newJigsawImage, 0, 0, null);
            graphics.dispose();

            ByteArrayOutputStream os = new ByteArrayOutputStream(); // 新建流。
            ImageIO.write(newJigsawImage, IMAGE_TYPE_PNG, os); // 利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
            byte[] jigsawImages = os.toByteArray();

            ByteArrayOutputStream oriImagesOs = new ByteArrayOutputStream(); // 新建流。
            ImageIO.write(originalImage, IMAGE_TYPE_PNG, oriImagesOs); // 利用ImageIO类提供的write方法，将bi以jpg图片的数据模式写入流。
            byte[] oriCopyImages = oriImagesOs.toByteArray();
            Base64.Encoder encoder = Base64.getEncoder();
            dataVO.setOriginalImageBase64(encoder.encodeToString(oriCopyImages).replaceAll("\r|\n", ""));
            // point信息不传到前端，只做后端check校验
//            dataVO.setPoint(point);
            dataVO.setJigsawImageBase64(encoder.encodeToString(jigsawImages).replaceAll("\r|\n", ""));
            dataVO.setToken(StrixCaptchaRandomUtils.getUUID());
            dataVO.setSecretKey(point.getSecretKey());
//            base64StrToImage(encoder.encodeToString(oriCopyImages), "D:\\原图.png");
//            base64StrToImage(encoder.encodeToString(jigsawImages), "D:\\滑动.png");

            //将坐标信息存入redis中
            String codeKey = String.format(REDIS_CAPTCHA_KEY, dataVO.getToken());
            CaptchaServiceFactory.getCache(cacheType).set(codeKey, StrixCaptchaJsonUtil.toJSONString(point), EXPIRESIN_SECONDS);
//            logger.debug("token：{},point:{}", dataVO.getToken(), JsonUtil.toJSONString(point));
            return dataVO;
        } catch (Exception e) {
            log.warn("Strix Captcha: 滑动验证码生成失败", e);
            return null;
        }
    }

    /**
     * 随机生成拼图坐标
     *
     * @param originalWidth  原图宽度
     * @param originalHeight 原图高度
     * @param jigsawWidth    拼图宽度
     * @param jigsawHeight   拼图高度
     * @return 拼图坐标
     */
    private static CaptchaPointVO generateJigsawPoint(int originalWidth, int originalHeight, int jigsawWidth, int jigsawHeight) {
        Random random = new Random();
        int widthDifference = originalWidth - jigsawWidth;
        int heightDifference = originalHeight - jigsawHeight;
        int x, y;
        if (widthDifference <= 0) {
            x = 5;
        } else {
            x = random.nextInt(originalWidth - jigsawWidth - 100) + 100;
        }
        if (heightDifference <= 0) {
            y = 5;
        } else {
            y = random.nextInt(originalHeight - jigsawHeight) + 5;
        }
        String key = null;
        if (captchaAesStatus) {
            key = StrixCaptchaAESUtil.getKey();
        }
        return new CaptchaPointVO(x, y, key);
    }

    /**
     * @param oriImage      原图
     * @param templateImage 模板图
     * @param newImage      新抠出的小图
     * @param x             随机扣取坐标X
     * @param y             随机扣取坐标y
     */
    private static void cutByTemplate(BufferedImage oriImage, BufferedImage templateImage, BufferedImage newImage, int x, int y) {
        // 临时数组遍历用于高斯模糊存周边像素值
        int[][] matrix = new int[3][3];
        int[] values = new int[9];

        int xLength = templateImage.getWidth();
        int yLength = templateImage.getHeight();
        // 模板图像宽度
        for (int i = 0; i < xLength; i++) {
            // 模板图片高度
            for (int j = 0; j < yLength; j++) {
                // 如果模板图像当前像素点不是透明色 copy源文件信息到目标图片中
                int rgb = templateImage.getRGB(i, j);
                if (rgb < 0) {
                    newImage.setRGB(i, j, oriImage.getRGB(x + i, y + j));

                    //抠图区域高斯模糊
                    readPixel(oriImage, x + i, y + j, values);
                    fillMatrix(matrix, values);
                    oriImage.setRGB(x + i, y + j, avgMatrix(matrix));
                }

                // 防止数组越界判断
                if (i == (xLength - 1) || j == (yLength - 1)) {
                    continue;
                }
                int rightRgb = templateImage.getRGB(i + 1, j);
                int downRgb = templateImage.getRGB(i, j + 1);
                // 描边处理，,取带像素和无像素的界点，判断该点是不是临界轮廓点,如果是设置该坐标像素是白色
                if ((rgb >= 0 && rightRgb < 0) || (rgb < 0 && rightRgb >= 0) || (rgb >= 0 && downRgb < 0) || (rgb < 0 && downRgb >= 0)) {
                    newImage.setRGB(i, j, Color.white.getRGB());
                    oriImage.setRGB(x + i, y + j, Color.white.getRGB());
                }
            }
        }

    }

    /**
     * 干扰抠图处理
     *
     * @param oriImage      原图
     * @param templateImage 模板图
     * @param x             随机扣取坐标X
     * @param y             随机扣取坐标y
     */
    private static void interferenceByTemplate(BufferedImage oriImage, BufferedImage templateImage, int x, int y) {
        // 临时数组遍历用于高斯模糊存周边像素值
        int[][] matrix = new int[3][3];
        int[] values = new int[9];

        int xLength = templateImage.getWidth();
        int yLength = templateImage.getHeight();
        // 模板图像宽度
        for (int i = 0; i < xLength; i++) {
            // 模板图片高度
            for (int j = 0; j < yLength; j++) {
                // 如果模板图像当前像素点不是透明色 copy源文件信息到目标图片中
                int rgb = templateImage.getRGB(i, j);
                if (rgb < 0) {
                    // 抠图区域高斯模糊
                    readPixel(oriImage, x + i, y + j, values);
                    fillMatrix(matrix, values);
                    oriImage.setRGB(x + i, y + j, avgMatrix(matrix));
                }
                // 防止数组越界判断
                if (i == (xLength - 1) || j == (yLength - 1)) {
                    continue;
                }
                int rightRgb = templateImage.getRGB(i + 1, j);
                int downRgb = templateImage.getRGB(i, j + 1);
                // 描边处理，,取带像素和无像素的界点，判断该点是不是临界轮廓点,如果是设置该坐标像素是白色
                if ((rgb >= 0 && rightRgb < 0) || (rgb < 0 && rightRgb >= 0) || (rgb >= 0 && downRgb < 0) || (rgb < 0 && downRgb >= 0)) {
                    oriImage.setRGB(x + i, y + j, Color.white.getRGB());
                }
            }
        }

    }

    private static void readPixel(BufferedImage img, int x, int y, int[] pixels) {
        int xStart = x - 1;
        int yStart = y - 1;
        int current = 0;
        for (int i = xStart; i < 3 + xStart; i++) {
            for (int j = yStart; j < 3 + yStart; j++) {
                int tx = i;
                if (tx < 0) {
                    tx = -tx;

                } else if (tx >= img.getWidth()) {
                    tx = x;
                }
                int ty = j;
                if (ty < 0) {
                    ty = -ty;
                } else if (ty >= img.getHeight()) {
                    ty = y;
                }
                pixels[current++] = img.getRGB(tx, ty);

            }
        }
    }

    private static void fillMatrix(int[][] matrix, int[] values) {
        int filled = 0;
        for (int[] x : matrix) {
            for (int j = 0; j < x.length; j++) {
                x[j] = values[filled++];
            }
        }
    }

    private static int avgMatrix(int[][] matrix) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (int[] x : matrix) {
            for (int j = 0; j < x.length; j++) {
                if (j == 1) {
                    continue;
                }
                Color c = new Color(x[j]);
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
            }
        }
        return new Color(r / 8, g / 8, b / 8).getRGB();
    }

}
