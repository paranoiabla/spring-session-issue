package demo;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.session.ExpiringSession;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.web.http.CookieHttpSessionStrategy;
import org.springframework.session.web.http.HttpSessionStrategy;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import redis.clients.jedis.Protocol;
import redis.embedded.RedisServer;

import javax.servlet.Filter;
import java.util.Arrays;

@SpringBootApplication
public class SpringSessionIssueApplication extends WebMvcConfigurerAdapter {

    public static void main(String[] args) {
        SpringApplication.run(SpringSessionIssueApplication.class, args);
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.viewResolver(defaultInternalResourceViewResolver());
        super.configureViewResolvers(registry);
    }

    /* Session */

    @Bean(name = { "defaultSessionFilter", "sessionFilter" })
    public Filter sessionFilter(final SessionRepository<ExpiringSession> sessionRepository, final HttpSessionStrategy sessionStrategy) {

        SessionRepositoryFilter<ExpiringSession> sessionRepositoryFilter = new SessionRepositoryFilter<>(sessionRepository);
        sessionRepositoryFilter.setHttpSessionStrategy(sessionStrategy);

        CompositeFilter compositeFilter = new CompositeFilter();
        compositeFilter.setFilters(Arrays.asList(sessionRepositoryFilter));

        return compositeFilter;
    }

    @Bean(name = "sessionStrategy")
    public HttpSessionStrategy defaultSessionStrategy() {
        final CookieHttpSessionStrategy result = new CookieHttpSessionStrategy();
        result.setCookieName("TEST_SESSION_COOKIE");
        return result;
    }

    @Bean
    @ConditionalOnProperty(name = { "nemesis.platform.redis.host" }, matchIfMissing = true)
    public static RedisServerBean redisServer() {
        return new RedisServerBean();
    }

    @Bean(name = { "defaultRedisSessionRepository", "sessionRepository" })
    public SessionRepository defaultRedisSessionRepository(JedisConnectionFactory redisCF) throws Exception {
        return new RedisOperationsSessionRepository(redisCF);
    }

    @Bean
    public JedisConnectionFactory connectionFactory(final Environment environment) throws Exception {
        final JedisConnectionFactory jcf = new JedisConnectionFactory();
        jcf.setHostName(environment.getProperty("nemesis.platform.redis.host", String.class, "localhost"));
        jcf.setPort(environment.getProperty("nemesis.platform.redis.port", Integer.class, Protocol.DEFAULT_PORT));
        jcf.setPassword(environment.getProperty("nemesis.platform.redis.password", String.class, ""));
        jcf.afterPropertiesSet();

        return jcf;
    }

    /**
     * Implements BeanDefinitionRegistryPostProcessor to ensure this Bean
     * is initialized before any other Beans. Specifically, we want to ensure
     * that the Redis Server is started before RedisHttpSessionConfiguration
     * attempts to enable Keyspace notifications.
     */
    static class RedisServerBean implements InitializingBean, DisposableBean, BeanDefinitionRegistryPostProcessor {
        private RedisServer redisServer;

        public void afterPropertiesSet() throws Exception {
            redisServer = new RedisServer(Protocol.DEFAULT_PORT);
            redisServer.start();
        }

        public void destroy() throws Exception {
            if (redisServer != null) {
                redisServer.stop();
            }
        }

        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        }

        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        }
    }

    protected ViewResolver defaultInternalResourceViewResolver() {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setViewClass(JstlView.class);
        viewResolver.setOrder(1);
        viewResolver.setPrefix("/WEB-INF/static/");
        viewResolver.setSuffix(".jsp");
        viewResolver.setRedirectHttp10Compatible(false);

        return viewResolver;
    }
}
