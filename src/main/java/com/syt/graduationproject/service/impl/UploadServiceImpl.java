package com.syt.graduationproject.service.impl;

import com.syt.graduationproject.enums.VideoResolutionEnum;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.exception.ErrorOperationException;
import com.syt.graduationproject.exception.ErrorParamException;
import com.syt.graduationproject.exception.NoPermissionException;
import com.syt.graduationproject.exception.NotFoundException;
import com.syt.graduationproject.model.dto.MultipartUploadSessionDto;
import com.syt.graduationproject.model.po.VideoPo;
import com.syt.graduationproject.model.po.VideoSourcePo;
import com.syt.graduationproject.model.request.MultipartUploadCompleteRequest;
import com.syt.graduationproject.model.request.MultipartUploadInitRequest;
import com.syt.graduationproject.model.vo.MultipartUploadInitVo;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.UploadService;
import com.syt.graduationproject.service.minio.MinioService;
import com.syt.graduationproject.util.JsonUtil;
import com.syt.graduationproject.util.RedisKeyUtil;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final long SESSION_TTL_HOURS = 24L;

    private final VideoRepository videoRepository;

    private final MinioService minioService;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public MultipartUploadInitVo initMultipartUpload(MultipartUploadInitRequest request) {
        validateInitRequest(request);
        Long userId = UserHolderUtil.getUser().getUserId();
        Long videoId = videoRepository.createUploadingVideo(userId);
        String uploadToken = UUID.randomUUID().toString().replace("-", "");

        MultipartUploadSessionDto sessionDto = MultipartUploadSessionDto.builder()
                .videoId(videoId)
                .userId(userId)
                .uploadToken(uploadToken)
                .fileName(request.getFileName())
                .contentType(request.getContentType())
                .totalChunks(request.getTotalChunks())
                .createdAt(LocalDateTime.now())
                .build();

        String sessionKey = RedisKeyUtil.videoUploadSessionKey(videoId, uploadToken);
        String partKey = RedisKeyUtil.videoUploadPartsKey(videoId, uploadToken);
        stringRedisTemplate.opsForValue().set(sessionKey, JsonUtil.toJson(sessionDto), SESSION_TTL_HOURS, TimeUnit.HOURS);
        stringRedisTemplate.expire(partKey, SESSION_TTL_HOURS, TimeUnit.HOURS);

        return MultipartUploadInitVo.builder()
                .videoId(videoId)
                .uploadToken(uploadToken)
                .build();
    }

    @Override
    public void uploadMultipartChunk(Long videoId, String uploadToken, Integer chunkIndex, MultipartFile file) {
        if (videoId == null || chunkIndex == null || StringUtils.isBlank(uploadToken) || file == null || file.isEmpty()) {
            throw new ErrorParamException("分片参数非法");
        }
        MultipartUploadSessionDto sessionDto = getSession(videoId, uploadToken);
        Long userId = UserHolderUtil.getUser().getUserId();
        if (!userId.equals(sessionDto.getUserId())) {
            throw new NoPermissionException("无权上传该视频分片");
        }
        if (chunkIndex < 0 || chunkIndex >= sessionDto.getTotalChunks()) {
            throw new ErrorParamException("分片索引越界");
        }

        String chunkObjectName = buildChunkObjectName(videoId, uploadToken, chunkIndex);
        minioService.uploadPartFile(file, chunkObjectName);

        String partKey = RedisKeyUtil.videoUploadPartsKey(videoId, uploadToken);
        stringRedisTemplate.opsForHash().put(partKey, String.valueOf(chunkIndex), chunkObjectName);
        stringRedisTemplate.expire(partKey, SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    @Override
    public void completeMultipartUpload(MultipartUploadCompleteRequest request) {
        if (request == null || request.getVideoId() == null || StringUtils.isBlank(request.getUploadToken())) {
            throw new ErrorParamException("分片合并参数非法");
        }
        Long videoId = request.getVideoId();
        String uploadToken = request.getUploadToken();
        MultipartUploadSessionDto sessionDto = getSession(videoId, uploadToken);
        Long userId = UserHolderUtil.getUser().getUserId();
        if (!userId.equals(sessionDto.getUserId())) {
            throw new NoPermissionException("无权完成该视频上传");
        }

        VideoPo videoPo = videoRepository.queryVideoByIdAndUserId(videoId, userId);
        if (videoPo == null) {
            throw new NotFoundException("视频不存在");
        }
        if (VideoStatusEnum.UPLOADING.getCode() != videoPo.getStatus()) {
            throw new ErrorOperationException("当前视频状态不允许合并分片");
        }

        String partKey = RedisKeyUtil.videoUploadPartsKey(videoId, uploadToken);
        Map<Object, Object> partMap = stringRedisTemplate.opsForHash().entries(partKey);
        if (partMap.size() != sessionDto.getTotalChunks()) {
            throw new ErrorOperationException("分片数量不完整");
        }

        List<Integer> indexes = new ArrayList<>();
        for (Object key : partMap.keySet()) {
            indexes.add(Integer.parseInt(String.valueOf(key)));
        }
        Collections.sort(indexes);
        for (int i = 0; i < sessionDto.getTotalChunks(); i++) {
            if (!indexes.get(i).equals(i)) {
                throw new ErrorOperationException("分片缺失，无法合并");
            }
        }

        List<String> orderedParts = new ArrayList<>();
        for (Integer index : indexes) {
            orderedParts.add(String.valueOf(partMap.get(String.valueOf(index))));
        }

        String mergedObjectName = buildMergedObjectName(videoId, sessionDto.getFileName());
        minioService.composeObject(orderedParts, mergedObjectName);

        int updated = videoRepository.updateVideoStatus(videoId,
                VideoStatusEnum.UPLOADING.getCode(),
                VideoStatusEnum.UPLOAD_SUCCESS.getCode());
        if (updated <= 0) {
            throw new ErrorOperationException("更新上传状态失败");
        }
        stringRedisTemplate.delete(RedisKeyUtil.videoInfoKey(videoId));

        videoRepository.deleteVideoSource(videoId, VideoResolutionEnum.ORIGINAL.getCode());
        VideoSourcePo sourcePo = VideoSourcePo.builder()
                .videoId(videoId)
                .resolutionCode(VideoResolutionEnum.ORIGINAL.getCode())
                .playUrl(mergedObjectName)
                .format("mp4")
                .codec("")
                .build();
        videoRepository.saveVideoSource(sourcePo);

        stringRedisTemplate.delete(RedisKeyUtil.videoUploadSessionKey(videoId, uploadToken));
        stringRedisTemplate.delete(partKey);
    }

    private MultipartUploadSessionDto getSession(Long videoId, String uploadToken) {
        String sessionKey = RedisKeyUtil.videoUploadSessionKey(videoId, uploadToken);
        String sessionJson = stringRedisTemplate.opsForValue().get(sessionKey);
        if (StringUtils.isBlank(sessionJson)) {
            throw new NotFoundException("上传会话不存在或已过期");
        }
        return JsonUtil.fromJson(sessionJson, MultipartUploadSessionDto.class);
    }

    private void validateInitRequest(MultipartUploadInitRequest request) {
        if (request == null || StringUtils.isBlank(request.getFileName()) || request.getTotalChunks() == null || request.getTotalChunks() <= 0) {
            throw new ErrorParamException("初始化分片上传参数非法");
        }
    }

    private String buildChunkObjectName(Long videoId, String uploadToken, Integer chunkIndex) {
        return "video/chunk/" + videoId + "/" + uploadToken + "/" + chunkIndex + ".part";
    }

    private String buildMergedObjectName(Long videoId, String fileName) {
        String extension = "mp4";
        if (StringUtils.isNotBlank(fileName) && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }
        return "video/source/" + videoId + "/merged." + extension;
    }
}


