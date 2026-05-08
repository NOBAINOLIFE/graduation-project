create table `graduation-project`.tb_collection_directory
(
    id          bigint auto_increment comment '自增ID'
        primary key,
    user_id     bigint                              not null comment '所属用户ID',
    name        varchar(64)                         not null comment '收藏夹名称',
    description varchar(255)                        null comment '描述',
    cover_url   varchar(255)                        null comment '封面图',
    is_public   tinyint   default 0                 null comment '是否公开：0-私有，1-公开',
    is_default  tinyint   default 0                 not null comment '0-no 1-yes',
    is_deleted  tinyint   default 0                 not null comment '状态 0-未删除 1-已删除',
    create_time timestamp default CURRENT_TIMESTAMP not null,
    update_time timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
)
    comment '收藏夹目录表';

create table `graduation-project`.tb_collection_item
(
    id           bigint auto_increment
        primary key,
    directory_id bigint                              not null comment '所属收藏夹ID',
    user_id      bigint                              not null comment '用户ID',
    video_id     bigint                              not null comment '视频ID',
    is_deleted   tinyint   default 0                 not null comment '状态 0-未删除 1-已删除',
    create_time  timestamp default CURRENT_TIMESTAMP not null,
    update_time  timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_directory_video_user
        unique (directory_id, user_id, video_id)
)
    comment '收藏内容明细表';

create table `graduation-project`.tb_comment
(
    id            bigint auto_increment
        primary key,
    video_id      bigint                              not null comment '所属内容ID（如视频ID）',
    user_id       bigint                              not null comment '评论者ID',
    root_id       bigint    default 0                 null comment '根评论ID：0表示自己是根评论，非0表示属于哪条主评论',
    parent_id     bigint    default 0                 null comment '父评论ID：回复的是哪一条具体评论',
    reply_user_id bigint    default 0                 null comment '被回复者ID（冗余，方便显示：回复 @xxx）',
    content       varchar(1000)                       not null comment '评论内容',
    is_top        tinyint   default 0                 not null comment '是否置顶：1-置顶，0-未置顶',
    is_deleted    tinyint   default 0                 null comment '手动逻辑删除标志',
    create_time   timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '二级评论表';

create index idx_comment_video_root_top
    on `graduation-project`.tb_comment (video_id, root_id, is_top, is_deleted, create_time, id);

create index idx_video_root
    on `graduation-project`.tb_comment (video_id, root_id);

create table `graduation-project`.tb_comment_stats
(
    id          bigint auto_increment comment '主键'
        primary key,
    video_id    bigint                              not null comment '关联视频ID，冗余字段，用于按热度排序使用',
    comment_id  bigint                              not null comment '评论ID',
    like_count  bigint    default 0                 not null comment '点赞数',
    reply_count bigint    default 0                 not null comment '根评论下的总回复数，非跟评论不维护',
    hot_score   bigint    default 10                not null comment '评论热度分',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '评论数据统计表';

create index idx_video_hot_cursor
    on `graduation-project`.tb_comment_stats (video_id asc, hot_score desc, create_time desc, comment_id desc);

create table `graduation-project`.tb_follow_record
(
    id          bigint auto_increment comment '唯一 id'
        primary key,
    follower_id bigint                              not null comment '关注者 ID',
    followee_id bigint                              not null comment '被关注者 ID',
    is_deleted  tinyint   default 0                 not null comment '状态 0-未删除 1-已删除',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '关注记录表';

create table `graduation-project`.tb_like_comment
(
    id          bigint auto_increment comment '唯一 id'
        primary key,
    user_id     bigint                              not null comment '点赞用户 ID',
    comment_id  bigint                              not null comment '评论 ID',
    create_time timestamp default CURRENT_TIMESTAMP not null,
    constraint uk_user_comment
        unique (user_id, comment_id)
);

create table `graduation-project`.tb_like_video
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                              not null comment '点赞人ID',
    video_id    bigint                              not null comment '点赞视频ID',
    create_time timestamp default CURRENT_TIMESTAMP not null,
    constraint uk_user_video
        unique (user_id, video_id)
);

create table `graduation-project`.tb_private_message
(
    id             bigint auto_increment comment '主键ID（服务端消息ID）'
        primary key,
    from_user_id   bigint                              not null comment '发送方用户ID',
    to_user_id     bigint                              not null comment '接收方用户ID',
    client_msg_id  varchar(64)                         null comment '前端消息ID（幂等/回执关联）',
    content        varchar(1024)                       not null comment '文本内容',
    status         tinyint   default 0                 not null comment '状态：0-SAVED 1-DELIVERED 2-ACKED 3-FAIL',
    fail_reason    tinyint   default 0                 not null comment '失败原因：0-无 1-系统异常 2-发送方被封禁 3-接收方被封禁 4-发送方拉黑接收方 5-接收方拉黑发送方',
    delivered_time timestamp                           null comment '投递时间',
    acked_time     timestamp                           null comment '确认时间',
    read_time      timestamp                           null comment '已读时间',
    create_time    timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_from_client_msg
        unique (from_user_id, client_msg_id)
)
    comment '私信消息表';

create index idx_from_user_time
    on `graduation-project`.tb_private_message (from_user_id, create_time);

create index idx_to_user_time
    on `graduation-project`.tb_private_message (to_user_id, create_time);

create table `graduation-project`.tb_report
(
    id          bigint auto_increment
        primary key,
    reporter_id bigint                              not null,
    report_type tinyint                             not null comment '1-user 2-video',
    target_id   bigint                              not null,
    reason      varchar(256)                        not null,
    detail      varchar(1024)                       null,
    status      tinyint   default 0                 not null comment '0-waiting 1-approved 2-rejected',
    reviewer_id bigint                              null,
    review_note varchar(512)                        null,
    create_time timestamp default CURRENT_TIMESTAMP null,
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
);

create index idx_reporter_id
    on `graduation-project`.tb_report (reporter_id);

create index idx_status
    on `graduation-project`.tb_report (status);

create index idx_target
    on `graduation-project`.tb_report (report_type, target_id);

create table `graduation-project`.tb_role
(
    id          bigint auto_increment comment '角色ID，主键自增'
        primary key,
    role_name   varchar(50)                         not null comment '角色名称',
    role_desc   varchar(100)                        not null comment '角色描述',
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '角色表';

create table `graduation-project`.tb_user
(
    id          bigint auto_increment comment '唯一id'
        primary key,
    account     bigint                              not null comment '账号',
    username    varchar(256)                        not null comment '用户名',
    password    varchar(256)                        not null comment '密码',
    avatar_url  varchar(512)                        null comment '头像地址',
    bio         varchar(255)                        not null comment '个人简介',
    status      tinyint   default 0                 not null comment '状态 0-正常 1-封禁 2-删除',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '用户信息表';

create table `graduation-project`.tb_user_block
(
    id              bigint auto_increment
        primary key,
    user_id         bigint                              not null,
    blocked_user_id bigint                              not null,
    is_deleted      tinyint   default 0                 not null,
    create_time     timestamp default CURRENT_TIMESTAMP null,
    update_time     timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_user_block
        unique (user_id, blocked_user_id)
);

create index idx_blocked_user_id
    on `graduation-project`.tb_user_block (blocked_user_id);

create table `graduation-project`.tb_user_coin_change_log
(
    id                bigint auto_increment
        primary key,
    user_id           bigint                              not null,
    change_amount     int                                 not null comment '正数为增加，负数为减少',
    change_type       tinyint                             not null comment '1-每日登录奖励 2-给视频投币',
    related_target_id bigint                              null comment '关联对象ID，例如视频ID',
    create_time       timestamp default CURRENT_TIMESTAMP null,
    update_time       timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
);

create index idx_user_time
    on `graduation-project`.tb_user_coin_change_log (user_id, create_time);

create index idx_user_type
    on `graduation-project`.tb_user_coin_change_log (user_id, change_type);

create table `graduation-project`.tb_user_role_rel
(
    id          bigint auto_increment comment '主键自增'
        primary key,
    user_id     bigint                              not null comment '用户ID',
    role_id     bigint                              not null comment '角色ID',
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint tb_user_role_rel_user_id_role_id_uindex
        unique (user_id, role_id)
)
    comment '用户角色关联表';

create table `graduation-project`.tb_user_stats
(
    id          bigint auto_increment comment '自增ID'
        primary key,
    user_id     bigint                              not null comment '用户ID',
    video_num   bigint    default 0                 not null comment '视频数量',
    like_num    bigint    default 0                 not null comment '点赞数量',
    follow_num  bigint    default 0                 not null comment '关注人数',
    fans_num    bigint    default 0                 not null comment '粉丝人数',
    play_num    bigint    default 0                 not null,
    is_deleted  tinyint   default 0                 not null comment '状态 0-未删除 1-已删除',
    create_time timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '用户数据统计表';

create table `graduation-project`.tb_user_video_history
(
    id             bigint auto_increment comment '唯一 id'
        primary key,
    user_id        bigint                              not null comment '用户ID',
    video_id       bigint                              not null comment '视频ID',
    last_play_time int       default 0                 not null comment '最后播放位置（秒）',
    duration       int       default 0                 not null comment '视频总时长（秒）',
    is_finished    tinyint   default 0                 null comment '是否看完',
    is_deleted     tinyint   default 0                 not null comment '状态 0-未删除 1-已删除',
    create_time    timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_user_video
        unique (user_id, video_id)
)
    comment '用户观看历史与进度表';

create index idx_user_update
    on `graduation-project`.tb_user_video_history (user_id, update_time);

create table `graduation-project`.tb_user_wallet
(
    id           bigint auto_increment
        primary key,
    user_id      bigint                              not null,
    coin_balance bigint    default 0                 not null,
    create_time  timestamp default CURRENT_TIMESTAMP null,
    update_time  timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_wallet_user_id
        unique (user_id)
);

create table `graduation-project`.tb_video
(
    id           bigint auto_increment comment '唯一 id'
        primary key,
    user_id      bigint                              not null comment '发布者 ID',
    title        varchar(256)                        null comment '视频标题',
    description  varchar(1024)                       null comment '视频描述',
    cover_url    varchar(512)                        null comment '封面地址',
    duration     int       default 0                 null comment '视频时长 (秒)',
    partition_id bigint                              null comment '视频分区ID',
    status       tinyint   default 0                 not null comment '状态 0-待上传 1-上传完成 2-转码中 3-审核中 4-已发布 5-封禁 6-删除',
    create_time  timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '视频信息表';

create index idx_partition_id
    on `graduation-project`.tb_video (partition_id);

create table `graduation-project`.tb_video_audit_record
(
    id           bigint auto_increment
        primary key,
    video_id     bigint                              not null,
    applicant_id bigint                              not null,
    status       tinyint   default 0                 not null comment '0-auditing 1-passed 2-rejected',
    reviewer_id  bigint                              null,
    review_note  varchar(512)                        null,
    create_time  timestamp default CURRENT_TIMESTAMP null,
    update_time  timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
);

create index idx_status
    on `graduation-project`.tb_video_audit_record (status);

create index idx_video_id
    on `graduation-project`.tb_video_audit_record (video_id);

create table `graduation-project`.tb_video_partition
(
    id             bigint auto_increment
        primary key,
    partition_name varchar(64)                         not null,
    create_time    timestamp default CURRENT_TIMESTAMP null,
    update_time    timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_partition_name
        unique (partition_name)
);

create table `graduation-project`.tb_video_share_record
(
    id          bigint auto_increment
        primary key,
    video_id    bigint                              not null,
    user_id     bigint                              not null,
    create_time timestamp default CURRENT_TIMESTAMP null,
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_video_user_share
        unique (video_id, user_id)
);

create index idx_user_id
    on `graduation-project`.tb_video_share_record (user_id);

create table `graduation-project`.tb_video_source
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    video_id        bigint                                not null comment '关联视频主表ID',
    resolution_code tinyint                               not null comment '清晰度标签: 360P, 720P, 1080P, 原画',
    play_url        varchar(500)                          not null comment 'MinIO中的相对路径',
    format          varchar(10) default 'mp4'             null comment '视频格式: mp4, m3u8, flv',
    codec           varchar(20) default 'h264'            null comment '编码格式: h264, h265',
    is_deleted      tinyint     default 0                 not null,
    create_time     timestamp   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     timestamp   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '视频清晰度资源表';

create index idx_video_id
    on `graduation-project`.tb_video_source (video_id);

create table `graduation-project`.tb_video_stats
(
    id            bigint auto_increment comment '自增ID'
        primary key,
    video_id      bigint                              not null comment '视频ID',
    play_count    bigint    default 0                 not null comment '播放量',
    like_count    bigint    default 0                 not null comment '点赞数',
    coin_count    bigint    default 0                 not null comment '投币数',
    collect_count bigint    default 0                 not null comment '收藏数',
    share_count   bigint    default 0                 not null comment '分享数',
    comment_count bigint    default 0                 not null comment '评论数',
    is_deleted    tinyint   default 0                 not null comment '状态 0-未删除 1-已删除',
    create_time   timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
)
    comment '视频数据统计表';

create table `graduation-project`.tb_video_tag
(
    id          bigint auto_increment
        primary key,
    tag_name    varchar(64)                         not null,
    hot         bigint    default 0                 not null comment '标签热度，引用次数累计值',
    create_time timestamp default CURRENT_TIMESTAMP null,
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_tag_name
        unique (tag_name)
);

create table `graduation-project`.tb_video_tag_rel
(
    id          bigint auto_increment
        primary key,
    video_id    bigint                              not null,
    tag_id      bigint                              not null,
    create_time timestamp default CURRENT_TIMESTAMP null,
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_video_tag
        unique (video_id, tag_id)
);

create index idx_tag_id
    on `graduation-project`.tb_video_tag_rel (tag_id);

