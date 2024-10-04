package cn.projectan.strix.core.datamask;

/**
 * 数据脱敏操作
 *
 * @author ProjectAn
 * @since 2023/2/22 14:29
 */
public interface DataMaskOperation {

    String mask(String data, char maskChar, int n1, int n2);

}
