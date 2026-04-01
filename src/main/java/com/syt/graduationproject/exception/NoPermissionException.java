package com.syt.graduationproject.exception;

import static com.syt.graduationproject.constant.ResponseConstant.NO_PERMISSION;

public class NoPermissionException extends CustomException {

    public NoPermissionException() {
        super(NO_PERMISSION);
    }

    public NoPermissionException(String message) {
        super(message);
    }
}
