package com.CodeEvalCrew.AutoScore.utils;

public class PathUtil {

    //Sql server
    public static final String DB_URL = "jdbc:sqlserver://MSI\\SQLSERVER;databaseName=master;user=sa;password=123456;encrypt=false;trustServerCertificate=true;";
    public static final String DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    public static final String DB_SERVER = "192.168.1.223\\SQLSERVER";
    public static final String DB_UID = "sa";
    public static final String DB_PWD = "123456";

    //Docker
    public static final String DOCKER_DESKTOP_PATH = "C:\\Program Files\\Docker\\Docker\\Docker Desktop.exe";

    //Newman
    public static final String NEWMAN_CMD_PATH = "C:\\Users\\nhatt\\AppData\\Roaming\\npm\\newman.cmd";

    // Cấu hình khác
    public static final int BASE_PORT = 10000;
    public static final String DIRECTORY_PATH = "C:\\Project\\AutoScore\\Grading";

    //Giới hạn memory docker
    public static final String CONFIG_MEMORY_PROCESSOR = "C:\\Users\\Admin\\.wslconfig";

}
