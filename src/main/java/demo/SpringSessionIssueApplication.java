package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.CookieHttpSessionStrategy;
import org.springframework.session.web.http.HttpSessionStrategy;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.filter.CompositeFilter;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

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

    @Bean(name = "sessionStrategy")
    public HttpSessionStrategy defaultSessionStrategy() {
        final CookieHttpSessionStrategy result = new CookieHttpSessionStrategy();
        result.setCookieName("TEST_SESSION_COOKIE");
        return result;
    }

    @Bean(name = { "defaultMapSessionRepository", "sessionRepository" })
    public SessionRepository defaultMapSessionRepository() {
        return new MapSessionRepository();
    }

    @Bean(name = { "defaultSessionFilter", "sessionFilter" })
    public Filter sessionFilter(final SessionRepository<ExpiringSession> sessionRepository, final HttpSessionStrategy sessionStrategy) {

        SessionRepositoryFilter<ExpiringSession> sessionRepositoryFilter = new SessionRepositoryFilter<>(sessionRepository);
        sessionRepositoryFilter.setHttpSessionStrategy(sessionStrategy);

        CompositeFilter compositeFilter = new CompositeFilter();
        compositeFilter.setFilters(Arrays.asList(sessionRepositoryFilter));

        return compositeFilter;
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
