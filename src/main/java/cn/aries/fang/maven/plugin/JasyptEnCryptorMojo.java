package cn.aries.fang.maven.plugin;

import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.*;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;


/**
 * 密码加密插件
 */
@Mojo(name = "jasypt-encryptor")
public class JasyptEnCryptorMojo extends AbstractMojo {

    @Parameter(property = "jasypt.encryptor.password", defaultValue = "4df98cad061444e6adb3a703876ec01b")
    private String encryptPassword;

    @Parameter(property = "nacos.password", required = true)
    private String password;


    public void execute() throws MojoExecutionException {
        String encryptedPassword = encryptPassword(encryptPassword, password);
        getLog().info("=========encryptedPassword:\n" + encryptedPassword + "=========");
    }

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
}

