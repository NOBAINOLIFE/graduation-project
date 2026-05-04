# InteractRelationService 使用说明

## 概述

`InteractRelationService` 是一个专门用于查询互动关系的服务，提供了用户与视频、用户之间的各种互动状态查询功能。

## 主要功能

### 1. 点赞相关

```java
// 查询用户是否点赞了指定视频
boolean isLiked = interactRelationService.isLikedVideo(userId, videoId);
```

### 2. 收藏相关

```java
// 查询用户是否收藏了指定视频
boolean isCollected = interactRelationService.isCollectedVideo(userId, videoId);
```

### 3. 关注关系

```java
// 查询两个用户之间的关注关系
boolean isFollowing = interactRelationService.isFollowing(followerId, followeeId);

// 查询两个用户之间是否互相关注
boolean isMutualFollow = interactRelationService.isMutualFollow(userA, userB);
```

### 4. 投币相关

```java
// 查询用户对视频的投币数量
Integer coinAmount = interactRelationService.getUserCoinAmountForVideo(userId, videoId);

// 查询视频的总投币数
Integer totalCoins = interactRelationService.getVideoTotalCoins(videoId);
```

### 5. 综合查询

```java
// 查询用户与视频的完整交互信息
UserVideoInteractionBo interaction = interactRelationService.queryUserVideoInteraction(userId, videoId);
// 返回包含:
// - userId: 用户ID
// - videoId: 视频ID
// - isLiked: 是否点赞
// - isCollected: 是否收藏
// - coinAmount: 投币数量
```

## 架构设计

### Service层
- `InteractRelationService`: 服务接口
- `InteractRelationServiceImpl`: 服务实现，负责业务逻辑处理

### Repository层
- `InteractRelationRepository`: 仓库接口，负责SQL组装
- `InteractRelationRepositoryImpl`: 仓库实现，使用MyBatis-Plus构建查询条件

### 数据模型
- `UserVideoInteractionBo`: 用户视频交互信息业务对象

## 使用场景

1. **视频播放页**: 显示当前用户对视频的互动状态（是否点赞、收藏、投币）
2. **用户主页**: 显示用户之间的关注关系
3. **视频列表**: 批量查询用户对多个视频的互动状态
4. **消息中心**: 判断用户之间的互动关系

## 注意事项

1. 所有方法都进行了空值检查，传入null参数会返回默认值（false或0）
2. Repository层使用MyBatis-Plus的LambdaQueryWrapper构建类型安全的查询条件
3. 收藏状态只查询未删除的记录（isDeleted = 0）
4. 投币数量通过查询用户硬币变更日志统计

## 扩展示例

如果需要添加新的互动关系查询，可以按以下步骤：

1. 在 `InteractRelationRepository` 接口中添加新方法
2. 在 `InteractRelationRepositoryImpl` 中实现该方法，使用MyBatis-Plus构建SQL
3. 在 `InteractRelationService` 接口中添加业务方法
4. 在 `InteractRelationServiceImpl` 中实现业务逻辑

例如，添加"是否分享过视频"的查询：

```java
// Repository接口
Boolean hasSharedVideo(Long userId, Long videoId);

// Repository实现
@Override
public Boolean hasSharedVideo(Long userId, Long videoId) {
    LambdaQueryWrapper<VideoShareRecordPo> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(VideoShareRecordPo::getUserId, userId)
            .eq(VideoShareRecordPo::getVideoId, videoId)
            .last("LIMIT 1");
    return videoShareRecordMapper.selectOne(queryWrapper) != null;
}

// Service接口
boolean hasSharedVideo(Long userId, Long videoId);

// Service实现
@Override
public boolean hasSharedVideo(Long userId, Long videoId) {
    if (userId == null || videoId == null) {
        return false;
    }
    return interactRelationRepository.hasSharedVideo(userId, videoId);
}
```
