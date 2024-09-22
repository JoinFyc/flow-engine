package com.wflow.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.wflow.bean.vo.FileResourceVo;
import com.wflow.exception.BusinessException;
import com.wflow.service.FileManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 本地实现的文件管理服务
 * @author : JoinFyc
 * @date : 2024/9/7
 */
@Slf4j
@Service
public class LocalFileManagerServiceImpl implements FileManagerService {

    @Value("${wflow.file.max-size:20}")
    private Integer maxSize;

    //默认文件存储路径
    private static final String BASE_DIR_FILE = "resource/file";
    private static final String BASE_SIGN_FILE = "resource/sign";
    //操作系统信息
    private final static String OS = System.getProperty("os.name").toLowerCase();

    @Override
    public FileResourceVo uploadFile(MultipartFile file, Boolean isImg, Boolean isSign) throws IOException {
        if (file.getSize() / 1048576 > maxSize){
            throw new BusinessException("管理员限制了最大文件大小为" + maxSize + "MB");
        }
        String name = file.getOriginalFilename();
        String md5 = SecureUtil.md5(file.getInputStream()).substring(8, 24);
        String type = name.substring(name.lastIndexOf("."));
        String fullPath = getPathByOs(true, isSign ? BASE_SIGN_FILE : BASE_DIR_FILE, md5) + type;
        FileResourceVo resourceVo = FileResourceVo.builder().id(md5 + type).isImage(isImg)
                .url("/wflow/res/" + md5 + type).size(file.getSize()).name(name).build();
        if (!FileUtil.exist(fullPath)){
            File touch = FileUtil.touch(fullPath);
            file.transferTo(touch);
            log.info("上传文件[{} => {}]成功", name, md5);
        }else {
            log.info("上传文件[{} => {}]成功，文件已存在", name, md5);
        }
        return resourceVo;
    }

    @Override
    public void delFileById(String fileId, Boolean isSign) {
        String path = getPathByOs(true, isSign ? BASE_SIGN_FILE : BASE_DIR_FILE, fileId);
        if (FileUtil.exist(path)){
            FileUtil.del(path);
        }else {
            throw new BusinessException("文件不存在");
        }
        log.info("删除文件[{}]成功", fileId);
    }

    @Override
    public InputStreamResource getFileById(String fileId, String name, Boolean isSign) throws IOException {
        String path = getPathByOs(true, isSign ? BASE_SIGN_FILE : BASE_DIR_FILE, fileId);
        if (FileUtil.exist(path)){
          return new InputStreamResource(Files.newInputStream(Paths.get(path)));
        }else {
            throw new BusinessException("文件不存在");
        }
    }

    /**
     * @param isAb 是否相对路径
     * @param path xx.xx.xx
     * @return 对应系统下路径
     */
    public static synchronized String getPathByOs(boolean isAb, String ...path){
        StringBuilder builder = new StringBuilder(isAb ? System.getProperty("user.dir") : "");
        for (String pt : path) {
            builder.append("/").append(pt);
        }
        if (!isAb){
            builder.deleteCharAt(0);
        }
        return OS.startsWith("win") ?
                builder.toString().replaceAll("/", "\\\\")
                : builder.toString();
    }
}
