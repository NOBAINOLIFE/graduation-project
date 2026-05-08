package com.syt.graduationproject.exception;

public class NotFoundException extends CustomException {

    public NotFoundException() {
        super("资源不存在");
    }

    public NotFoundException(String message) {
        super(message);
    }
}
