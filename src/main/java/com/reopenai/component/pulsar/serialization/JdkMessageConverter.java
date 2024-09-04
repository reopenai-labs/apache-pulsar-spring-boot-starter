package com.reopenai.component.pulsar.serialization;

import org.springframework.core.MethodParameter;

import java.io.*;

/**
 * 基于JDK默认的序列化方式实现的序列化解释器
 *
 * @author Allen Huang
 */
public class JdkMessageConverter implements MessageConverter {

    @Override
    public byte[] serialize(Object payload, MethodParameter parameter) {
        if (!(payload instanceof Serializable)) {
            throw new IllegalArgumentException("Serialization of the message failed, parameters serialized with JDK must implement the Serializable interface.");
        }
        return serialize(payload);
    }

    @Override
    public Object deserialize(byte[] data, MethodParameter parameter) {
        if (data == null) {
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return ois.readObject();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to deserialize object", ex);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to deserialize object type", ex);
        }
    }

    @Override
    public String supportedType() {
        return MessageProtocol.JDK;
    }

    private byte[] serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(obj);
            oos.flush();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to serialize object of type: " + obj.getClass(), ex);
        }
        return out.toByteArray();
    }

}
