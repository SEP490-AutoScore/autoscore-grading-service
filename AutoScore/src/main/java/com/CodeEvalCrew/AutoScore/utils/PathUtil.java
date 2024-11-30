package com.CodeEvalCrew.AutoScore.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathUtil {

    // Sql server
    public static final String DB_URL = "jdbc:sqlserver://MSI\\SQLSERVER;databaseName=master;user=sa;password=123456;encrypt=false;trustServerCertificate=true;";
    public static final String DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    public static final String DB_SERVER = "AUTOMATIC\\SQLSERVER";

    public static final String DB_UID = "sa";
    public static final String DB_PWD = "123456";

    // Docker
    public static final String DOCKER_DESKTOP_PATH = "AUTOMATIC";

    public static final String MEMORY_MEGA_BYTE = "4096"; 
    public static final String PROCESSORS = "4";
    public static final String CONFIG_MEMORY_PROCESSOR = "AUTOMATIC";
    public static final String PATH_FILE_CONFIG = "AUTOMATIC";

 // public static final String PATH_FILE_CONFIG = "C:\\Users\\Admin\\.wslconfig";
    // Newman
    public static final String NEWMAN_CMD_PATH = "AUTOMATIC";

    // Cấu hình khác
    public static final int BASE_PORT = 10000;
    // public static final String DIRECTORY_PATH =
    // "C:\\Project\\AutoScore\\Grading";



    public static String getNewmanCmdPath() {
        if (!"AUTOMATIC".equalsIgnoreCase(NEWMAN_CMD_PATH)) {
            return NEWMAN_CMD_PATH;
        }

        try {
            // Execute the 'where newman' command to find the path
            Process process = new ProcessBuilder("where", "newman").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".cmd") || line.endsWith(".exe")) {
                    return line.trim(); // Return the first valid path
                }
            }
        } catch (IOException e) {
            System.err.println("Error while trying to locate newman: " + e.getMessage());
        }

        // Default to existing NEWMAN_CMD_PATH if 'where' command fails
        return NEWMAN_CMD_PATH;
    }

    public static String getDbServer() {
        if (!DB_SERVER.startsWith("AUTOMATIC")) {
            return DB_SERVER;
        }

        try {
            // Chạy lệnh ipconfig
            Process process = new ProcessBuilder("ipconfig").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            String ethernetIp = null;
            String wifiIp = null;

            // Mẫu regex để lấy địa chỉ IPv4
            Pattern ipv4Pattern = Pattern.compile("IPv4 Address.*: (\\d+\\.\\d+\\.\\d+\\.\\d+)");

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Tìm địa chỉ IPv4 từ Ethernet
                if (line.startsWith("Ethernet adapter Ethernet")) {
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = ipv4Pattern.matcher(line.trim());
                        if (matcher.find()) {
                            ethernetIp = matcher.group(1);
                            break;
                        }
                    }
                }

                // Tìm địa chỉ IPv4 từ Wi-Fi
                if (line.startsWith("Wireless LAN adapter Wi-Fi")) {
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = ipv4Pattern.matcher(line.trim());
                        if (matcher.find()) {
                            wifiIp = matcher.group(1);
                            break;
                        }
                    }
                }
            }

            // Ưu tiên Ethernet, nếu không có thì dùng Wi-Fi
            String ipAddress = ethernetIp != null ? ethernetIp : wifiIp;

            if (ipAddress != null) {
                // Kết hợp địa chỉ IP với phần hậu tố "\\SQLSERVER"
                String suffix = DB_SERVER.substring("AUTOMATIC".length());
                return ipAddress + suffix;
            } else {
                throw new RuntimeException("Could not determine IP address for DB_SERVER.");
            }

        } catch (IOException e) {
            throw new RuntimeException("Error while trying to determine DB_SERVER automatically.", e);
        }
    }

    public static String getDockerDesktopPath() {
        if (!"AUTOMATIC".equals(DOCKER_DESKTOP_PATH)) {
            return DOCKER_DESKTOP_PATH;
        }

        // Các đường dẫn phổ biến của Docker Desktop
        String[] commonPaths = {
                "C:\\Program Files\\Docker\\Docker\\Docker Desktop.exe",
                "C:\\Program Files (x86)\\Docker\\Docker\\Docker Desktop.exe"
        };

        // Kiểm tra các đường dẫn mặc định
        for (String path : commonPaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return path;
            }
        }

        // Nếu không tìm thấy trong các đường dẫn mặc định, báo lỗi
        throw new RuntimeException("Docker Desktop.exe not found in default locations.");
    }

    public static String getConfigFilePath() {
        String configFilePath = PATH_FILE_CONFIG;

        if (configFilePath.equals("AUTOMATIC")) {
            // Automatically determine the path for the config file
            // For example, using the user's home directory or project directory
            configFilePath = System.getProperty("user.home") + File.separator + ".wslconfig"; // Or change this to your desired default path
            System.out.println("Auto-configuring file path: " + configFilePath);
        } else {
            // If PATH_FILE_CONFIG is not "AUTOMATIC", use the provided path
            System.out.println("Using provided config file path: " + configFilePath);
        }

        return configFilePath;
    }

    // Method to get CONFIG_MEMORY_PROCESSOR or auto-configure if it's AUTOMATIC
    public static void getConfigMemoryProcessor() {
        // Get the path for the config file
        String configFilePath = getConfigFilePath();

        if (CONFIG_MEMORY_PROCESSOR.equals("AUTOMATIC")) {
            // If CONFIG_MEMORY_PROCESSOR is "AUTOMATIC", delete the config file if it exists
            try {
                File configFile = new File(configFilePath);
                if (configFile.exists()) {
                    Files.delete(Paths.get(configFilePath));
                    System.out.println("Deleted existing config file: " + configFilePath);
                }
            } catch (IOException e) {
                System.err.println("Error while deleting the config file: " + e.getMessage());
            }
        } else if (CONFIG_MEMORY_PROCESSOR.isEmpty()) {
            // If CONFIG_MEMORY_PROCESSOR is empty, create/update the config file with MEMORY_MEGA_BYTE and PROCESSORS
            try {
                File configFile = new File(configFilePath);
                if (!configFile.exists()) {
                    configFile.createNewFile();
                    System.out.println("Config file created: " + configFilePath);
                }

                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write("[wsl2]\n");
                    writer.write("memory=" + MEMORY_MEGA_BYTE + "MB\n");
                    writer.write("processors=" + PROCESSORS + "\n");
                    System.out.println(".wslconfig file created/updated with new configuration.");
                }
            } catch (IOException e) {
                System.err.println("Error while creating or updating config file: " + e.getMessage());
            }
        }
    }
}
