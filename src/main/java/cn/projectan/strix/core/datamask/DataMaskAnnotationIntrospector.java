package cn.projectan.strix.core.datamask;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

/**
 * 数据脱敏注解解析器
 *
 * @author ProjectAn
 * @since 2023/2/22 14:55
 */
public class DataMaskAnnotationIntrospector extends NopAnnotationIntrospector {

    @Override
    public Object findSerializer(Annotated am) {
        DataMask annotation = am.getAnnotation(DataMask.class);
        if (annotation != null) {
            return new DataMaskSerializer(annotation.maskFunc().operation(), annotation.maskChar(), annotation.n1(), annotation.n2());
        }
        return null;
    }

}
