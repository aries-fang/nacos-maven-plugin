package com.aspirecn.nj.dto;


public class NacosLoginResp {
    // 登录成功后返回的token
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
