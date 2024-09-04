package com.reopenai.component.pulsar.annotation;

import com.reopenai.component.pulsar.producer.core.PulsarProducerRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 显式声明Pulsar声明式Producer的扫描范围.
 * <p>
 * 默认情况下(不使用此注解), 框架会自动扫描 main class 所在包(及子包)下所有
 * {@link PulsarProducer}接口; 一旦使用此注解并指定了 basePackages/basePackageClasses,
 * 则只扫描指定的目录, 覆盖默认的全包扫描.
 * <p>
 * 用法示例:
 * <pre>{@code
 * // 显式指定扫描包(只扫描指定目录)
 * @ImportProducers("com.example.producer")
 *
 * // 等价写法
 * @ImportProducers(basePackages = "com.example.producer")
 * @ImportProducers(basePackageClasses = MyProducer.class)
 * }</pre>
 *
 * @author Allen Huang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(PulsarProducerRegistrar.class)
public @interface ImportProducers {

    /**
     * {@link #basePackages()}的别名简写, 与Spring Data/MyBatis的 value 习惯一致
     */
    @AliasFor("basePackages")
    String[] value() default {};

    /**
     * 扫描{@link PulsarProducer}接口的基础包, 与{@link #basePackageClasses()}取并集
     */
    @AliasFor("value")
    String[] basePackages() default {};

    /**
     * 类型安全的basePackages替代方案, 取这些类所在的包作为扫描包
     */
    Class<?>[] basePackageClasses() default {};

}
