package com.syt.graduationproject.exception;


import static com.syt.graduationproject.constant.ResponseConstant.PARAM_INVALID;

public class ErrorParamException extends CustomException{

    public ErrorParamException() {
        super(PARAM_INVALID);
    }

    public ErrorParamException(String msg) {
        super(msg);
    }
}
