package com.github.ezsecure.oauth2;

import org.mitre.oauth2.introspectingfilter.IntrospectingTokenService;
import org.mitre.oauth2.introspectingfilter.service.impl.StaticIntrospectionConfigurationService;
import org.mitre.oauth2.model.RegisteredClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.frameoptions.WhiteListedAllowFromStrategy;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Boot auto-configuration class to setup an Ouath2 Resource Server
 *
 * This implementation supports MitreId connect Authorization server
 *
 * @author youssefguenoun
 */
@Configuration
@EnableResourceServer
@EnableConfigurationProperties(OAuth2SecuredServiceProperties.class)
@Order(OAuth2ResourceAutoConfiguration.ORDER)
public class OAuth2ResourceAutoConfiguration extends ResourceServerConfigurerAdapter {

    //public static final int ORDER = 0;
    public static final int ORDER = SecurityProperties.ACCESS_OVERRIDE_ORDER;

    private final OAuth2SecuredServiceProperties properties;


    @Autowired
    public OAuth2ResourceAutoConfiguration(OAuth2SecuredServiceProperties properties) {
        this.properties = properties;
    }


    @Override
    public void configure(HttpSecurity http) throws Exception {

        List<String> scopes = properties.getService().getScopes();

        // Customize the population strategy of the Http Header Frame-Options
        configureHttpHeaderFrameOptions(http);

        // Allow access to unprotected paths
        configureUnprotectedPathsAccessControlRules(http);

        // No security restriction for static resources : swagger ui & spring fox
        configureStaticResourcesAccessControlRules(http);

        // Check default scopes for all requests
        configureAllRequestWithDefaultScopes(http, scopes);

        // provides hooks to enable adding custom headers
        configureCustomStaticHeaders(http);

    }

    /**
     * Check default scopes for all requests
     *
     * @param http the Spring <code>{@link HttpSecurity}</code> configuration class
     * @param scopes <<code>List</code> of String representing the global oauth2 scopes for the service
     * @throws Exception if any problem occurs
     */
    private void configureAllRequestWithDefaultScopes(HttpSecurity http, List<String> scopes) throws Exception {
        if (!scopes.isEmpty()) {
            http.authorizeRequests().
                    anyRequest().
                    access(scopes.stream().map(scope -> "#oauth2.hasScope('" + scope + "')").collect(Collectors.joining(" and ")));
        }
    }


    /**
     * No security restriction for static resources : swagger ui & spring fox
     *
     * @param http the Spring <code>{@link HttpSecurity}</code> configuration class
     * @throws Exception if any problem occurs
     */
    private void configureStaticResourcesAccessControlRules(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/v2/api-docs/**").permitAll();
        http.authorizeRequests().antMatchers("/swagger-resources/**").permitAll();
        http.authorizeRequests().antMatchers("/swagger-ui.html").permitAll();
        http.authorizeRequests().antMatchers("/webjars/**/*").permitAll();
        http.authorizeRequests().antMatchers("/swagger-resources/webjars/**").permitAll();
    }

    /**
     * Allow access to unprotected paths
     *
     * @param http http the Spring <code>{@link HttpSecurity}</code> configuration class
     * @throws Exception if any problem occurs
     */
    private void configureUnprotectedPathsAccessControlRules(HttpSecurity http) throws Exception {
        List<String> unprotectedPaths = properties.getService().getUnprotectedpaths();
        if (!unprotectedPaths.isEmpty()) {
            http.authorizeRequests().
                    antMatchers(unprotectedPaths.toArray(new String[unprotectedPaths.size()])).
                    permitAll();
        }
    }

    /**
     * Allow the customization of the Http Header Frame-Options strategy population
     *
     * @param http http the Spring <code>{@link HttpSecurity}</code> configuration class
     * @throws Exception if any problem occurs
     */
    private void configureHttpHeaderFrameOptions(HttpSecurity http) throws Exception {
        switch (properties.getService().getXframeOptions().getXframeOptionsMode()){
            case SAMEORIGIN:
                http.headers().frameOptions().sameOrigin();
                break;
            case DENY:
                http.headers().frameOptions().deny();
                break;
            case ALLOW_FROM:
                http.headers().frameOptions().disable().addHeaderWriter(new XFrameOptionsHeaderWriter(new WhiteListedAllowFromStrategy(properties.getService().getXframeOptions().getAllowedOrigins())));
                break;
            default: http.headers().frameOptions().sameOrigin();
        }
    }

    /**
     * Provides hooks to enable adding custom headers
     *
     * @param http http http the Spring <code>{@link HttpSecurity}</code> configuration class
     *
     * @throws Exception if any problem occurs
     *
     */
    private void configureCustomStaticHeaders(HttpSecurity http) throws Exception {
        List<OAuth2SecuredServiceProperties.CustomHeader> customHeaders = properties.getService().getCustomHeaders();

        if(!customHeaders.isEmpty()){
            for (OAuth2SecuredServiceProperties.CustomHeader customHeader : customHeaders){
                http.headers().addHeaderWriter(new StaticHeadersWriter(customHeader.getHeaderName(),customHeader.getHeaderValue()));
            }
        }
    }

    @Bean
    public ResourceServerTokenServices remoteTokenServices() {

        IntrospectingTokenService introspectingTokenService = new IntrospectingTokenService();
        introspectingTokenService.setIntrospectionConfigurationService(staticIntrospectionConfigurationService());
        return introspectingTokenService;
    }

    @Bean
    public StaticIntrospectionConfigurationService staticIntrospectionConfigurationService(){
        StaticIntrospectionConfigurationService staticIntrospectionConfigurationService = new StaticIntrospectionConfigurationService();

        RegisteredClient registeredClient = new RegisteredClient();
        registeredClient.setClientId(properties.getServer().getResourceId());
        registeredClient.setClientSecret(properties.getServer().getResourceSecret());

        staticIntrospectionConfigurationService.setIntrospectionUrl(properties.getServer().getAuthorizationServerEndpoints().getIntrospect());
        staticIntrospectionConfigurationService.setClientConfiguration(registeredClient);

        return staticIntrospectionConfigurationService;
    }

}