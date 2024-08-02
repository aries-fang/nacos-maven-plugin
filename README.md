# Nacos 配置检查 Maven 插件

这个 Maven 插件用于检查本地配置文件与 Nacos 配置管理系统中的配置是否一致。它特别适用于微服务架构中，确保本地与远程配置的一致性。

## 功能特点

- **流水线门禁**：结合jenkins流水线使用，可以达到Nacos配置与本地配置不一致时，自动失败的效果。有效放置开发人员忘记提交配置导致线上服务不可用的问题。
- **本地与远程配置比较**：检查本地 YAML 配置文件与 Nacos 中存储的配置是否一致。
- **支持加解密**：使用 Jasypt 处理加密值。
- **详细日志记录**：在比较过程中记录所有发现的不一致之处。

## 安装

在你的 Maven 项目中，添加以下内容到 `pom.xml` 文件中：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.aspirecn.nj</groupId>
            <artifactId>nacos-config-check-maven-plugin</artifactId>
            <version>1.0.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>nacos-config-check</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <encryptPassword>your-encryption-password</encryptPassword>
                <serverUrl>http://nacos-server-url:8848</serverUrl>
                <username>nacos-username</username>
                <password>nacos-password</password>
                <namespace>your-namespace</namespace>
                <dataId>your-dataId</dataId>
                <group>your-group</group>
                <configPath>path/to/your/local/config.yaml</configPath>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## 配置说明

该插件接受以下参数配置：

- `encryptPassword`：（可选）用于解密配置中加密值的密码。默认为 `4df98cad061444e6adb3a703876ec01b`
- `serverUrl`：**（必填）** Nacos 服务器的 URL 地址。
- `username`：**（必填）** Nacos 登录用户名。
- `password`：**（必填）** Nacos 登录密码。
- `namespace`：（可选）需要检查的 Nacos 命名空间。
- `dataId`：**（必填）** Nacos 中配置的 Data ID。
- `group`：（可选）Nacos 的分组名称，默认值为 `DEFAULT_GROUP`。
- `configPath`：**（必填）** 本地 YAML 配置文件的路径。

## 使用方法

在你的 Maven 项目中运行以下命令来执行插件：

```bash
mvn com.aspirecn.nj:nacos-config-check-maven-plugin:nacos-config-check
```

这将触发插件，将 `configPath` 指定的本地配置文件与 Nacos 中存储的远程配置进行比较。

```bash
mvn com.aspirecn.nj:nacos-config-check-maven-plugin:jasypt-encryptor
```
用于将明文密码加密，默认加密秘钥为4df98cad061444e6adb3a703876ec01b，你可以更改它。
或者使用java代码自主生成对应的秘钥

```java
    import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

    /**
     *  加密密码
     * @param encryptedPassword 加盐值
     * @param password 明文密码
     * @return 加密密码
     */
    private static String encryptPassword(String encryptedPassword, String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(encryptedPassword);
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        return encryptor.encrypt(password);
    }

    public static void main(String[] args) {
        System.out.println(encryptPassword("4df98cad061444e6adb3a703876ec01b", "123456"));
    }
```

## 依赖

该插件依赖以下库：

- [Hutool](https://hutool.cn/) - 用于集合、文件处理等的实用工具库。
- [Jasypt](https://www.jasypt.org/) - 用于处理加密值的 Java 简化加密库。
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) - YAML 解析和生成库。

请确保这些依赖在项目的类路径中可用。

## 贡献

欢迎贡献代码！请 fork 本仓库并提交你的改进内容的 pull request。

对于重大变更，请先开启一个 issue 以讨论你想要做出的更改。

---

这个 `README.md` 提供了如何在 Maven 项目中安装、配置和使用 `NacosConfigCheckMojo` 插件的清晰指南。你可以根据具体需求或组织标准进一步定制它。