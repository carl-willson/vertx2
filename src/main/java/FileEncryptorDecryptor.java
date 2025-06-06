import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class FileEncryptorDecryptor {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java FileEncryptorDecryptor <password> <filename> <encrypt|decrypt>");
            return;
        }

        String password = args[0];
        String inputFile = args[1];
        String operation = args[2].toLowerCase();
        String outputFile;

        if (operation.equals("encrypt")) {
            outputFile = inputFile + ".encrypted";
        } else if (operation.equals("decrypt")) {
            outputFile = inputFile.replace(".encrypted", ".decrypted");
        } else {
            System.out.println("Invalid operation. Use 'encrypt' or 'decrypt'.");
            return;
        }

        try {
            if (operation.equals("encrypt")) {
                encryptFile(password, inputFile, outputFile);
                System.out.println("File encrypted successfully to: " + outputFile);
            } else {
                decryptFile(password, inputFile, outputFile);
                System.out.println("File decrypted successfully to: " + outputFile);
            }
        } catch (Exception e) {
            System.err.println("Error during " + operation + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void encryptFile(String password, String inputFilePath, String outputFilePath) throws Exception {
        // Generate a key from the password using SHA-256
        byte[] key = password.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // Use only first 128 bits for AES-128
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        // Initialize cipher for encryption
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Read input file
        File inputFile = new File(inputFilePath);
        byte[] inputBytes = new byte[(int) inputFile.length()];
        try (FileInputStream inputStream = new FileInputStream(inputFile)) {
            inputStream.read(inputBytes);
        }

        // Encrypt the file content
        byte[] outputBytes = cipher.doFinal(inputBytes);

        // Write to output file
        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            outputStream.write(outputBytes);
        }
    }

    private static void decryptFile(String password, String inputFilePath, String outputFilePath) throws Exception {
        // Generate a key from the password using SHA-256
        byte[] key = password.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // Use only first 128 bits for AES-128
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Read input file
        File inputFile = new File(inputFilePath);
        byte[] inputBytes = new byte[(int) inputFile.length()];
        try (FileInputStream inputStream = new FileInputStream(inputFile)) {
            inputStream.read(inputBytes);
        }

        // Decrypt the file content
        byte[] outputBytes = cipher.doFinal(inputBytes);

        // Write to output file
        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            outputStream.write(outputBytes);
        }
    }
}