package com.wflow.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author : JoinFyc
 * @version : 1.0
 */
public class R {
    public static <T> ResponseEntity<T> badRequest(T msg){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
    }

    public static ResponseEntity<InputStreamResource> resource(InputStreamResource resource, String name) throws UnsupportedEncodingException {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (StrUtil.isNotBlank(name)){
            String fileName = URLEncoder.encode(name, "utf-8");
            builder.header("Content-Disposition", "attachment; filename=" + fileName);
            builder.header("Access-Control-Expose-Headers", "Content-Disposition, download-filename");
            builder.header("download-filename", fileName);
        }
        builder.contentType(MediaType.APPLICATION_OCTET_STREAM);

        return builder.body(resource);
    }

    public static <T> ResponseEntity<T> notFound(T msg){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
    }

    public static <T> ResponseEntity<T> serverError(T msg){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
    }

    public static <T> ResponseEntity<T> unAuthorized(T msg){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
    }

    public static <T> ResponseEntity<T> forbidden(T msg){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
    }

    public static <T> ResponseEntity<T> noContent(T msg){
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(msg);
    }

    public static <T> ResponseEntity<T> ok(T msg){
        return ResponseEntity.ok(msg);
    }

    public static <T> ResponseEntity<T> created(T msg){
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }
}
