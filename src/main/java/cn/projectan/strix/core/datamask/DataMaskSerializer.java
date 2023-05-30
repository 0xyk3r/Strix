package cn.projectan.strix.core.datamask;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;
import java.util.Objects;

/**
 * @author 安炯奕
 * @date 2023/2/22 14:56
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
    public boolean isEmpty(SerializerProvider prov, Object value) {
        String str = (String) value;
        return str.isEmpty();
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (Objects.isNull(operation)) {
            String content = DataMaskFunc.KEEP_SIDE.operation().mask((String) value, maskChar, n1, n2);
            gen.writeString(content);
        } else {
            String content = operation.mask((String) value, maskChar, n1, n2);
            gen.writeString(content);
        }
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        this.serialize(value, gen, provider);
    }

}
