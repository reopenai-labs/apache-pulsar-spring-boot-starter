package com.reopenai.component.pulsar.producer.core;

import com.reopenai.component.pulsar.annotation.ImportProducers;
import com.reopenai.component.pulsar.annotation.PulsarProducer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 扫描{@link PulsarProducer}标注的接口, 为每个接口注册一个{@link PulsarProducerFactoryBean}.
 * <p>
 * 触发方式(两者叠加生效):
 * <ul>
 *   <li>由 {@code ApachePulsarAutoConfiguration} 自动触发 —— 扫描 main class 所在包, 用户无需任何配置</li>
 *   <li>用户标注 {@link ImportProducers} 显式指定包 —— 额外导入 main 包之外的外部Producer</li>
 * </ul>
 * 同一个 {@code @PulsarProducer} 接口无论被扫描/导入几次, 都按接口名去重, 只注册一次, 避免重复初始化Bean.
 * <p>
 * 说明: Spring Framework 7 虽引入了更现代的 {@code BeanRegistrar}, 但其
 * {@code register(BeanRegistry, Environment)} 签名无法获取触发注解的 {@link AnnotationMetadata},
 * 不适用于"按注解属性动态扫描注册"的场景, 故仍采用 ImportBeanDefinitionRegistrar.
 *
 * @author Allen Huang
 */
public class PulsarProducerRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

    private static final String BASE_PACKAGES = "basePackages";

    private static final String BASE_PACKAGE_CLASSES = "basePackageClasses";

    private Environment environment;

    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        Set<String> basePackages = resolveBasePackages(metadata, registry);
        ClassPathScanningCandidateComponentProvider scanner = createScanner();
        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                String beanClassName = candidate.getBeanClassName();
                Assert.hasText(beanClassName, "Producer接口名不能为空");
                String beanName = StringUtils.unqualify(beanClassName);
                if (registry.containsBeanDefinition(beanName)) {
                    continue;
                }
                BeanDefinitionBuilder builder = BeanDefinitionBuilder
                        .genericBeanDefinition(PulsarProducerFactoryBean.class)
                        .addPropertyValue("type", beanClassName);
                registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
            }
        }
    }

    /**
     * 创建接口扫描器: 关闭默认过滤器, 放行接口(默认实现会过滤掉接口), 注入容器的 Environment 与 ResourceLoader
     */
    private ClassPathScanningCandidateComponentProvider createScanner() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
            }
        };
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(PulsarProducer.class));
        return scanner;
    }

    /**
     * 解析扫描包: 由 {@link ImportProducers} 显式指定时扫描指定包(用于导入外部Producer);
     * 否则(自动触发)使用 main class 所在包(Spring Boot 的 {@link AutoConfigurationPackages})
     */
    private Set<String> resolveBasePackages(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(ImportProducers.class.getName()));
        Set<String> basePackages = new LinkedHashSet<>();
        if (attributes != null) {
            basePackages.addAll(Arrays.asList(attributes.getStringArray(BASE_PACKAGES)));
            for (Class<?> clazz : attributes.getClassArray(BASE_PACKAGE_CLASSES)) {
                basePackages.add(clazz.getPackage().getName());
            }
        }
        if (basePackages.isEmpty() && registry instanceof BeanFactory beanFactory
                && AutoConfigurationPackages.has(beanFactory)) {
            basePackages.addAll(AutoConfigurationPackages.get(beanFactory));
        }
        return basePackages;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

}
