package com.syt.graduationproject.aspect;

import com.syt.graduationproject.annotation.RequirePermission;
import com.syt.graduationproject.enums.RoleEnum;
import com.syt.graduationproject.exception.NoPermissionException;
import com.syt.graduationproject.model.dto.UserDto;
import com.syt.graduationproject.util.UserHolderUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import static com.syt.graduationproject.constant.UserConstant.ADMIN_PERMISSION;
import static com.syt.graduationproject.constant.UserConstant.USER_PERMISSION;

@Aspect
@Component
public class PermissionAspect {

    @Around("@within(com.syt.graduationproject.annotation.RequirePermission) || @annotation(com.syt.graduationproject.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        UserDto currentUser = UserHolderUtil.getUser();
        if (currentUser == null) {
            // 公开路径（拦截器已放行），不做角色校验
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequirePermission methodAnnotation = signature.getMethod().getAnnotation(RequirePermission.class);
        RequirePermission classAnnotation = joinPoint.getTarget().getClass().getAnnotation(RequirePermission.class);

        // 方法级注解优先于类级注解
        RequirePermission effectiveAnnotation = methodAnnotation != null ? methodAnnotation : classAnnotation;
        if (effectiveAnnotation == null) {
            return joinPoint.proceed();
        }

        String requiredPermission = effectiveAnnotation.value();
        String userRoleCode = currentUser.getRoleCode();

        if (ADMIN_PERMISSION.equalsIgnoreCase(requiredPermission)
                && !RoleEnum.ADMIN.getRoleCode().equalsIgnoreCase(userRoleCode)) {
            throw new NoPermissionException("无权限访问");
        }

        if (USER_PERMISSION.equalsIgnoreCase(requiredPermission)
                && RoleEnum.ADMIN.getRoleCode().equalsIgnoreCase(userRoleCode)) {
            throw new NoPermissionException("管理员请使用前台账号访问");
        }

        return joinPoint.proceed();
    }
}
