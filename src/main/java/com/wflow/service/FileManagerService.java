package com.wflow.service;

import com.wflow.bean.vo.FileResourceVo;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author : JoinFyc
 * @date : 2024/9/7
 */
public interface FileManagerService {

    /**
     * 上传文件、图片
     * @param file 文件/图片
     * @param isImg 是否为图片
     */
    FileResourceVo uploadFile(MultipartFile file, Boolean isImg, Boolean isSign) throws IOException;

    /**
     * 删除文件
     * @param fileId 文件ID
     */
    void delFileById(String fileId, Boolean isSign);

    /**
     * 通过id获取文件流
     * @param fileId 文件ID
     * @param name 文件名称
     * @param isSign 是否为签名图片
     */
    InputStreamResource getFileById(String fileId, String name, Boolean isSign) throws IOException;
}
