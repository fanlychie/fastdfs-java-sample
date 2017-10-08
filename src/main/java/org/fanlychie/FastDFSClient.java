package org.fanlychie;

import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * FastDFS 客户端工具
 * Created by fanlychei on 2017/9/22.
 */
public final class FastDFSClient {

    private static StorageClient1 storageClient;

    /**
     * 上传文件
     *
     * @param file 文件对象
     * @return 上传如果成功, 则返回表示该文件的文件ID字符串; 否则抛出异常
     */
    public static String uploadFile(File file) {
        String name = file.getName();
        int index = name.lastIndexOf(".");
        String extension = index == -1 ? null : name.substring(index + 1);
        byte[] sources;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            sources = new byte[inputStream.available()];
            inputStream.read(sources);
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {}
            }
        }
        return storage(sources, extension);
    }

    /**
     * 存储任意源到文件
     *
     * @param sources 源
     * @return 存储如果成功, 则返回表示该文件的文件ID字符串; 否则抛出异常
     */
    public static String storage(byte[] sources) {
        return storage(sources, null);
    }

    /**
     * 存储任意源到文件
     *
     * @param sources   源
     * @param extension 存储的文件扩展名
     * @return 存储如果成功, 则返回表示该文件的文件ID字符串; 否则抛出异常
     */
    public static String storage(byte[] sources, String extension) {
        try {
            return storageClient.upload_file1(sources, extension, null);
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    /**
     * 更新文件, 更新时, 先上传新的文件, 若上传成功, 则删除原文件; 若新文件上传失败, 则原文件不会被删除, 并抛出异常
     *
     * @param fileId  原文件ID
     * @param newFile 新的文件对象
     * @return 先上传新的文件, 若上传成功, 则删除原文件, 并返回新的文件ID; 若新文件上传失败, 则原文件不会被删除, 并抛出异常
     */
    public static String updateFile(String fileId, File newFile) {
        String newFileId = uploadFile(newFile);
        delete(fileId);
        return newFileId;
    }

    /**
     * 更新源, 更新时, 先存储新的源内容, 若存储成功, 则删除原文件; 若存储失败, 则原文件不会被删除, 并抛出异常
     *
     * @param fileId     原文件ID
     * @param newSources 新的源内容
     * @return 先存储新的源内容, 若存储成功, 则删除原文件, 并返回新的文件ID; 若存储失败, 则原文件不会被删除, 并抛出异常
     */
    public static String modify(String fileId, byte[] newSources) {
        return modify(fileId, newSources, null);
    }

    /**
     * 更新源, 更新时, 先存储新的源内容, 若存储成功, 则删除原文件; 若存储失败, 则原文件不会被删除, 并抛出异常
     *
     * @param fileId     原文件ID
     * @param newSources 新的源内容
     * @param extension 存储的文件扩展名
     * @return 先存储新的源内容, 若存储成功, 则删除原文件, 并返回新的文件ID; 若存储失败, 则原文件不会被删除, 并抛出异常
     */
    public static String modify(String fileId, byte[] newSources, String extension) {
        String newFileId = storage(newSources, extension);
        delete(fileId);
        return newFileId;
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     */
    public static void delete(String fileId) {
        try {
            storageClient.delete_file1(fileId);
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    /**
     * 下载文件
     *
     * @param fileId       文件ID
     * @param outputStream 输出流对象
     */
    public static void downloadFile(String fileId, OutputStream outputStream) {
        byte[] bytes;
        try {
            bytes = storageClient.download_file1(fileId);
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
        InputStream inputStream = new ByteArrayInputStream(bytes);
        try (BufferedInputStream bis = new BufferedInputStream(inputStream);
             BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {
            int read;
            byte[] buffer = new byte[512 * 1024];
            while ((read = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
        } catch (IOException e) {
            if (!e.getClass().getSimpleName().equals("ClientAbortException")) {
                throw new ExceptionWrapper(e);
            }
        }
    }

    /**
     * 下载文件
     *
     * @param response 客户端响应对象
     * @param fileId   文件ID
     * @param fileName 客户端下载框中显示的文件名称, 如果有扩展名, 该名称应含文件的扩展名
     */
    public static void downloadFile(HttpServletResponse response, String fileId, String fileName) {
        try {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
            response.setContentType("application/octet-stream; charset=iso-8859-1");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            downloadFile(fileId, response.getOutputStream());
        } catch (IOException e) {
            throw new ExceptionWrapper(e);
        }
    }

    /**
     * 初始化 FastDFS
     */
    private static void init() {
        try {
            ClientGlobal.initByProperties("fastdfs-client.properties");
            TrackerClient trackerClient = new TrackerClient(ClientGlobal.getG_tracker_group());
            TrackerServer trackerServer = trackerClient.getConnection();
            if (trackerServer == null) {
                throw new FastDFSClientException("连接 FastDFS 服务失败");
            }
            StorageServer storageServer = trackerClient.getStoreStorage(trackerServer);
            if (storageServer == null) {
                throw new FastDFSClientException("获取 FastDFS 存储对象失败");
            }
            storageClient = new StorageClient1(trackerServer, storageServer);
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    /**
     * 第一次加载时执行初始化工作
     */
    static { init(); }

    /**
     * 异常包装器, 将非运行时异常包装成运行时异常并在运行时抛出
     */
    static class ExceptionWrapper extends RuntimeException {

        private Throwable cause;

        public ExceptionWrapper(Throwable throwable) {
            this.cause = throwable;
        }

        @Override
        public synchronized Throwable getCause() {
            return cause;
        }

    }

    /**
     * FastDFS 客户端异常
     */
    public static class FastDFSClientException extends RuntimeException {

        public FastDFSClientException(String message) {
            super(message);
        }

    }

}