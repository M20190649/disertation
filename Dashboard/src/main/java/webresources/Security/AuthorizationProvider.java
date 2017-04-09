package webresources.Security;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import webresources.Users.UserDao;
import webresources.Users.UserResource;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class AuthorizationProvider {

    private final UserDao userDao;

    private final String keyString = "qkjll5@2md3gs5Q1";
    private final byte[] IV = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    @Autowired
    public AuthorizationProvider(UserDao userDao) {
        this.userDao = userDao;
    }

    public String generateAccessToken(UserResource user) {
        String accessTokenEncoded = null;

        String accessTokenString = user.email + ":" + user.getPassword();

        System.out.println("accessTokenString: " + accessTokenString);

        try {
            byte[] keyValue = keyString.getBytes(StandardCharsets.UTF_8);
            Key key = new SecretKeySpec(keyValue, "AES");
            Cipher c1 = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c1.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV));

            System.out.println("Access token string " + accessTokenString + " key " + keyString);

            byte[] encVal = c1.doFinal(accessTokenString.getBytes(StandardCharsets.UTF_8));

            System.out.println("encval length: " + encVal.length);

            byte[] data = Base64.encodeBase64(encVal);
            accessTokenEncoded =  new String(data);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return accessTokenEncoded;
    }

    public UserResource getUserContenxt(String accessToken) {

        System.out.println("Acess token: " + accessToken);

        String decriptedAccessToken = null;
        byte[] accessTokenData = Base64.decodeBase64(accessToken);

        System.out.println("Acess token data lengtj: " + accessTokenData.length);

        try {
            byte[] keyValue = keyString.getBytes(StandardCharsets.UTF_8);
            Key key = new SecretKeySpec(keyValue, "AES");
            Cipher c1 = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c1.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV));

            byte[] decVal = c1.doFinal(accessTokenData);

            decriptedAccessToken = new String(decVal, StandardCharsets.UTF_8);
        } catch(Exception ex) {
            return null;
        }

        System.out.println("Decrypted acess token: " + decriptedAccessToken);

        String email = decriptedAccessToken.split(":")[0];
        String password = decriptedAccessToken.split(":")[1];

        UserResource user = userDao.findByEmailAndPassword(email, password);

        return user;
    }
}
