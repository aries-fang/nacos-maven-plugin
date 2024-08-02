package cn.aries.fang.maven.plugin;


import cn.aries.fang.maven.plugin.utils.HttpClientResult;
import cn.aries.fang.maven.plugin.utils.HttpClientUtils;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import cn.aries.fang.maven.plugin.dto.NacosLoginResp;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.yaml.snakeyaml.Yaml;


@Mojo(name = "nacos-config-check")
public class NacosConfigCheckMojo extends AbstractMojo {

    @Parameter(property = "jasypt.encryptor.password", defaultValue = "4df98cad061444e6adb3a703876ec01b")
    private String encryptPassword;

    @Parameter(property = "nacos.serverUrl", required = true)
    private String serverUrl;

    @Parameter(property = "nacos.username", required = true)
    private String username;

    @Parameter(property = "nacos.password", required = true)
    private String password;

    @Parameter(property = "nacos.namespace", defaultValue = "")
    private String namespace;

    @Parameter(property = "nacos.dataId", required = true)
    private String dataId;

    @Parameter(property = "nacos.group", defaultValue = "DEFAULT_GROUP")
    private String group;

    @Parameter(property = "configPath", required = true)
    private String configPath;

    public void execute() throws MojoExecutionException {

        getLog().info("=========门禁检查启动=========");
        Yaml yaml = new Yaml();
        Map<String, Object> localYaml = null;
        List<String> localYamlKeys = new ArrayList<>();
        try (InputStream inputStream = FileUtil.getInputStream(new File(configPath))) {
            localYaml = yaml.load(inputStream);
            extractKeys(localYaml, localYamlKeys, "");
        } catch (Exception e) {
            getLog().error("=========门禁检查不通过=========");
            throw new MojoExecutionException("加载" + configPath + "失败", e);
        }

        // 登录nacos
        String nacosPwd = decryptPassword(encryptPassword, password);
        String loginUrl = String.format("%s/nacos/v1/auth/users/login", serverUrl);

        // 设置form-data的请求体
        HashMap<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", nacosPwd);

        // 将请求头和请求体封装到HttpEntity中
        String accessToken = "";
        try {
            HttpClientResult loginResp = HttpClientUtils.postFormBody(loginUrl, body);
            if (loginResp.getCode() == 200) {
                accessToken = JSONUtil.toBean(loginResp.getContent(), NacosLoginResp.class).getAccessToken();
            } else {
                getLog().error("=========门禁检查不通过=========");
                throw new MojoExecutionException("登录nacos失败. HTTP Status: " + loginResp.getCode());
            }
        } catch (Exception e) {
            getLog().error("=========门禁检查不通过=========");
            throw new MojoExecutionException("登录nacos失败：", e);
        }

        String url = String.format("%s/nacos/v1/cs/configs?dataId=%s&group=%s&tenant=%s&accessToken=%s", serverUrl,
                dataId, group, namespace, accessToken);
        Map<String, Object> nacosYaml = null;
        List<String> nacosYamlKeys = new ArrayList<>();
        try {
            // 发送 GET 请求并获取响应
            HttpClientResult response = HttpClientUtils.doGet(url);
            if (response.getCode() == 200) {
                nacosYaml = yaml.load(response.getContent());
                extractKeys(nacosYaml, nacosYamlKeys, "");
            } else {
                getLog().error("=========门禁检查不通过=========");
                throw new MojoExecutionException(
                        "下载nacos文件失败，dataId:" + dataId + "，HTTP Status: " + response.getCode() + "，content:"
                                + response.getContent());
            }
        } catch (Exception e) {
            getLog().error("=========门禁检查不通过=========");
            throw new MojoExecutionException("下载nacos文件失败，dataId:" + dataId, e);
        }

        Map<String, Object> finalLocalYaml = localYaml;
        List<String> errorKeys = nacosYamlKeys.stream().filter(key -> !finalLocalYaml.containsKey(key))
                .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(errorKeys)) {
            getLog().info("=========门禁检查通过=========");
        } else {
            getLog().error("=========门禁检查不通过=========");
            getLog().error("以下key缺失：\n" + String.join(",", errorKeys));
            throw new MojoExecutionException("以下key缺失：\n" + String.join(",", errorKeys));
        }
    }

    /**
     * 递归方法，提取所有的key
     * @param map  nacos的yaml
     * @param keys 提取的key集合
     * @param parentKey 父key
     */
    private static void extractKeys(Map<String, Object> map, List<String> keys, String parentKey) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String currentKey = parentKey.isEmpty() ? entry.getKey() : parentKey + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                extractKeys((Map<String, Object>) entry.getValue(), keys, currentKey);
            } else {
                keys.add(currentKey);
            }
        }
    }

    /**
     * 解密密码
     *
     * @param encryptedPassword 加盐值
     * @param jasyptPassword    加密密码
     * @return 明文密码
     */
    private String decryptPassword(String encryptedPassword, String jasyptPassword) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(encryptedPassword);
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        return encryptor.decrypt(jasyptPassword);
    }
}

