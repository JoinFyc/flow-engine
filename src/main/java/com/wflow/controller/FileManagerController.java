package com.wflow.controller;

import com.wflow.service.FileManagerService;
import com.wflow.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件管理服务
 * @author : willian fu
 * @date : 2022/9/7
 */
@RestController
@RequestMapping("wflow/res")
public class FileManagerController {

    @Autowired
    private FileManagerService fileManagerService;

    /**
     * 通过文件ID下载/获取文件/图片
     * 文件ID格式为 文件md5取中间16位 + 文件后缀
     * @param fileId 文件ID
     * @param name 下载的文件名
     * @param isSign 是否为签名，签名的话单独一个文件夹存
     * @return 文件流
     * @throws IOException
     */
    @GetMapping("{fileId}")
    public Object getFileById(@PathVariable String fileId,
                              @RequestParam(required = false) String name,
                              @RequestParam(defaultValue = "false") Boolean isSign) throws IOException {
        return R.resource(fileManagerService.getFileById(fileId, name, isSign), name);
    }

    /**
     * 上传文件/图片
     * @param file 文件流
     * @param isImg 是否为图片
     * @param isSign 是否为签名，签名的话单独一个文件夹存
     * @return 上传结果
     * @throws IOException
     */
    @PostMapping
    public Object uploadFile(MultipartFile file,
                             @RequestParam(defaultValue = "false") Boolean isImg,
                             @RequestParam(defaultValue = "false") Boolean isSign) throws IOException {
        return R.ok(fileManagerService.uploadFile(file, isImg, isSign));
    }

    /**
     * 通过文件ID删除文件
     * 文件ID格式为 文件md5取中间16位 + 文件后缀
     * @param fileId 文件ID
     * @return response
     */
    @DeleteMapping("{fileId}")
    public Object delFileById(@PathVariable String fileId,
                              @RequestParam(defaultValue = "false") Boolean isSign){
        fileManagerService.delFileById(fileId, isSign);
        return R.ok("操作成功");
    }
}
