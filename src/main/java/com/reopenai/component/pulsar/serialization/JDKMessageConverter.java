package com.reopenai.component.pulsar.serialization;

import com.reopenai.component.pulsar.constant.MessageProtocol;
import org.springframework.core.MethodParameter;

import java.io.*;

/**
 * 基于JDK默认的序列化方式实现的序列化解释器
 *
 * @author Allen Huang
 */
public class JDKMessageConverter implements MessageConverter {

    @Override
    public byte[] serializer(Object message, MethodParameter parameterInfo) {
        if (!(message instanceof Serializable)) {
            throw new IllegalArgumentException("Serialization of the message failed, parameters serialized with JDK must implement the Serializable interface.");
        }
        return serializer(message);
    }

    @Override
    public Object deserializer(byte[] message, MethodParameter parameterInfo) {
        if (message == null) {
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(message))) {
            return ois.readObject();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to deserialize object", ex);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to deserialize object type", ex);
        }
    }

    @Override
    public String supportType() {
        return MessageProtocol.JDK;
    }

    private byte[] serializer(Object obj) {
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
