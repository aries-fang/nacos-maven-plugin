package cn.aries.fang.maven.plugin;


import cn.aries.fang.maven.plugin.dto.MappingConfig;
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
import org.apache.maven.plugin.MojoFailureException;
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

    @Parameter(property = "configs", required = true)
    private List<MappingConfig> configs;


    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("=========门禁检查启动=========");

        // 获取Nacos的token
        String nacosToken = getNacosToken();
        // 检查配置
        Map<String, List<String>> result = new HashMap<>();
        for (MappingConfig config : configs) {
            List<String> errorKeys = handleMappingConfig(config, nacosToken);
            if (CollectionUtil.isNotEmpty(errorKeys)) {
                result.put(config.getDataId(), errorKeys);
            }
        }
        // 输出结果
        if (CollectionUtil.isEmpty(result.keySet())) {
            getLog().info("=========门禁检查通过=========");
        } else {
            getLog().error("=========门禁检查不通过=========");
            StringBuilder sb = new StringBuilder();
            result.forEach((key, value) -> {
                String errorMessage = key + "以下key缺失：\n" + String.join(",", value);
                sb.append(errorMessage);
                getLog().error(errorMessage);
            });
            throw new MojoExecutionException(sb.toString());
        }
    }

    /**
     * 处理单个配置文件
     *
     * @param config 配置文件信息
     * @throws MojoExecutionException 执行异常
     */
    private List<String> handleMappingConfig(MappingConfig config, String nacosToken) throws MojoExecutionException {
        Yaml yaml = new Yaml();
        Map<String, Object> localYaml = null;
        List<String> localYamlKeys = new ArrayList<>();
        getLog().info("=========加载本地文件：" + config.getConfigPath() + "=========");
        try (InputStream inputStream = FileUtil.getInputStream(new File(config.getConfigPath()))) {
            localYaml = yaml.load(inputStream);
            extractKeys(localYaml, localYamlKeys, "");
        } catch (Exception e) {
            getLog().error("=========门禁检查不通过=========");
            throw new MojoExecutionException("加载" + config.getConfigPath() + "失败", e);
        }

        String url = String.format("%s/nacos/v1/cs/configs?dataId=%s&group=%s&tenant=%s&accessToken=%s", serverUrl, config.getDataId(), config.getGroup(), namespace, nacosToken);
        Map<String, Object> nacosYaml = null;
        List<String> nacosYamlKeys = new ArrayList<>();
        getLog().info("=========加载远程文件：namespace：" + namespace + " dataId：" + config.getDataId() + "=========");
        try {
            // 发送 GET 请求并获取响应
            HttpClientResult response = HttpClientUtils.doGet(url);
            if (response.getCode() == 200) {
                nacosYaml = yaml.load(response.getContent());
                extractKeys(nacosYaml, nacosYamlKeys, "");
            } else {
                getLog().error("=========门禁检查不通过=========");
                throw new MojoExecutionException("下载nacos文件失败，dataId:" + config.getDataId() + "，HTTP Status: " + response.getCode() + "，content:" + response.getContent());
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            getLog().error("=========门禁检查不通过=========");
            throw new MojoExecutionException("下载nacos文件失败，dataId:" + config.getDataId(), e);
        }
        return nacosYamlKeys.stream().filter(key -> !localYamlKeys.contains(key)).collect(Collectors.toList());
    }

    /**
     * 获取nacos token
     *
     * @return token
     * @throws MojoExecutionException 异常
     */
    private String getNacosToken() throws MojoExecutionException {
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
        return accessToken;
    }

    /**
     * 递归方法，提取所有的key
     *
     * @param map       nacos的yaml
     * @param keys      提取的key集合
     * @param parentKey 父key
     */
    private void extractKeys(Map<String, Object> map, List<String> keys, String parentKey) {
        if (CollectionUtil.isEmpty(map)) {
            return;
        }
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

