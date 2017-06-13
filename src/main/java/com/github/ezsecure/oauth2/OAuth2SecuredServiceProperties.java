package com.github.ezsecure.oauth2;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by youssefguenoun on 09/06/2017.
 */
@ConfigurationProperties(prefix = "auth")
@Setter
@Getter
public class OAuth2SecuredServiceProperties {
    private Server server = new Server();
    private Service service = new Service();

    @Setter
    @Getter
    public static class Server {
        private AuthorizationServerEndpoints authorizationServerEndpoints = new AuthorizationServerEndpoints();
        private String resourceId;
        private String resourceSecret;

    }

    @Setter
    @Getter
    public static class AuthorizationServerEndpoints {

        private String introspect;
        private String userInfo;
    }

    @Setter
    @Getter
    public static class Service {
        private List<String> scopes = new ArrayList<>();
        private List<String> unprotectedpaths = new ArrayList<>();
        private List<CustomHeader> customHeaders = new ArrayList<>();
        private CommonUserPermissionsRules commonUserPermissionsRules = new CommonUserPermissionsRules();
        private XFrameOptions xframeOptions = new XFrameOptions();
    }

    @Setter
    @Getter
    public static class XFrameOptions{
        private XFrameOptionsMode xframeOptionsMode = XFrameOptionsMode.SAMEORIGIN;
        private List<String> allowedOrigins = new ArrayList<>();
    }

    /**
     * The possible values for the X-Frame-Options header.
     */
    public enum XFrameOptionsMode {
        DENY("DENY"), SAMEORIGIN("SAMEORIGIN"), ALLOW_FROM("ALLOW-FROM");

        private String mode;

        private XFrameOptionsMode(String mode) {
            this.mode = mode;
        }

        /**
         * Gets the mode for the X-Frame-Options header value. For example, DENY,
         * SAMEORIGIN, ALLOW-FROM. Cannot be null.
         *
         * @return the mode for the X-Frame-Options header value.
         */
        private String getMode() {
            return mode;
        }
    }

    @Setter
    @Getter
    public static class CommonUserPermissionsRules {
        private boolean enabled = false;
        private List<ProtectedPath> protectedPaths = new ArrayList<>();
    }

    @Setter
    @Getter
    public static class ProtectedPath{
        private String path;
        private String permission;
    }

    @Setter
    @Getter
    public static class CustomHeader{
        private String headerName;
        private String headerValue;
    }
}