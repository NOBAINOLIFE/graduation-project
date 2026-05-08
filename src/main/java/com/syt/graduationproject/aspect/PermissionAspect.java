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

@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(com.syt.graduationproject.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        UserDto currentUser = UserHolderUtil.getUser();
        if (currentUser == null) {
            throw new NoPermissionException("未登录");
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequirePermission requirePermission = signature.getMethod().getAnnotation(RequirePermission.class);
        String requiredPermission = requirePermission.value();

        if (ADMIN_PERMISSION.equalsIgnoreCase(requiredPermission)
                && !RoleEnum.ADMIN.getRoleCode().equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new NoPermissionException("无权限访问");
        }

        return joinPoint.proceed();
    }
}
