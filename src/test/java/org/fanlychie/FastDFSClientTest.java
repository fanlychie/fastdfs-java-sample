package org.fanlychie;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class FastDFSClientTest {

    /**
     * 测试文件上传
     */
    @Test
    public void testUploadFile() {
        File file = new File("src/test/resources/demo.txt");
        String fileID = FastDFSClient.uploadFile(file);
        System.out.println(fileID);

        file = new File("src/test/resources/demo.txt");
        fileID = FastDFSClient.uploadFile(file);
        System.out.println(fileID);
    }

    /**
     * 测试内容存储
     */
    @Test
    public void testStorage() {
        String fileID = FastDFSClient.storage("Hello World!!!".getBytes());
        System.out.println(fileID);
    }

    /**
     * 测试内容修改更新, 文件更新使用 updateFile
     */
    @Test
    public void testModify() {
        String fileID = FastDFSClient.modify("group1/M00/00/00/wKiLgVnHDwOAdxolAAAADtiRrk0132.txt", "Hello FastDFS!!!".getBytes());
        System.out.println(fileID);
    }

    /**
     * 测试文件下载
     */
    @Test
    public void testDownloadFile() throws Exception {
        OutputStream os = new FileOutputStream("src/test/resources/demo-download.txt");
        FastDFSClient.downloadFile("group1/M00/00/00/wKiLgVnHFRKAZDZJAAAAEB_3vnU4400918", os);
    }

}