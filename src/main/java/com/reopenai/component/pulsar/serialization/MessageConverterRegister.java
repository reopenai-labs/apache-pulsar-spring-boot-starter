package com.reopenai.component.pulsar.serialization;

/**
 * @author Allen Huang
 */
public interface MessageConverterRegister {

    void register(MessageConverter converter);

    MessageConverter get(String protocol);

}
