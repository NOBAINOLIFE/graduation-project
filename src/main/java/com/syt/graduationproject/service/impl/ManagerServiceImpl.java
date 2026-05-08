package com.syt.graduationproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syt.graduationproject.converter.VideoConverter;
import com.syt.graduationproject.enums.*;
import com.syt.graduationproject.exception.ErrorOperationException;
import com.syt.graduationproject.exception.ErrorParamException;
import com.syt.graduationproject.exception.NotFoundException;
import com.syt.graduationproject.mapper.*;
import com.syt.graduationproject.model.po.*;
import com.syt.graduationproject.model.request.ManagerAuditVideoListRequest;
import com.syt.graduationproject.model.request.ManagerAuditVideoRequest;
import com.syt.graduationproject.model.request.ManagerReportListRequest;
import com.syt.graduationproject.model.request.ManagerReviewReportRequest;
import com.syt.graduationproject.model.request.ManagerVideoPartitionListRequest;
import com.syt.graduationproject.model.request.ManagerUserListRequest;
import com.syt.graduationproject.model.request.ManagerVideoTagListRequest;
import com.syt.graduationproject.model.vo.ManagerVideoPartitionVo;
import com.syt.graduationproject.model.vo.ManagerUserVo;
import com.syt.graduationproject.model.vo.ManagerVideoTagVo;
import com.syt.graduationproject.model.vo.VideoSourceVo;
import com.syt.graduationproject.model.vo.VideoAuditVo;
import com.syt.graduationproject.model.vo.report.CommentReportInfoVo;
import com.syt.graduationproject.model.vo.report.ManagerReportRecordVo;
import com.syt.graduationproject.model.vo.Page.PageVo;
import com.syt.graduationproject.model.vo.report.ReportInfoVo;
import com.syt.graduationproject.model.vo.report.UserReportInfoVo;
import com.syt.graduationproject.model.vo.report.VideoReportInfoVo;
import com.syt.graduationproject.model.vo.VideoTagVo;
import com.syt.graduationproject.repository.InteractRepository;
import com.syt.graduationproject.repository.UserRepository;
import com.syt.graduationproject.repository.VideoRepository;
import com.syt.graduationproject.service.EsSyncService;
import com.syt.graduationproject.service.ManagerService;
import com.syt.graduationproject.service.minio.MinioService;
import com.syt.graduationproject.util.UserHolderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

	private final CommentMapper commentMapper;

	private final CommentStatsMapper commentStatsMapper;

	private final VideoStatsMapper videoStatsMapper;

	private final UserStatsMapper userStatsMapper;

	private final VideoPartitionMapper videoPartitionMapper;

	private final VideoTagMapper videoTagMapper;

	private final VideoTagRelMapper videoTagRelMapper;

	private final UserRepository userRepository;

	private final VideoRepository videoRepository;

	private final EsSyncService esSyncService;

	private final MinioService minioService;

	private final InteractRepository interactRepository;

	private final VideoConverter videoConverter;

	@Override
	public PageVo<VideoAuditVo> queryAuditVideoList(ManagerAuditVideoListRequest request) {
		int pageNum = normalizePageNum(request == null ? null : request.getPageNum());
		int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
		Integer status = request == null || request.getStatus() == null
				? VideoAuditRecordStatusEnum.AUDITING.getCode()
				: request.getStatus();
		boolean queryAllStatus = status != null && status < 0;

		Page<VideoAuditRecordPo> page = new Page<>(pageNum, pageSize);
		Page<VideoAuditRecordPo> result = videoAuditRecordMapper.selectPage(page,
				new LambdaQueryWrapper<VideoAuditRecordPo>()
						.eq(!queryAllStatus, VideoAuditRecordPo::getStatus, status)
						.orderByDesc(VideoAuditRecordPo::getUpdateTime));

		List<VideoAuditRecordPo> auditRecordList = result.getRecords();
		if (auditRecordList == null || auditRecordList.isEmpty()) {
			return PageVo.<VideoAuditVo>builder()
					.total(result.getTotal())
					.pageNum(pageNum)
					.pageSize(pageSize)
					.records(Collections.emptyList())
					.build();
		}

		List<Long> videoIdList = auditRecordList.stream()
				.map(VideoAuditRecordPo::getVideoId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());

		Map<Long, VideoPo> videoMap = videoMapper.selectBatchIds(videoIdList).stream()
				.collect(Collectors.toMap(VideoPo::getId, videoPo -> videoPo));

		Set<Long> userIdSet = videoMap.values().stream()
				.map(VideoPo::getUserId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, UserPo> userMap = userIdSet.isEmpty()
				? Collections.emptyMap()
				: userMapper.selectBatchIds(userIdSet).stream()
						.collect(Collectors.toMap(UserPo::getId, userPo -> userPo));

		Set<Long> reviewerIdSet = auditRecordList.stream()
				.map(VideoAuditRecordPo::getReviewerId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, UserPo> reviewerMap = reviewerIdSet.isEmpty()
				? Collections.emptyMap()
				: userMapper.selectBatchIds(reviewerIdSet).stream()
						.collect(Collectors.toMap(UserPo::getId, userPo -> userPo));

		Set<Long> partitionIdSet = videoMap.values().stream()
				.map(VideoPo::getPartitionId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, VideoPartitionPo> partitionMap = partitionIdSet.isEmpty()
				? Collections.emptyMap()
				: videoPartitionMapper.selectBatchIds(partitionIdSet).stream()
						.collect(Collectors.toMap(VideoPartitionPo::getId, partitionPo -> partitionPo));

		Map<Long, List<VideoTagVo>> videoTagMap = queryVideoTagMap(videoIdList);
		Map<Long, List<VideoSourceVo>> videoSourceMap = queryVideoSourceMap(videoIdList);

		List<VideoAuditVo> records = auditRecordList.stream().map(auditRecordPo -> {
			VideoPo videoPo = videoMap.get(auditRecordPo.getVideoId());
			UserPo userPo = videoPo == null ? null : userMap.get(videoPo.getUserId());
			UserPo reviewerPo = reviewerMap.get(auditRecordPo.getReviewerId());
			VideoPartitionPo partitionPo = videoPo == null ? null : partitionMap.get(videoPo.getPartitionId());
			return VideoAuditVo.builder()
					.videoId(auditRecordPo.getVideoId())
					.videoSourceList(videoSourceMap.getOrDefault(auditRecordPo.getVideoId(), Collections.emptyList()))
					.title(videoPo == null ? null : videoPo.getTitle())
					.coverUrl(videoPo == null ? null : videoPo.getCoverUrl())
					.description(videoPo == null ? null : videoPo.getDescription())
					.partitionName(partitionPo == null ? null : partitionPo.getPartitionName())
					.tagList(videoTagMap.getOrDefault(auditRecordPo.getVideoId(), Collections.emptyList()))
					.userId(videoPo == null ? auditRecordPo.getApplicantId() : videoPo.getUserId())
					.username(userPo == null ? null : userPo.getUsername())
					.avatar(userPo == null ? null : userPo.getAvatarUrl())
					.status(auditRecordPo.getStatus())
					.reviewerId(auditRecordPo.getReviewerId())
					.reviewerName(reviewerPo == null ? null : reviewerPo.getUsername())
					.reviewerAvatar(reviewerPo == null ? null : reviewerPo.getAvatarUrl())
					.reviewNote(auditRecordPo.getReviewNote())
					.createTime(auditRecordPo.getCreateTime())
					.updateTime(auditRecordPo.getUpdateTime())
					.build();
		}).collect(Collectors.toList());

		return PageVo.<VideoAuditVo>builder()
				.total(result.getTotal())
				.pageNum(pageNum)
				.pageSize(pageSize)
				.records(records)
				.build();
	}

	@Override
	public PageVo<ManagerUserVo> queryUserList(ManagerUserListRequest request) {
		int pageNum = normalizePageNum(request == null ? null : request.getPageNum());
		int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
		String keyword = request == null ? null : StringUtils.trimToNull(request.getKeyword());
		Integer status = request == null ? null : request.getStatus();
		boolean queryAllStatus = status == null || status < 0;

		Page<UserPo> page = new Page<>(pageNum, pageSize);
		Page<UserPo> result = userMapper.selectPage(page, new LambdaQueryWrapper<UserPo>()
				.like(StringUtils.isNotBlank(keyword), UserPo::getUsername, keyword)
				.eq(!queryAllStatus, UserPo::getStatus, status)
				.ne(queryAllStatus, UserPo::getStatus, UserStatusEnum.DELETED.getCode())
				.orderByDesc(UserPo::getCreateTime)
				.orderByDesc(UserPo::getId));

		List<UserPo> users = result.getRecords();
		if (CollectionUtils.isEmpty(users)) {
			return PageVo.<ManagerUserVo>builder()
					.total(result.getTotal())
					.pageNum(pageNum)
					.pageSize(pageSize)
					.records(Collections.emptyList())
					.build();
		}

		Set<Long> userIds = users.stream()
				.map(UserPo::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, UserStatsPo> userStatsMap = queryUserStatsMap(userIds);
		List<ManagerUserVo> records = users.stream()
				.map(userPo -> buildManagerUserVo(userPo, userStatsMap.get(userPo.getId())))
				.collect(Collectors.toList());

		return PageVo.<ManagerUserVo>builder()
				.total(result.getTotal())
				.pageNum(pageNum)
				.pageSize(pageSize)
				.records(records)
				.build();
	}

	@Override
	public PageVo<ManagerVideoPartitionVo> queryVideoPartitionList(ManagerVideoPartitionListRequest request) {
		int pageNum = normalizePageNum(request == null ? null : request.getPageNum());
		int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
		String keyword = request == null ? null : StringUtils.trimToNull(request.getKeyword());

		Page<VideoPartitionPo> page = new Page<>(pageNum, pageSize);
		Page<VideoPartitionPo> result = videoPartitionMapper.selectPage(page, new LambdaQueryWrapper<VideoPartitionPo>()
				.like(StringUtils.isNotBlank(keyword), VideoPartitionPo::getPartitionName, keyword)
				.orderByDesc(VideoPartitionPo::getUpdateTime)
				.orderByDesc(VideoPartitionPo::getId));

		List<VideoPartitionPo> partitionList = result.getRecords();
		if (CollectionUtils.isEmpty(partitionList)) {
			return PageVo.<ManagerVideoPartitionVo>builder()
					.total(result.getTotal())
					.pageNum(pageNum)
					.pageSize(pageSize)
					.records(Collections.emptyList())
					.build();
		}

		Set<Long> partitionIdSet = partitionList.stream()
				.map(VideoPartitionPo::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, Long> relatedVideoCountMap = queryPartitionVideoCountMap(partitionIdSet);

		List<ManagerVideoPartitionVo> records = partitionList.stream()
				.map(partitionPo -> ManagerVideoPartitionVo.builder()
						.partitionId(partitionPo.getId())
						.partitionName(partitionPo.getPartitionName())
						.relatedVideoCount(relatedVideoCountMap.getOrDefault(partitionPo.getId(), 0L))
						.createTime(partitionPo.getCreateTime())
						.updateTime(partitionPo.getUpdateTime())
						.build())
				.collect(Collectors.toList());

		return PageVo.<ManagerVideoPartitionVo>builder()
				.total(result.getTotal())
				.pageNum(pageNum)
				.pageSize(pageSize)
				.records(records)
				.build();
	}

	@Override
	public PageVo<ManagerVideoTagVo> queryVideoTagList(ManagerVideoTagListRequest request) {
		int pageNum = normalizePageNum(request == null ? null : request.getPageNum());
		int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
		String keyword = request == null ? null : StringUtils.trimToNull(request.getKeyword());

		Page<VideoTagPo> page = new Page<>(pageNum, pageSize);
		Page<VideoTagPo> result = videoTagMapper.selectPage(page, new LambdaQueryWrapper<VideoTagPo>()
				.like(StringUtils.isNotBlank(keyword), VideoTagPo::getTagName, keyword)
				.orderByDesc(VideoTagPo::getHot)
				.orderByDesc(VideoTagPo::getUpdateTime)
				.orderByDesc(VideoTagPo::getId));

		List<VideoTagPo> tagList = result.getRecords();
		if (CollectionUtils.isEmpty(tagList)) {
			return PageVo.<ManagerVideoTagVo>builder()
					.total(result.getTotal())
					.pageNum(pageNum)
					.pageSize(pageSize)
					.records(Collections.emptyList())
					.build();
		}

		Set<Long> tagIdSet = tagList.stream()
				.map(VideoTagPo::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, Long> relatedVideoCountMap = queryTagVideoCountMap(tagIdSet);

		List<ManagerVideoTagVo> records = tagList.stream()
				.map(tagPo -> ManagerVideoTagVo.builder()
						.tagId(tagPo.getId())
						.tagName(tagPo.getTagName())
						.hot(defaultLong(tagPo.getHot()))
						.relatedVideoCount(relatedVideoCountMap.getOrDefault(tagPo.getId(), 0L))
						.createTime(tagPo.getCreateTime())
						.updateTime(tagPo.getUpdateTime())
						.build())
				.collect(Collectors.toList());

		return PageVo.<ManagerVideoTagVo>builder()
				.total(result.getTotal())
				.pageNum(pageNum)
				.pageSize(pageSize)
				.records(records)
				.build();
	}

	private ManagerUserVo buildManagerUserVo(UserPo userPo, UserStatsPo statsPo) {
		return ManagerUserVo.builder()
				.userId(userPo.getId())
				.account(userPo.getAccount())
				.username(userPo.getUsername())
				.avatarUrl(userPo.getAvatarUrl())
				.bio(userPo.getBio())
				.status(userPo.getStatus())
				.statusText(userStatusText(userPo.getStatus()))
				.videoNum(statsPo == null ? 0L : defaultLong(statsPo.getVideoNum()))
				.fansNum(statsPo == null ? 0L : defaultLong(statsPo.getFansNum()))
				.followNum(statsPo == null ? 0L : defaultLong(statsPo.getFollowNum()))
				.likeNum(statsPo == null ? 0L : defaultLong(statsPo.getLikeNum()))
				.playNum(statsPo == null ? 0L : defaultLong(statsPo.getPlayNum()))
				.createTime(userPo.getCreateTime())
				.updateTime(userPo.getUpdateTime())
				.build();
	}

	private Map<Long, Long> queryPartitionVideoCountMap(Set<Long> partitionIdSet) {
		if (partitionIdSet == null || partitionIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		return videoMapper.selectList(new LambdaQueryWrapper<VideoPo>()
						.in(VideoPo::getPartitionId, partitionIdSet))
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(VideoPo::getPartitionId, Collectors.counting()));
	}

	private Map<Long, Long> queryTagVideoCountMap(Set<Long> tagIdSet) {
		if (tagIdSet == null || tagIdSet.isEmpty()) {
			return Collections.emptyMap();
		}
		return videoTagRelMapper.selectList(new LambdaQueryWrapper<VideoTagRelPo>()
						.in(VideoTagRelPo::getTagId, tagIdSet))
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(VideoTagRelPo::getTagId, Collectors.counting()));
	}

	private String userStatusText(Integer status) {
		if (status == null) {
			return "未知";
		}
		for (UserStatusEnum statusEnum : UserStatusEnum.values()) {
			if (statusEnum.getCode() == status) {
				return statusEnum.getMessage();
			}
		}
		return "状态" + status;
	}

	private Map<Long, List<VideoTagVo>> queryVideoTagMap(List<Long> videoIdList) {
		if (videoIdList == null || videoIdList.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<VideoTagRelPo> relQuery = new LambdaQueryWrapper<VideoTagRelPo>()
				.in(VideoTagRelPo::getVideoId, videoIdList)
				.orderByAsc(VideoTagRelPo::getId);
		List<VideoTagRelPo> relList = videoTagRelMapper.selectList(relQuery);
		if (relList == null || relList.isEmpty()) {
			return Collections.emptyMap();
		}

		Set<Long> tagIdSet = relList.stream()
				.map(VideoTagRelPo::getTagId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, VideoTagPo> tagMap = tagIdSet.isEmpty()
				? Collections.emptyMap()
				: videoTagMapper.selectBatchIds(tagIdSet).stream()
						.collect(Collectors.toMap(VideoTagPo::getId, tagPo -> tagPo));

		Map<Long, List<VideoTagVo>> result = new LinkedHashMap<>();
		for (VideoTagRelPo relPo : relList) {
			VideoTagPo tagPo = tagMap.get(relPo.getTagId());
			if (tagPo == null) {
				continue;
			}
			result.computeIfAbsent(relPo.getVideoId(), key -> new ArrayList<>())
					.add(VideoTagVo.builder()
							.tagId(tagPo.getId())
							.tagName(tagPo.getTagName())
							.build());
		}
		return result;
	}

	private Map<Long, List<VideoSourceVo>> queryVideoSourceMap(List<Long> videoIdList) {
		if (videoIdList == null || videoIdList.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, List<VideoSourceVo>> result = new HashMap<>();
		for (Long videoId : videoIdList) {
			List<VideoSourcePo> sourceList = videoRepository.queryVideoSource(videoId, null, true);
			if (sourceList != null) {
				for (VideoSourcePo sourcePo : sourceList) {
					if (sourcePo != null && StringUtils.isNotBlank(sourcePo.getPlayUrl())) {
						sourcePo.setPlayUrl(minioService.generateGetUrl(sourcePo.getPlayUrl(), 30));
					}
				}
			}
			result.put(videoId, sourceList == null ? Collections.emptyList() : videoConverter.toVideoSourceVoList(sourceList));
		}
		return result;
	}

	@Override
	public PageVo<ManagerReportRecordVo> queryReportList(ManagerReportListRequest request) {
		int pageNum = normalizePageNum(request == null ? null : request.getPageNum());
		int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
		Integer reportType = request == null ? null : request.getReportType();
		Integer status = request == null ? null :request.getStatus();

		Page<ReportPo> reportPoPage = interactRepository
				.queryReportRecord(null, status, reportType, pageNum, pageSize);
		if (CollectionUtils.isEmpty(reportPoPage.getRecords())) {
			return PageVo.<ManagerReportRecordVo>builder()
					.total(reportPoPage.getTotal())
					.pageNum(pageNum)
					.pageSize(pageSize)
					.records(Collections.emptyList())
					.build();
		}

		Set<Long> userIds = new HashSet<>();
		Set<Long> videoIds = new HashSet<>();
		Set<Long> commentIds = new HashSet<>();
		reportPoPage.getRecords().forEach(reportPo -> {
			userIds.add(reportPo.getReporterId());
			if (reportPo.getReviewerId() != null) {
				userIds.add(reportPo.getReviewerId());
			}
			if (ReportTargetTypeEnum.USER.getCode().equals(reportPo.getReportType())) {
				userIds.add(reportPo.getTargetId());
			} else if (ReportTargetTypeEnum.VIDEO.getCode().equals(reportPo.getReportType())) {
				videoIds.add(reportPo.getTargetId());
			} else if (ReportTargetTypeEnum.COMMENT.getCode().equals(reportPo.getReportType())) {
				commentIds.add(reportPo.getTargetId());
			}
		});

		Map<Long, CommentPo> commentMap = queryCommentMap(commentIds);
		Set<Long> rootCommentIds = commentMap.values().stream()
				.map(CommentPo::getRootId)
				.filter(rootId -> rootId != null && rootId > 0)
				.collect(Collectors.toSet());
		Map<Long, CommentPo> rootCommentMap = queryCommentMap(rootCommentIds);

		commentMap.values().forEach(commentPo -> {
			if (commentPo.getUserId() != null) {
				userIds.add(commentPo.getUserId());
			}
			if (commentPo.getVideoId() != null) {
				videoIds.add(commentPo.getVideoId());
			}
		});

		Map<Long, VideoPo> videoMap = queryVideoMap(videoIds);
		Set<Long> partitionIds = videoMap.values().stream()
				.map(VideoPo::getPartitionId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		videoMap.values().forEach(videoPo -> {
			if (videoPo.getUserId() != null) {
				userIds.add(videoPo.getUserId());
			}
		});

		Map<Long, UserPo> userMap = queryUserMap(userIds);
		Map<Long, UserStatsPo> userStatsMap = queryUserStatsMap(userIds);
		Map<Long, VideoStatsPo> videoStatsMap = queryVideoStatsMap(videoIds);
		Map<Long, VideoPartitionPo> partitionMap = queryPartitionMap(partitionIds);
		Map<Long, List<String>> videoTagNameMap = queryVideoTagNameMap(new ArrayList<>(videoIds));
		Map<Long, List<VideoSourceVo>> videoSourceMap = queryVideoSourceMap(new ArrayList<>(videoIds));

        List<ManagerReportRecordVo> records = reportPoPage.getRecords().stream()
				.map(reportPo -> buildManagerReportRecord(reportPo, userMap, userStatsMap, videoMap,
                        videoStatsMap, partitionMap, videoTagNameMap, videoSourceMap,
                        commentMap, rootCommentMap))
				.collect(Collectors.toList());

		return PageVo.<ManagerReportRecordVo>builder()
				.total(reportPoPage.getTotal())
				.pageNum(pageNum)
				.pageSize(pageSize)
				.records(records)
				.build();
	}

	private ManagerReportRecordVo buildManagerReportRecord(ReportPo reportPo,
			Map<Long, UserPo> userMap,
			Map<Long, UserStatsPo> userStatsMap,
			Map<Long, VideoPo> videoMap,
			Map<Long, VideoStatsPo> videoStatsMap,
			Map<Long, VideoPartitionPo> partitionMap,
			Map<Long, List<String>> videoTagNameMap,
			Map<Long, List<VideoSourceVo>> videoSourceMap,
			Map<Long, CommentPo> commentMap,
			Map<Long, CommentPo> rootCommentMap) {
		UserPo reporter = userMap.get(reportPo.getReporterId());
		UserPo reviewer = reportPo.getReviewerId() != null ? userMap.get(reportPo.getReviewerId()) : null;
		return ManagerReportRecordVo.builder()
				.reportId(reportPo.getId())
				.reporterId(reportPo.getReporterId())
				.reporterName(reporter != null ? reporter.getUsername() : "未知用户")
				.reporterAvatar(reporter != null ? reporter.getAvatarUrl() : null)
				.reporterBio(reporter != null ? reporter.getBio() : null)
				.reportType(reportPo.getReportType())
				.reportInfo(buildReportInfo(reportPo, userMap, userStatsMap, videoMap, videoStatsMap, partitionMap,
						videoTagNameMap, videoSourceMap, commentMap, rootCommentMap))
				.reason(reportPo.getReason())
				.detail(reportPo.getDetail())
				.status(reportPo.getStatus())
				.reviewerId(reportPo.getReviewerId())
				.reviewerName(reviewer != null ? reviewer.getUsername() : null)
				.reviewerAvatar(reviewer != null ? reviewer.getAvatarUrl() : null)
				.reviewNote(reportPo.getReviewNote())
				.createTime(reportPo.getCreateTime())
				.updateTime(reportPo.getUpdateTime())
				.build();
	}

	private ReportInfoVo buildReportInfo(ReportPo reportPo,
			Map<Long, UserPo> userMap,
			Map<Long, UserStatsPo> userStatsMap,
			Map<Long, VideoPo> videoMap,
			Map<Long, VideoStatsPo> videoStatsMap,
			Map<Long, VideoPartitionPo> partitionMap,
			Map<Long, List<String>> videoTagNameMap,
			Map<Long, List<VideoSourceVo>> videoSourceMap,
			Map<Long, CommentPo> commentMap,
			Map<Long, CommentPo> rootCommentMap) {
		if (ReportTargetTypeEnum.USER.getCode().equals(reportPo.getReportType())) {
			return buildUserReportInfo(reportPo.getTargetId(), userMap, userStatsMap);
		}
		if (ReportTargetTypeEnum.VIDEO.getCode().equals(reportPo.getReportType())) {
			return buildVideoReportInfo(reportPo.getTargetId(), videoMap, videoStatsMap, partitionMap,
					videoTagNameMap, videoSourceMap, userMap);
		}
		if (ReportTargetTypeEnum.COMMENT.getCode().equals(reportPo.getReportType())) {
			return buildCommentReportInfo(reportPo.getTargetId(), commentMap, rootCommentMap, videoMap, userMap);
		}
		return null;
	}

	private UserReportInfoVo buildUserReportInfo(Long userId,
			Map<Long, UserPo> userMap,
			Map<Long, UserStatsPo> userStatsMap) {
		UserPo targetUser = userMap.get(userId);
		UserStatsPo targetUserStats = userStatsMap.get(userId);
		if (targetUser == null) {
			return null;
		}
		UserReportInfoVo reportInfo = new UserReportInfoVo();
		fillBaseUserReportInfo(reportInfo, targetUser);
		reportInfo.setVideoNum(targetUserStats == null ? 0L : defaultLong(targetUserStats.getVideoNum()));
		reportInfo.setFansNum(targetUserStats == null ? 0L : defaultLong(targetUserStats.getFansNum()));
		reportInfo.setLikeNum(targetUserStats == null ? 0L : defaultLong(targetUserStats.getLikeNum()));
		reportInfo.setPlayNum(targetUserStats == null ? 0L : defaultLong(targetUserStats.getPlayNum()));
		reportInfo.setCreateTime(targetUser.getCreateTime());
		return reportInfo;
	}

	private VideoReportInfoVo buildVideoReportInfo(Long videoId,
			Map<Long, VideoPo> videoMap,
			Map<Long, VideoStatsPo> videoStatsMap,
			Map<Long, VideoPartitionPo> partitionMap,
			Map<Long, List<String>> videoTagNameMap,
			Map<Long, List<VideoSourceVo>> videoSourceMap,
			Map<Long, UserPo> userMap) {
		VideoPo video = videoMap.get(videoId);
		if (video == null) {
			return null;
		}
		UserPo uploader = userMap.get(video.getUserId());
		VideoStatsPo videoStats = videoStatsMap.get(videoId);
		VideoPartitionPo partitionPo = video.getPartitionId() == null ? null : partitionMap.get(video.getPartitionId());
		VideoReportInfoVo reportInfo = new VideoReportInfoVo();
		if (uploader != null) {
			fillBaseUserReportInfo(reportInfo, uploader);
		}
		reportInfo.setVideoId(video.getId());
		reportInfo.setTitle(video.getTitle());
		reportInfo.setCoverUrl(video.getCoverUrl());
		reportInfo.setDescription(video.getDescription());
		reportInfo.setPartitionName(partitionPo == null ? null : partitionPo.getPartitionName());
		reportInfo.setTagList(videoTagNameMap.getOrDefault(videoId, Collections.emptyList()));
		reportInfo.setSourceList(videoSourceMap.getOrDefault(videoId, Collections.emptyList()));
		reportInfo.setPlayCount(videoStats == null ? 0L : defaultLong(videoStats.getPlayCount()));
		reportInfo.setLikeCount(videoStats == null ? 0L : defaultLong(videoStats.getLikeCount()));
		reportInfo.setCollectCount(videoStats == null ? 0L : defaultLong(videoStats.getCollectCount()));
		reportInfo.setCommentCount(videoStats == null ? 0L : defaultLong(videoStats.getCommentCount()));
		reportInfo.setShareCount(videoStats == null ? 0L : defaultLong(videoStats.getShareCount()));
		reportInfo.setCreateTime(video.getCreateTime() == null ? null : video.getCreateTime().toString());
		return reportInfo;
	}

	private CommentReportInfoVo buildCommentReportInfo(Long commentId,
			Map<Long, CommentPo> commentMap,
			Map<Long, CommentPo> rootCommentMap,
			Map<Long, VideoPo> videoMap,
			Map<Long, UserPo> userMap) {
		CommentPo commentPo = commentMap.get(commentId);
		if (commentPo == null) {
			return null;
		}
		UserPo commentAuthor = userMap.get(commentPo.getUserId());
		CommentPo rootComment = commentPo.getRootId() != null ? rootCommentMap.get(commentPo.getRootId()) : null;
		VideoPo relatedVideo = videoMap.get(commentPo.getVideoId());
		CommentReportInfoVo reportInfo = new CommentReportInfoVo();
		if (commentAuthor != null) {
			fillBaseUserReportInfo(reportInfo, commentAuthor);
		}
		reportInfo.setCommentId(commentPo.getId());
		reportInfo.setContent(commentPo.getContent());
		reportInfo.setCreateTime(commentPo.getCreateTime());
		reportInfo.setIsRootComment(commentPo.getRootId() == null || commentPo.getRootId() == 0);
		reportInfo.setRootCommentContent(rootComment == null ? null : rootComment.getContent());
		reportInfo.setVideoId(commentPo.getVideoId() == null ? null : String.valueOf(commentPo.getVideoId()));
		reportInfo.setCoverUrl(relatedVideo == null ? null : relatedVideo.getCoverUrl());
		reportInfo.setDescription(relatedVideo == null ? null : relatedVideo.getDescription());
		reportInfo.setTitle(relatedVideo == null ? null : relatedVideo.getTitle());
		return reportInfo;
	}

	private void fillBaseUserReportInfo(ReportInfoVo reportInfo, UserPo userPo) {
		if (reportInfo == null || userPo == null) {
			return;
		}
		reportInfo.setUserId(userPo.getId());
		reportInfo.setUsername(userPo.getUsername());
		reportInfo.setAvatarUrl(userPo.getAvatarUrl());
		reportInfo.setBio(userPo.getBio());
		reportInfo.setStatus(userPo.getStatus());
	}

	private Map<Long, UserPo> queryUserMap(Set<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return userMapper.selectBatchIds(userIds).stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(UserPo::getId, userPo -> userPo, (left, right) -> left));
	}

	private Map<Long, UserStatsPo> queryUserStatsMap(Set<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return userStatsMapper.selectList(new LambdaQueryWrapper<UserStatsPo>()
						.in(UserStatsPo::getUserId, userIds))
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(UserStatsPo::getUserId, userStatsPo -> userStatsPo, (left, right) -> left));
	}

	private Map<Long, VideoPo> queryVideoMap(Set<Long> videoIds) {
		if (videoIds == null || videoIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return videoMapper.selectBatchIds(videoIds).stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(VideoPo::getId, videoPo -> videoPo, (left, right) -> left));
	}

	private Map<Long, VideoStatsPo> queryVideoStatsMap(Set<Long> videoIds) {
		if (videoIds == null || videoIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return videoStatsMapper.selectList(new LambdaQueryWrapper<VideoStatsPo>()
						.in(VideoStatsPo::getVideoId, videoIds))
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(VideoStatsPo::getVideoId, videoStatsPo -> videoStatsPo, (left, right) -> left));
	}

	private Map<Long, CommentPo> queryCommentMap(Set<Long> commentIds) {
		if (commentIds == null || commentIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return commentMapper.selectBatchIds(commentIds).stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(CommentPo::getId, commentPo -> commentPo, (left, right) -> left));
	}

	private Map<Long, VideoPartitionPo> queryPartitionMap(Set<Long> partitionIds) {
		if (partitionIds == null || partitionIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return videoPartitionMapper.selectBatchIds(partitionIds).stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(VideoPartitionPo::getId, partitionPo -> partitionPo, (left, right) -> left));
	}

	private Map<Long, List<String>> queryVideoTagNameMap(List<Long> videoIdList) {
		Map<Long, List<VideoTagVo>> videoTagMap = queryVideoTagMap(videoIdList);
		if (videoTagMap.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, List<String>> result = new HashMap<>();
		videoTagMap.forEach((videoId, tagList) -> result.put(videoId, tagList.stream()
				.map(VideoTagVo::getTagName)
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.toList())));
		return result;
	}

	private Long defaultLong(Long value) {
		return value == null ? 0L : value;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void reviewReport(ManagerReviewReportRequest request) {
		if (request == null || request.getReportId() == null || request.getOperation() == null) {
			throw new ErrorParamException("举报审核参数不完整");
		}
		ReportPo reportPo = reportMapper.selectById(request.getReportId());
		if (reportPo == null) {
			throw new NotFoundException("举报记录不存在");
		}
		if (!ReportStatusEnum.WAITING_AUDIT.getCode().equals(reportPo.getStatus())) {
			throw new ErrorOperationException("该举报已审核");
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
		if (ReportTargetTypeEnum.USER.getCode().equals(reportPo.getReportType())) {
			banUser(reportPo.getTargetId());
			return;
		}
		if (ReportTargetTypeEnum.VIDEO.getCode().equals(reportPo.getReportType())) {
			banVideo(reportPo.getTargetId());
			return;
		}
		if (ReportTargetTypeEnum.COMMENT.getCode().equals(reportPo.getReportType())) {
			removeCommentByReport(reportPo.getTargetId());
			return;
		}
		throw new ErrorParamException("举报目标类型非法");
	}

	private void removeCommentByReport(Long commentId) {
		CommentPo commentPo = commentMapper.selectById(commentId);
		if (commentPo == null || commentPo.getIsDeleted() != 0) {
			return;
		}
		if (commentPo.getRootId() == 0) {
			int removed = commentMapper.logicDeleteRootTree(commentPo.getId());
			if (removed > 0) {
				videoStatsMapper.updateCommentCount(commentPo.getVideoId(), -1);
			}
			return;
		}
		int removed = commentMapper.logicDeleteById(commentPo.getId());
		if (removed > 0) {
			commentStatsMapper.updateReplyCount(commentPo.getRootId(), -1);
		}
	}

	@Override
	public void deleteComment(Long commentId) {
		if (commentId == null) {
			throw new ErrorParamException("评论ID不能为空");
		}
		CommentPo commentPo = commentMapper.selectById(commentId);
		if (commentPo == null || commentPo.getIsDeleted() != 0) {
			throw new NotFoundException("评论不存在或已删除");
		}
		removeCommentByReport(commentId);
	}

	@Override
	public void banUser(Long userId) {
		UserPo userPo = userRepository.queryUserAnyStatusById(userId);
		if (userPo == null) {
			throw new NotFoundException("用户不存在");
		}
		if (UserStatusEnum.DELETED.getCode() == userPo.getStatus()) {
			throw new ErrorOperationException("用户已删除");
		}
		LambdaUpdateWrapper<UserPo> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(UserPo::getId, userId)
				.set(UserPo::getStatus, UserStatusEnum.BANNED.getCode())
				.set(UserPo::getUpdateTime, LocalDateTime.now());
		userMapper.update(null, updateWrapper);
		esSyncService.deleteUser(userId);
	}

	@Override
	public void unbanUser(Long userId) {
		UserPo userPo = userRepository.queryUserAnyStatusById(userId);
		if (userPo == null) {
			throw new NotFoundException("用户不存在");
		}
		LambdaUpdateWrapper<UserPo> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(UserPo::getId, userId)
				.set(UserPo::getStatus, UserStatusEnum.NORMAL.getCode());
		userMapper.update(null, updateWrapper);
		esSyncService.syncUser(userId);
	}

	@Override
	public void banVideo(Long videoId) {
		VideoPo videoPo = videoRepository.queryVideoById(videoId);
		if (videoPo == null) {
			throw new NotFoundException("视频不存在");
		}
		boolean publishedBeforeBan = Objects.equals(videoPo.getStatus(), VideoStatusEnum.PUBLISHED.getCode());
		videoRepository.updateVideoStatus(videoId, null, VideoStatusEnum.BANNED.getCode());
		if (publishedBeforeBan) {
			interactRepository.updateUserVideoNum(videoPo.getUserId(), -1L);
			esSyncService.syncUser(videoPo.getUserId());
		}
		esSyncService.deleteVideo(videoId);
	}

	@Override
	public void unbanVideo(Long videoId) {
		VideoPo videoPo = videoRepository.queryVideoById(videoId);
		if (videoPo == null) {
			throw new NotFoundException("视频不存在");
		}
		if (VideoStatusEnum.BANNED.getCode() != videoPo.getStatus()) {
			throw new ErrorOperationException("该视频未被封禁");
		}
		int updated = videoRepository.updateVideoStatus(videoId, VideoStatusEnum.BANNED.getCode(), VideoStatusEnum.AUDITING.getCode());
		if (updated > 0) {
			createAuditingRecord(videoId, videoPo.getUserId());
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteVideoPartition(Long partitionId) {
		if (partitionId == null) {
			throw new ErrorParamException("分区ID不能为空");
		}
		VideoPartitionPo partitionPo = videoPartitionMapper.selectById(partitionId);
		if (partitionPo == null) {
			throw new NotFoundException("视频分区不存在");
		}
		Long relatedVideoCount = videoMapper.selectCount(new LambdaQueryWrapper<VideoPo>()
				.eq(VideoPo::getPartitionId, partitionId));
		if (relatedVideoCount != null && relatedVideoCount > 0) {
			throw new ErrorOperationException("该分区下已有视频，不能删除");
		}
		videoPartitionMapper.deleteById(partitionId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteVideoTag(Long tagId) {
		if (tagId == null) {
			throw new ErrorParamException("标签ID不能为空");
		}
		VideoTagPo tagPo = videoTagMapper.selectById(tagId);
		if (tagPo == null) {
			throw new NotFoundException("视频标签不存在");
		}

		List<VideoTagRelPo> relList = videoTagRelMapper.selectList(new LambdaQueryWrapper<VideoTagRelPo>()
				.eq(VideoTagRelPo::getTagId, tagId));
		List<Long> affectedVideoIdList = relList.stream()
				.map(VideoTagRelPo::getVideoId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());

		videoTagRelMapper.delete(new LambdaQueryWrapper<VideoTagRelPo>()
				.eq(VideoTagRelPo::getTagId, tagId));
		videoTagMapper.deleteById(tagId);

		for (Long videoId : affectedVideoIdList) {
			VideoPo videoPo = videoRepository.queryVideoById(videoId);
			if (videoPo != null && Objects.equals(videoPo.getStatus(), VideoStatusEnum.PUBLISHED.getCode())) {
				esSyncService.syncVideo(videoId);
			}
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
			throw new ErrorParamException("视频审核参数不完整");
		}
		VideoPo videoPo = videoRepository.queryVideoById(videoId);
		if (videoPo == null) {
			throw new NotFoundException("视频不存在");
		}
		if (VideoStatusEnum.AUDITING.getCode() != videoPo.getStatus()) {
			throw new ErrorOperationException("当前视频不在审核中");
		}

		Long userId = UserHolderUtil.getUser().getUserId();
		if (request.getOperation() == 1) {
			int updated = videoRepository.updateVideoStatus(videoId,
					VideoStatusEnum.AUDITING.getCode(), VideoStatusEnum.AUDIT_PASSED.getCode());
			if (updated <= 0) {
				throw new ErrorOperationException("视频审核状态更新失败");
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
			throw new ErrorOperationException("视频发布失败，请稍后重试");
		}

		VideoPo videoPo = videoRepository.queryVideoById(videoId);
		interactRepository.updateUserVideoNum(videoPo.getUserId(), 1L);
		esSyncService.syncVideo(videoId);
		esSyncService.syncUser(videoPo.getUserId());
	}
}
