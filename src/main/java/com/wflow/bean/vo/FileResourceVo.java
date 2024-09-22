package com.wflow.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : JoinFyc
 * @date : 2024/9/7
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileResourceVo {

    //文件标识
    private String id;
    //文件大小 B
    private Long size;
    //是否为图片
    private Boolean isImage;
    //文件名
    private String name;
    //文件访问路径URL
    private String url;
}
