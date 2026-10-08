package com.charity.reunion.common;

/**
 * 业务异常:消息会原样返回给前端
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
