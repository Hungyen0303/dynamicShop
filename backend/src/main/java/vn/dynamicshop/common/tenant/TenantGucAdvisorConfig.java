package vn.dynamicshop.common.tenant;

import java.lang.reflect.Method;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bọc {@link TenantGucInterceptor} quanh mọi method có @Transactional bằng Spring AOP
 * thuần (không AspectJ) — {@code InfrastructureAdvisorAutoProxyCreator} do
 * {@code @EnableTransactionManagement} bật sẽ tự áp dụng advisor này cho MỌI bean, y hệt
 * cách nó áp dụng advisor của chính @Transactional.
 */
@Configuration
public class TenantGucAdvisorConfig {

    @Bean
    public Advisor tenantGucAdvisor(TenantGucInterceptor interceptor) {
        var pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return AnnotatedElementUtils.hasAnnotation(method, Transactional.class)
                        || AnnotatedElementUtils.hasAnnotation(targetClass, Transactional.class);
            }
        };
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }
}
