package com.syt.graduationproject.service;

import com.syt.graduationproject.model.request.ManagerAuditVideoRequest;
import com.syt.graduationproject.model.request.ManagerAuditVideoListRequest;
import com.syt.graduationproject.model.request.ManagerReportListRequest;
import com.syt.graduationproject.model.request.ManagerReviewReportRequest;
import com.syt.graduationproject.model.vo.VideoAuditVo;
import com.syt.graduationproject.model.vo.ReportVo;
import com.syt.graduationproject.model.vo.PageVo;

public interface ManagerService {

	PageVo<VideoAuditVo> queryAuditVideoList(ManagerAuditVideoListRequest request);

	PageVo<ReportVo> queryReportList(ManagerReportListRequest request);

	void reviewReport(ManagerReviewReportRequest request);

	void banUser(Long userId);

	void unbanUser(Long userId);

	void banVideo(Long videoId);

	void unbanVideo(Long videoId);

	void auditVideo(ManagerAuditVideoRequest request);

	/**
	 * 新增一条审核中记录
	 */
	void createAuditingRecord(Long videoId, Long applicantId);

	/**
	 * 完成最新一条审核中记录
	 */
	void completeLatestAuditingRecord(Long videoId, Integer targetStatus, Long reviewerId, String reviewNote);
}
