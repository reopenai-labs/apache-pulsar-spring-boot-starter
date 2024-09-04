package com.reopenai.component.pulsar.utils;

import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * 方法参数工具类
 *
 * @author Allen Huang
 */
public final class MethodParameterUtil {

    /**
     * 解析方法的参数类型的列表
     *
     * @param method 目标方法
     * @return 如果该方法存在参数，则返回参数的类型列表，否则返回空的List实例
     */
    public static List<MethodParameter> parseMethodParameters(Method method) {
        int parameterCount = method.getParameterCount();
        if (parameterCount != 0) {
            MethodParameter[] parameterInfos = new MethodParameter[parameterCount];
            for (int i = 0; i < parameterCount; i++) {
                parameterInfos[i] = new MethodParameter(method, i);
            }
            return List.of(parameterInfos);
        }
        return Collections.emptyList();
    }

}
