package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syt.graduationproject.enums.ReportStatusEnum;
import com.syt.graduationproject.enums.ReportTargetTypeEnum;
import com.syt.graduationproject.enums.UserStatusEnum;
import com.syt.graduationproject.enums.VideoAuditRecordStatusEnum;
import com.syt.graduationproject.enums.VideoStatusEnum;
import com.syt.graduationproject.exception.CustomException;
import com.syt.graduationproject.exception.ErrorParamException;
import com.syt.graduationproject.mapper.ReportMapper;
import com.syt.graduationproject.mapper.UserMapper;
import com.syt.graduationproject.mapper.VideoAuditRecordMapper;
import com.syt.graduationproject.mapper.VideoMapper;
import com.syt.graduationproject.model.es.VideoEsDoc;
import com.syt.graduationproject.model.po.*;
import com.syt.graduationproject.model.request.ManagerAuditVideoListRequest;
import com.syt.graduationproject.model.request.ManagerAuditVideoRequest;
import com.syt.graduationproject.model.request.ManagerReportListRequest;
import com.syt.graduationproject.model.request.ManagerReviewReportRequest;
import com.syt.graduationproject.model.vo.VideoAuditVo;
import com.syt.graduationproject.model.vo.ReportVo;
import com.syt.graduationproject.model.vo.SearchPageVo;
import com.syt.graduationproject.repository.SearchRepository;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.ManagerService;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

	private static final int DEFAULT_PAGE_NUM = 1;

	private static final int DEFAULT_PAGE_SIZE = 10;

	private static final int MAX_PAGE_SIZE = 100;

	private final ReportMapper reportMapper;

	private final UserMapper userMapper;

	private final VideoMapper videoMapper;

	private final VideoAuditRecordMapper videoAuditRecordMapper;

	private final UserRepository userRepository;

	private final VideoRepository videoRepository;

	private final SearchRepository searchRepository;

	@Override
	public SearchPageVo<VideoAuditVo> queryAuditVideoList(ManagerAuditVideoListRequest request) {
		int pageNum = normalizePageNum(request == null ? null : request.getPageNum());
		int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
		Integer status = request == null || request.getStatus() == null
				? VideoStatusEnum.AUDITING.getCode()
				: request.getStatus();

		Page<VideoPo> page = new Page<>(pageNum, pageSize);
		Page<VideoPo> result = videoMapper.selectPage(page,
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VideoPo>()
						.eq(VideoPo::getStatus, status)
						.orderByDesc(VideoPo::getUpdateTime));

		List<VideoAuditVo> records = result.getRecords().stream()
				.map(videoPo -> VideoAuditVo.builder()
						.videoId(videoPo.getId())
						.userId(videoPo.getUserId())
						.title(videoPo.getTitle())
						.coverUrl(videoPo.getCoverUrl())
						.duration(videoPo.getDuration())
						.status(videoPo.getStatus())
						.createTime(videoPo.getCreateTime())
						.updateTime(videoPo.getUpdateTime())
						.build())
				.collect(Collectors.toList());

		return SearchPageVo.<VideoAuditVo>builder()
				.total(result.getTotal())
				.pageNum(pageNum)
				.pageSize(pageSize)
				.records(records)
				.build();
	}

	@Override
	public SearchPageVo<ReportVo> queryReportList(ManagerReportListRequest request) {
		int pageNum = normalizePageNum(request == null ? null : request.getPageNum());
		int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
		Integer targetType = request == null ? null : request.getTargetType();
		Integer status = request == null || request.getStatus() == null
				? ReportStatusEnum.WAITING_AUDIT.getCode()
				: request.getStatus();

		Page<ReportPo> page = new Page<>(pageNum, pageSize);
		Page<ReportPo> result = reportMapper.selectPage(page,
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReportPo>()
						.eq(ReportPo::getStatus, status)
						.eq(targetType != null, ReportPo::getTargetType, targetType)
						.orderByDesc(ReportPo::getCreateTime));

		List<ReportVo> records = result.getRecords().stream()
				.map(reportPo -> ReportVo.builder()
						.reportId(reportPo.getId())
						.reporterId(reportPo.getReporterId())
						.targetType(reportPo.getTargetType())
						.targetId(reportPo.getTargetId())
						.reason(reportPo.getReason())
						.detail(reportPo.getDetail())
						.status(reportPo.getStatus())
						.reviewerId(reportPo.getReviewerId())
						.reviewNote(reportPo.getReviewNote())
						.createTime(reportPo.getCreateTime())
						.updateTime(reportPo.getUpdateTime())
						.build())
				.collect(Collectors.toList());

		return SearchPageVo.<ReportVo>builder()
				.total(result.getTotal())
				.pageNum(pageNum)
				.pageSize(pageSize)
				.records(records)
				.build();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void reviewReport(ManagerReviewReportRequest request) {
		if (request == null || request.getReportId() == null || request.getOperation() == null) {
			throw new CustomException("举报审核参数不完整");
		}
		ReportPo reportPo = reportMapper.selectById(request.getReportId());
		if (reportPo == null) {
			throw new CustomException("举报记录不存在");
		}
		if (!ReportStatusEnum.WAITING_AUDIT.getCode().equals(reportPo.getStatus())) {
			throw new CustomException("该举报已审核");
		}

		boolean approved = request.getOperation() == 1;
		reportPo.setStatus(approved ? ReportStatusEnum.APPROVED.getCode() : ReportStatusEnum.REJECTED.getCode());
		reportPo.setReviewerId(UserHolderUtil.getUser().getUserId());
		reportPo.setReviewNote(request.getReviewNote());
		reportPo.setUpdateTime(LocalDateTime.now());
		reportMapper.updateById(reportPo);

		if (!approved) {
			return;
		}
		if (ReportTargetTypeEnum.USER.getCode().equals(reportPo.getTargetType())) {
			banUser(reportPo.getTargetId());
			return;
		}
		if (ReportTargetTypeEnum.VIDEO.getCode().equals(reportPo.getTargetType())) {
			banVideo(reportPo.getTargetId());
			return;
		}
		throw new CustomException("举报目标类型非法");
	}

	@Override
	public void banUser(Long userId) {
		UserPo userPo = userRepository.queryUserAnyStatusById(userId);
		if (userPo == null) {
			throw new CustomException("用户不存在");
		}
		if (UserStatusEnum.DELETED.getCode() == userPo.getStatus()) {
			throw new CustomException("用户已删除");
		}
		LambdaUpdateWrapper<UserPo> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(UserPo::getId, userId)
				.set(UserPo::getStatus, UserStatusEnum.BANNED.getCode())
				.set(UserPo::getUpdateTime, LocalDateTime.now());
		userMapper.update(null, updateWrapper);
	}

	@Override
	public void unbanUser(Long userId) {
		UserPo userPo = userRepository.queryUserAnyStatusById(userId);
		if (userPo == null) {
			throw new CustomException("用户不存在");
		}
		LambdaUpdateWrapper<UserPo> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(UserPo::getId, userId)
				.set(UserPo::getStatus, UserStatusEnum.NORMAL.getCode());
		userMapper.update(null, updateWrapper);
	}

	@Override
	public void banVideo(Long videoId) {
		VideoPo videoPo = videoRepository.queryVideoById(videoId);
		if (videoPo == null) {
			throw new CustomException("视频不存在");
		}
		videoRepository.updateVideoStatus(videoId, null, VideoStatusEnum.BANNED.getCode());
		searchRepository.deleteVideoDoc(videoId);
	}

	@Override
	public void unbanVideo(Long videoId) {
		VideoPo videoPo = videoRepository.queryVideoById(videoId);
		if (videoPo == null) {
			throw new CustomException("视频不存在");
		}
		if (VideoStatusEnum.BANNED.getCode() != videoPo.getStatus()) {
			throw new CustomException("该视频未被封禁");
		}
		int updated = videoRepository.updateVideoStatus(videoId, VideoStatusEnum.BANNED.getCode(), VideoStatusEnum.AUDITING.getCode());
		if (updated > 0) {
			createAuditingRecord(videoId, videoPo.getUserId());
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void auditVideo(ManagerAuditVideoRequest request) {
		if (Objects.isNull(request)) {
			throw new ErrorParamException("视频审核参数不能为空");
		}
		Long videoId = request.getVideoId();
		if (videoId == null || request.getOperation() == null) {
			throw new CustomException("视频审核参数不完整");
		}
		VideoPo videoPo = videoRepository.queryVideoById(videoId);
		if (videoPo == null) {
			throw new CustomException("视频不存在");
		}
		if (VideoStatusEnum.AUDITING.getCode() != videoPo.getStatus()) {
			throw new CustomException("当前视频不在审核中");
		}

		Long userId = UserHolderUtil.getUser().getUserId();
		if (request.getOperation() == 1) {
			int updated = videoRepository.updateVideoStatus(videoId,
					VideoStatusEnum.AUDITING.getCode(), VideoStatusEnum.AUDIT_PASSED.getCode());
			if (updated <= 0) {
				throw new CustomException("视频审核状态更新失败");
			}
			completeLatestAuditingRecord(videoId,
					VideoAuditRecordStatusEnum.PASSED.getCode(), userId, request.getReviewNote());
			autoPublishVideo(videoId);
			return;
		}
		int updated = videoRepository.updateVideoStatus(videoId,
				VideoStatusEnum.AUDITING.getCode(), VideoStatusEnum.AUDIT_REJECTED.getCode());
		if (updated > 0) {
			completeLatestAuditingRecord(videoId,
					VideoAuditRecordStatusEnum.REJECTED.getCode(), userId, request.getReviewNote());
		}
	}

	@Override
	public void createAuditingRecord(Long videoId, Long applicantId) {
		LocalDateTime now = LocalDateTime.now();
		videoAuditRecordMapper.insert(VideoAuditRecordPo.builder()
				.videoId(videoId)
				.applicantId(applicantId)
				.status(VideoAuditRecordStatusEnum.AUDITING.getCode())
				.createTime(now)
				.updateTime(now)
				.build());
	}

	@Override
	public void completeLatestAuditingRecord(Long videoId, Integer targetStatus, Long reviewerId, String reviewNote) {
		LambdaQueryWrapper<VideoAuditRecordPo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(VideoAuditRecordPo::getVideoId, videoId)
				.eq(VideoAuditRecordPo::getStatus, VideoAuditRecordStatusEnum.AUDITING.getCode())
				.orderByDesc(VideoAuditRecordPo::getId)
				.last("limit 1");
		VideoAuditRecordPo auditRecordPo = videoAuditRecordMapper.selectOne(queryWrapper);
		if (auditRecordPo == null) {
			return;
		}
		auditRecordPo.setStatus(targetStatus);
		auditRecordPo.setReviewerId(reviewerId);
		auditRecordPo.setReviewNote(reviewNote);
		auditRecordPo.setUpdateTime(LocalDateTime.now());
		videoAuditRecordMapper.updateById(auditRecordPo);
	}

	private Integer normalizePageNum(Integer pageNum) {
		if (pageNum == null || pageNum < 1) {
			return DEFAULT_PAGE_NUM;
		}
		return pageNum;
	}

	private Integer normalizePageSize(Integer pageSize) {
		if (pageSize == null || pageSize < 1) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(pageSize, MAX_PAGE_SIZE);
	}

	private void autoPublishVideo(Long videoId) {
		int updated = videoRepository.updateVideoStatus(videoId,
				VideoStatusEnum.AUDIT_PASSED.getCode(), VideoStatusEnum.PUBLISHED.getCode());
		if (updated <= 0) {
			throw new CustomException("视频发布失败，请稍后重试");
		}

		VideoPo videoPo = videoRepository.queryVideoById(videoId);
		VideoStatsPo statsPo = videoRepository.queryVideoStatsById(videoId);
		UserPo userPo = userRepository.queryUserAnyStatusById(videoPo.getUserId());
		searchRepository.upsertVideoDoc(VideoEsDoc.builder()
				.videoId(videoPo.getId())
				.title(videoPo.getTitle())
				.description(videoPo.getDescription())
				.userId(videoPo.getUserId())
				.username(userPo == null ? "" : userPo.getUsername())
				.coverUrl(videoPo.getCoverUrl())
				.playCount(statsPo == null ? 0L : statsPo.getPlayCount())
				.collectionCount(statsPo == null ? 0L : statsPo.getCollectCount())
				.duration(videoPo.getDuration())
				.createTime(videoPo.getCreateTime())
				.build());
	}
}
