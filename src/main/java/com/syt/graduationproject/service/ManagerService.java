package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.ManagerAuditVideoRequest;
import com.syt.graduationproject.model.request.ManagerAuditVideoListRequest;
import com.syt.graduationproject.model.request.ManagerReportListRequest;
import com.syt.graduationproject.model.request.ManagerReviewReportRequest;
import com.syt.graduationproject.model.request.ManagerUserListRequest;
import com.syt.graduationproject.model.request.ManagerVideoPartitionListRequest;
import com.syt.graduationproject.model.request.ManagerVideoTagListRequest;
import com.syt.graduationproject.model.vo.ManagerVideoPartitionVo;
import com.syt.graduationproject.model.vo.ManagerUserVo;
import com.syt.graduationproject.model.vo.ManagerVideoTagVo;
import com.syt.graduationproject.model.vo.VideoAuditVo;
import com.syt.graduationproject.model.vo.report.ManagerReportRecordVo;
import com.syt.graduationproject.model.vo.Page.PageVo;

public interface ManagerService {

	PageVo<VideoAuditVo> queryAuditVideoList(ManagerAuditVideoListRequest request);

	PageVo<ManagerUserVo> queryUserList(ManagerUserListRequest request);

	PageVo<ManagerVideoPartitionVo> queryVideoPartitionList(ManagerVideoPartitionListRequest request);

	PageVo<ManagerVideoTagVo> queryVideoTagList(ManagerVideoTagListRequest request);

	PageVo<ManagerReportRecordVo> queryReportList(ManagerReportListRequest request);

	void reviewReport(ManagerReviewReportRequest request);

	void banUser(Long userId);

	void unbanUser(Long userId);

	void banVideo(Long videoId);

	void unbanVideo(Long videoId);

	void deleteVideoPartition(Long partitionId);

	void deleteVideoTag(Long tagId);

	void auditVideo(ManagerAuditVideoRequest request);

	/**
	 * 新增一条审核中记录
	 */
	void createAuditingRecord(Long videoId, Long applicantId);

	/**
	 * 完成最新一条审核中记录
	 */
	void completeLatestAuditingRecord(Long videoId, Integer targetStatus, Long reviewerId, String reviewNote);

	/**
	 * 管理员删除评论
	 */
	void deleteComment(Long commentId);
}
