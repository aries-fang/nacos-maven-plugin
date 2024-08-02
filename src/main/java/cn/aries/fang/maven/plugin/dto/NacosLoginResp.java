package cn.aries.fang.maven.plugin.dto;


public class NacosLoginResp {

    private String accessToken;

    private String tokenTtl;

    private String globalAdmin;

    private String username;


    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(String tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public String getGlobalAdmin() {
        return globalAdmin;
    }

    public void setGlobalAdmin(String globalAdmin) {
        this.globalAdmin = globalAdmin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
