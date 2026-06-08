package com.securetransfer.platform.security;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MfaService {

    private static final String SECRET_ALGORITHM = "HmacSHA1";
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int SECRET_SIZE = 20; // 20 bytes = 160 bits

    // Générer un secret TOTP unique (clé secrète pour l'utilisateur)
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_SIZE];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    // Générer l'URL pour Google Authenticator (format otpauth://)
    public String getGoogleAuthenticatorUrl(String secret, String email, String issuer) {
        try {
            String encodedSecret = URLEncoder.encode(secret, StandardCharsets.UTF_8.toString());
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
            String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8.toString());

            return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                    encodedIssuer, encodedEmail, encodedSecret, encodedIssuer, CODE_DIGITS, TIME_STEP_SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération URL MFA", e);
        }
    }

    // Générer un QR code à partir de l'URL
    public byte[] generateQrCode(String url, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Erreur génération QR code", e);
        }
    }

    // Vérifier le code TOTP entré par l'utilisateur
    public boolean verifyCode(String secret, String code) {
        try {
            long currentTime = System.currentTimeMillis() / 1000;
            long timeWindow = currentTime / TIME_STEP_SECONDS;

            // Vérifier la fenêtre actuelle et la fenêtre précédente (tolérance de 30 secondes)
            return generateTOTP(secret, timeWindow).equals(code) ||
                    generateTOTP(secret, timeWindow - 1).equals(code);
        } catch (Exception e) {
            return false;
        }
    }

    // Générer le code TOTP à 6 chiffres
    private String generateTOTP(String secret, long counter) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] key = Base64.getDecoder().decode(secret);
        SecretKeySpec signingKey = new SecretKeySpec(key, SECRET_ALGORITHM);

        Mac mac = Mac.getInstance(SECRET_ALGORITHM);
        mac.init(signingKey);

        byte[] counterBytes = new byte[8];
        for (int i = 8; i-- > 0; counter >>>= 8) {
            counterBytes[i] = (byte) counter;
        }

        byte[] hash = mac.doFinal(counterBytes);

        int offset = hash[hash.length - 1] & 0xF;
        int truncatedHash = ((hash[offset] & 0x7F) << 24) |
                ((hash[offset + 1] & 0xFF) << 16) |
                ((hash[offset + 2] & 0xFF) << 8) |
                (hash[offset + 3] & 0xFF);

        int codeValue = truncatedHash % (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", codeValue);
    }
}