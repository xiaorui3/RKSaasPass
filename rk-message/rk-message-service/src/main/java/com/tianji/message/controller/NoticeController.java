package com.tianji.message.controller;

import com.tianji.common.domain.R;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.utils.UserContext;
import com.tianji.message.domain.dto.NoticeDTO;
import com.tianji.message.domain.po.Notice;
import com.tianji.message.service.INoticeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通知公告控制器
 */
@Slf4j
@Api(tags = "通知公告接口")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final INoticeService noticeService;

    /**
     * 获取通知列表（全部）
     */
    @ApiOperation("获取通知列表")
    @GetMapping("/list")
    public R<List<Notice>> getNoticeList() {
        try {
            List<Notice> notices = noticeService.getNoticeList();
            return R.ok(notices);
        } catch (Exception e) {
            log.error("获取通知列表失败", e);
            return R.error("获取通知列表失败");
        }
    }

    /**
     * 获取已发布通知列表
     */
    @ApiOperation("获取已发布通知列表")
    @GetMapping("/published")
    public R<List<Notice>> getPublishedNotices() {
        try {
            List<Notice> notices = noticeService.getPublishedNotices();
            return R.ok(notices);
        } catch (Exception e) {
            log.error("获取已发布通知列表失败", e);
            return R.error("获取已发布通知列表失败");
        }
    }

    /**
     * 获取通知详情
     */
    @ApiOperation("获取通知详情")
    @GetMapping("/{id}")
    public R<Notice> getNoticeDetail(@PathVariable Long id) {
        try {
            Notice notice = noticeService.getNoticeDetail(id);
            if (notice == null) {
                return R.error("通知不存在");
            }
            return R.ok(notice);
        } catch (Exception e) {
            log.error("获取通知详情失败", e);
            return R.error("获取通知详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建通知
     */
    @ApiOperation("创建通知")
    @PostMapping("/create")
    public R<String> createNotice(@RequestBody NoticeDTO noticeDTO) {
        try {
            if (noticeDTO.getTitle() == null || noticeDTO.getTitle().isEmpty()) {
                return R.error("通知标题不能为空");
            }
            if (noticeDTO.getContent() == null || noticeDTO.getContent().isEmpty()) {
                return R.error("通知内容不能为空");
            }
            noticeService.createNotice(noticeDTO);
            log.info("通知创建成功: {}", noticeDTO.getTitle());
            return R.ok("创建成功");
        } catch (Exception e) {
            log.error("创建通知失败", e);
            return R.error("创建通知失败：" + e.getMessage());
        }
    }

    /**
     * 更新通知
     */
    @ApiOperation("更新通知")
    @PutMapping("/update")
    public R<String> updateNotice(@RequestBody NoticeDTO noticeDTO) {
        try {
            if (noticeDTO.getId() == null) {
                return R.error("通知ID不能为空");
            }
            noticeService.updateNotice(noticeDTO.getId(), noticeDTO);
            log.info("通知更新成功，ID: {}", noticeDTO.getId());
            return R.ok("更新成功");
        } catch (Exception e) {
            log.error("更新通知失败", e);
            return R.error("更新通知失败：" + e.getMessage());
        }
    }

    /**
     * 发布通知（将草稿状态改为已发布）
     */
    @ApiOperation("发布通知")
    @PutMapping("/{id}/publish")
    public R<String> publishNotice(@PathVariable Long id) {
        try {
            noticeService.publishNotice(id);
            log.info("通知发布成功，ID: {}", id);
            return R.ok("发布成功");
        } catch (Exception e) {
            log.error("通知发布失败", e);
            return R.error("通知发布失败：" + e.getMessage());
        }
    }

    /**
     * 撤回通知（将已发布状态改为草稿）
     */
    @ApiOperation("撤回通知")
    @PutMapping("/{id}/withdraw")
    public R<String> withdrawNotice(@PathVariable Long id) {
        try {
            Notice notice = noticeService.getNoticeDetail(id);
            if (notice == null) {
                return R.error("通知不存在");
            }
            noticeService.withdrawNotice(id);
            log.info("通知撤回成功，ID: {}", id);
            return R.ok("撤回成功");
        } catch (Exception e) {
            log.error("通知撤回失败", e);
            return R.error("通知撤回失败：" + e.getMessage());
        }
    }

    /**
     * 更新置顶状态
     */
    @ApiOperation("更新置顶状态")
    @PutMapping("/{id}/top")
    public R<String> updateTopStatus(@PathVariable Long id, @RequestParam Boolean isTop) {
        try {
            Notice notice = noticeService.getNoticeDetail(id);
            if (notice == null) {
                return R.error("通知不存在");
            }
            noticeService.updateTopStatus(id, isTop);
            log.info("通知置顶状态更新成功，ID: {}, isTop: {}", id, isTop);
            return R.ok(isTop ? "已置顶" : "取消置顶");
        } catch (Exception e) {
            log.error("更新置顶状态失败", e);
            return R.error("更新置顶状态失败：" + e.getMessage());
        }
    }

    /**
     * 删除通知
     */
    @ApiOperation("删除通知")
    @DeleteMapping("/{id}")
    public R<String> deleteNotice(@PathVariable Long id) {
        try {
            noticeService.deleteNotice(id);
            log.info("通知删除成功，ID: {}", id);
            return R.ok("删除成功");
        } catch (Exception e) {
            log.error("通知删除失败", e);
            return R.error("通知删除失败：" + e.getMessage());
        }
    }

    @ApiOperation("负责人审核通知")
    @PostMapping("/{id}/review/manager")
    public R<String> reviewNoticeByManager(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!Objects.equals(roleId, 1L) && !Objects.equals(roleId, 7L)) {
                return R.error("no manager review permission");
            }
            boolean success = noticeService.reviewNoticeByManager(id, extractApproved(body), extractRemark(body), UserContext.getUser(), Objects.equals(roleId, 1L));
            return success ? R.ok("manager review approved") : R.error("manager review failed");
        } catch (Exception e) {
            log.error("manager review notice failed, id: {}", id, e);
            return R.error("manager review failed: " + e.getMessage());
        }
    }

    @ApiOperation("指导老师审核通知")
    @PostMapping("/{id}/review/teacher")
    public R<String> reviewNoticeByTeacher(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-Role-Id", required = false) Long roleId) {
        try {
            if (!Objects.equals(roleId, 1L) && !Objects.equals(roleId, 8L)) {
                return R.error("no teacher review permission");
            }
            boolean success = noticeService.reviewNoticeByTeacher(id, extractApproved(body), extractRemark(body), UserContext.getUser(), Objects.equals(roleId, 1L));
            return success ? R.ok("teacher review approved") : R.error("teacher review failed");
        } catch (Exception e) {
            log.error("teacher review notice failed, id: {}", id, e);
            return R.error("teacher review failed: " + e.getMessage());
        }
    }

    @ApiOperation("鍒嗛〉鑾峰彇閫氱煡鍒楄〃")
    @GetMapping("/page")
    public R<PageDTO<Notice>> getNoticePage(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "size", required = false) Integer size) {
        try {
            int currentPage = page != null ? page : (pageNum != null ? pageNum : 1);
            int currentSize = pageSize != null ? pageSize : (size != null ? size : 10);
            return R.ok(noticeService.getNoticePage(currentPage, currentSize));
        } catch (Exception e) {
            log.error("鍒嗛〉鑾峰彇閫氱煡鍒楄〃澶辫触", e);
            return R.error("鍒嗛〉鑾峰彇閫氱煡鍒楄〃澶辫触");
        }
    }

    private String extractRemark(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object value = body.get("reviewComment");
        if (value == null) {
            value = body.get("remark");
        }
        return value == null ? null : String.valueOf(value);
    }

    private boolean extractApproved(Map<String, Object> body) {
        if (body == null || !body.containsKey("approved")) {
            return true;
        }
        Object value = body.get("approved");
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
