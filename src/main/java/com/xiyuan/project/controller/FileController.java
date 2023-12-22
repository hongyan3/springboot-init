package com.xiyuan.project.controller;

import cn.hutool.core.io.FileUtil;
import com.xiyuan.project.common.BaseResponse;
import com.xiyuan.project.common.ErrorCode;
import com.xiyuan.project.common.ResultUtils;
import com.xiyuan.project.constant.FileConstant;
import com.xiyuan.project.exception.BusinessException;
import com.xiyuan.project.model.dto.file.FileUploadRequest;
import com.xiyuan.project.model.entity.User;
import com.xiyuan.project.model.enums.FileUploadBusinessEnum;
import com.xiyuan.project.service.FileUploadService;
import com.xiyuan.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    UserService userService;
    @Resource
    FileUploadService fileUploadService;
    @PostMapping("/upload")
    public BaseResponse<String> UploadFile(@RequestPart("file") MultipartFile multipartFile, FileUploadRequest uploadRequest, HttpServletRequest request) {
        String business = uploadRequest.getBusiness();
        FileUploadBusinessEnum fileUploadEnum = FileUploadBusinessEnum.getEnumByValue(business);
        if (fileUploadEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        validFile(multipartFile,fileUploadEnum);
        User loginUser = userService.getLoginUser(request);
        // 文件目录：根据业务、用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String fileName = uuid + "-" + multipartFile.getOriginalFilename();
        String filePath = String.format("/%s/%s/%s", fileUploadEnum.getValue(), loginUser.getId(), fileName);
        File file = null;
        try {
            file = File.createTempFile(filePath,null);
            multipartFile.transferTo(file);
            fileUploadService.UploadFileToLocal(filePath,file);
            return ResultUtils.success(FileConstant.File_Host+filePath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filePath);
                }
            }
        }
    }
    /**
     * 校验文件
     *
     * @param multipartFile
     * @param fileUploadEnum 业务类型
     */
    private void validFile(MultipartFile multipartFile, FileUploadBusinessEnum fileUploadEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        if (fileSuffix != null) {
            fileSuffix = Strings.toRootLowerCase(fileSuffix);
        }
        switch(fileUploadEnum) {
            case USER_AVATAR:
                if (fileSize > 1024 * 1024L) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");
                }
                if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
                }
                break;
        }
    }
}

