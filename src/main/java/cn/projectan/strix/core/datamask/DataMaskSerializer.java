package cn.projectan.strix.core.datamask;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;

import java.util.Objects;

/**
 * 数据脱敏序列化器
 *
 * @author ProjectAn
 * @since 2023/2/22 14:56
 */
public final class DataMaskSerializer extends StdScalarSerializer<Object> {

    private final DataMaskOperation operation;

    private final char maskChar;

    private final int n1;

    private final int n2;

    public DataMaskSerializer() {
        super(String.class, false);
        this.operation = null;
        this.maskChar = '*';
        this.n1 = 2;
        this.n2 = 2;
    }

    public DataMaskSerializer(DataMaskOperation operation, char maskChar, int n1, int n2) {
        super(String.class, false);
        this.operation = operation;
        this.maskChar = maskChar;
        this.n1 = n1;
        this.n2 = n2;
    }

    @Override
    public boolean isEmpty(SerializationContext context, Object value) {
        if (value == null) {
            return true;
        }
        String str = (String) value;
        return str.isEmpty();
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext context) {
        String content;
        if (Objects.isNull(operation)) {
            content = DataMaskFunc.KEEP_SIDE.operation().mask((String) value, maskChar, n1, n2);
        } else {
            content = operation.mask((String) value, maskChar, n1, n2);
        }
        gen.writeString(content);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializationContext context, TypeSerializer typeSer) {
        this.serialize(value, gen, context);
    }

}
