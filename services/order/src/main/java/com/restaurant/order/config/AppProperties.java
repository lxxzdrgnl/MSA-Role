package com.restaurant.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String menuServiceUrl;
    private String gatewayUrl;

    public String getMenuServiceUrl() { return menuServiceUrl; }
    public void setMenuServiceUrl(String menuServiceUrl) { this.menuServiceUrl = menuServiceUrl; }

    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }
}
