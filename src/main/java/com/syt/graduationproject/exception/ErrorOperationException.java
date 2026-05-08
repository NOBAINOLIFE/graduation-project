package com.syt.graduationproject.exception;


import static com.syt.graduationproject.constant.ResponseConstant.ERROR_OPERATION;

/**
 * 操作异常
 */
public class ErrorOperationException extends CustomException {

    public ErrorOperationException() {
        super(ERROR_OPERATION);
    }

    public ErrorOperationException(String message) {
        super(message);
    }
}
