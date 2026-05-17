# 毕业项目 E-R 图

---

## 图一：用户与认证模块

```mermaid
erDiagram
    User {
        bigint id PK
        bigint account UK
        varchar username
        varchar password
        varchar avatar_url
        varchar bio
        tinyint status
        timestamp create_time
        timestamp update_time
    }

    UserRole {
        bigint id PK
        bigint user_id FK
        bigint role_id FK
        timestamp create_time
        timestamp update_time
    }

    Role {
        bigint id PK
        varchar role_name
        varchar role_desc
        timestamp create_time
        timestamp update_time
    }

    UserWallet {
        bigint id PK
        bigint user_id FK
        bigint coin_balance
        timestamp create_time
        timestamp update_time
    }

    UserStats {
        bigint id PK
        bigint user_id FK
        bigint video_num
        bigint like_num
        bigint follow_num
        bigint fans_num
        bigint play_num
        tinyint is_deleted
        timestamp create_time
        timestamp update_time
    }

    FollowRecord {
        bigint id PK
        bigint follower_id FK
        bigint followee_id FK
        tinyint is_deleted
        timestamp create_time
        timestamp update_time
    }

    UserBlock {
        bigint id PK
        bigint user_id FK
        bigint blocked_user_id FK
        tinyint is_deleted
        timestamp create_time
        timestamp update_time
    }

    User ||--|| UserRole : "1:1 拥有"
    User ||--|| UserWallet : "1:1 拥有"
    User ||--|| UserStats : "1:1 拥有"
    User ||--o{ FollowRecord : "1:N 关注他人"
    User ||--o{ UserBlock : "1:N 拉黑他人"

    Role ||--o{ UserRole : "1:N 分配"
```

| 关系 | 类型 | 说明 |
|---|---|---|
| User ↔ UserRole | 1:1 | uk_user_id 唯一 |
| User ↔ UserWallet | 1:1 | uk_wallet_user_id 唯一 |
| User ↔ UserStats | 1:1 | 隐式一对一 |
| User → FollowRecord | 1:N | follower_id / followee_id |
| User → UserBlock | 1:N | user_id / blocked_user_id |
| Role → UserRole | 1:N | |

---

## 图二：视频与内容模块

```mermaid
erDiagram
    User {
        bigint id PK
        bigint account UK
        varchar username
        varchar password
        varchar avatar_url
        varchar bio
        tinyint status
        timestamp create_time
        timestamp update_time
    }

    Video {
        bigint id PK
        bigint user_id FK
        varchar title
        varchar description
        varchar cover_url
        int duration
        bigint partition_id FK
        tinyint status
        timestamp create_time
        timestamp update_time
    }

    VideoPartition {
        bigint id PK
        varchar partition_name
        timestamp create_time
        timestamp update_time
    }

    VideoStats {
        bigint id PK
        bigint video_id FK
        bigint play_count
        bigint like_count
        bigint coin_count
        bigint collect_count
        bigint share_count
        bigint comment_count
        tinyint is_deleted
        timestamp create_time
        timestamp update_time
    }

    VideoSource {
        bigint id PK
        bigint video_id FK
        tinyint resolution_code
        varchar play_url
        varchar format
        varchar codec
        tinyint is_deleted
        timestamp create_time
        timestamp update_time
    }

    VideoTag {
        bigint id PK
        varchar tag_name
        bigint hot
        timestamp create_time
        timestamp update_time
    }

    VideoTagRel {
        bigint id PK
        bigint video_id FK
        bigint tag_id FK
        timestamp create_time
        timestamp update_time
    }

    User ||--o{ Video : "1:N 发布"
    VideoPartition ||--o{ Video : "1:N 分类"
    Video ||--|| VideoStats : "1:1 拥有"
    Video ||--o{ VideoSource : "1:N 版本"

    Video ||--o{ VideoTagRel : "1:N"
    VideoTag ||--o{ VideoTagRel : "1:N"
    Video }o--o{ VideoTag : "M:N 标签"
```

| 关系 | 类型 | 说明 |
|---|---|---|
| User → Video | 1:N | |
| VideoPartition → Video | 1:N | |
| Video ↔ VideoStats | 1:1 | |
| Video → VideoSource | 1:N | 多清晰度文件 |
| Video ↔ VideoTag | M:N | 通过 VideoTagRel, uk_video_tag |

---

## 图三：社交互动模块

```mermaid
erDiagram
    User {
        bigint id PK
        bigint account UK
        varchar username
        varchar password
        varchar avatar_url
        varchar bio
        tinyint status
        timestamp create_time
        timestamp update_time
    }

    Video {
        bigint id PK
        bigint user_id FK
        varchar title
        varchar description
        varchar cover_url
        int duration
        bigint partition_id FK
        tinyint status
        timestamp create_time
        timestamp update_time
    }

    Comment {
        bigint id PK
        bigint video_id FK
        bigint user_id FK
        bigint root_id
        bigint parent_id FK
        bigint reply_user_id
        varchar content
        tinyint is_deleted
        tinyint is_top
        timestamp create_time
        timestamp update_time
    }

    CommentStats {
        bigint id PK
        bigint video_id FK
        bigint comment_id FK
        bigint like_count
        bigint reply_count
        bigint hot_score
        timestamp create_time
        timestamp update_time
    }

    LikeVideo {
        bigint id PK
        bigint user_id FK
        bigint video_id FK
        timestamp create_time
    }

    LikeComment {
        bigint id PK
        bigint user_id FK
        bigint comment_id FK
        timestamp create_time
    }

    CollectionDirectory {
        bigint id PK
        bigint user_id FK
        varchar name
        varchar description
        varchar cover_url
        tinyint is_public
        tinyint is_default
        tinyint is_deleted
        timestamp create_time
        timestamp update_time
    }

    CollectionItem {
        bigint id PK
        bigint directory_id FK
        bigint user_id FK
        bigint video_id FK
        tinyint is_deleted
        timestamp create_time
        timestamp update_time
    }

    VideoShareRecord {
        bigint id PK
        bigint video_id FK
        bigint user_id FK
        timestamp create_time
        timestamp update_time
    }

    UserVideoHistory {
        bigint id PK
        bigint user_id FK
        bigint video_id FK
        int last_play_time
        int duration
        tinyint is_finished
        tinyint is_deleted
        timestamp create_time
        timestamp update_time
    }

    User ||--o{ Comment : "1:N 发表"
    Video ||--o{ Comment : "1:N 包含"
    Comment ||--|| CommentStats : "1:1 拥有"
    Comment ||--o{ Comment : "1:N 回复自引用"

    User ||--o{ LikeVideo : "1:N"
    Video ||--o{ LikeVideo : "1:N"
    User }o--o{ Video : "M:N 点赞"

    User ||--o{ LikeComment : "1:N"
    Comment ||--o{ LikeComment : "1:N"
    User }o--o{ Comment : "M:N 点赞评论"

    User ||--o{ CollectionDirectory : "1:N 创建"
    CollectionDirectory ||--o{ CollectionItem : "1:N 包含"
    User ||--o{ CollectionItem : "1:N"
    Video ||--o{ CollectionItem : "1:N"
    User }o--o{ Video : "M:N 收藏"

    User ||--o{ VideoShareRecord : "1:N"
    Video ||--o{ VideoShareRecord : "1:N"
    User }o--o{ Video : "M:N 分享"

    User ||--o{ UserVideoHistory : "1:N"
    Video ||--o{ UserVideoHistory : "1:N"
    User }o--o{ Video : "M:N 观看历史"
```

| 中间表 | 拆解关系 |
|---|---|
| **LikeVideo** | User 1:N LikeVideo, Video 1:N LikeVideo → User ↔ Video M:N |
| **LikeComment** | User 1:N LikeComment, Comment 1:N LikeComment → User ↔ Comment M:N |
| **CollectionItem** | User 1:N CollectionItem, Video 1:N CollectionItem, CollectionDirectory 1:N CollectionItem → User ↔ Video M:N（挂在收藏夹下） |
| **VideoShareRecord** | User 1:N VideoShareRecord, Video 1:N VideoShareRecord → User ↔ Video M:N（uk_video_user_share 去重） |
| **UserVideoHistory** | User 1:N UserVideoHistory, Video 1:N UserVideoHistory → User ↔ Video M:N |

| 其他关系 | 类型 | 说明 |
|---|---|---|
| User → Comment | 1:N | |
| Video → Comment | 1:N | |
| Comment ↔ CommentStats | 1:1 | |
| Comment → Comment（自引用） | 1:N | parent_id，两级嵌套 |
| User → CollectionDirectory | 1:N | |
| CollectionDirectory → CollectionItem | 1:N | |

---

## 图四：管理与审核模块

```mermaid
erDiagram
    User {
        bigint id PK
        bigint account UK
        varchar username
        varchar password
        varchar avatar_url
        varchar bio
        tinyint status
        timestamp create_time
        timestamp update_time
    }

    Video {
        bigint id PK
        bigint user_id FK
        varchar title
        varchar description
        varchar cover_url
        int duration
        bigint partition_id FK
        tinyint status
        timestamp create_time
        timestamp update_time
    }

    Comment {
        bigint id PK
        bigint video_id FK
        bigint user_id FK
        bigint root_id
        bigint parent_id FK
        bigint reply_user_id
        varchar content
        tinyint is_deleted
        tinyint is_top
        timestamp create_time
        timestamp update_time
    }

    PrivateMessage {
        bigint id PK
        bigint from_user_id FK
        bigint to_user_id FK
        varchar client_msg_id
        varchar content
        tinyint status
        tinyint fail_reason
        timestamp delivered_time
        timestamp acked_time
        timestamp read_time
        timestamp create_time
        timestamp update_time
    }

    Report {
        bigint id PK
        bigint reporter_id FK
        tinyint report_type
        bigint target_id
        varchar reason
        varchar detail
        tinyint status
        bigint reviewer_id
        varchar review_note
        timestamp create_time
        timestamp update_time
    }

    VideoAuditRecord {
        bigint id PK
        bigint video_id FK
        bigint applicant_id FK
        tinyint status
        bigint reviewer_id
        varchar review_note
        timestamp create_time
        timestamp update_time
    }

    UserCoinChangeLog {
        bigint id PK
        bigint user_id FK
        int change_amount
        tinyint change_type
        bigint related_target_id
        timestamp create_time
        timestamp update_time
    }

    User ||--o{ PrivateMessage : "1:N 发送"
    User ||--o{ PrivateMessage : "1:N 接收"

    User ||--o{ Report : "1:N 举报"
    User ||--o{ VideoAuditRecord : "1:N 提交审核"

    User ||--o{ UserCoinChangeLog : "1:N 金币变更"

    Report }o--|| User : "N:1 被举报"
    Report }o--|| Video : "N:1 被举报"
    Report }o--|| Comment : "N:1 被举报"
```

| 关系 | 类型 | 说明 |
|---|---|---|
| User → PrivateMessage（发送） | 1:N | from_user_id |
| User → PrivateMessage（接收） | 1:N | to_user_id |
| User → Report | 1:N | reporter_id |
| User → VideoAuditRecord | 1:N | applicant_id |
| User → UserCoinChangeLog | 1:N | |
| Report → User/Video/Comment | N:1 | 多态关联，report_type(1/2/3) + target_id |
