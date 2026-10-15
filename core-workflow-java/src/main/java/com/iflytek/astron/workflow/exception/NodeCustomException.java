package com.iflytek.astron.workflow.exception;

/**
 * Custom exception class for workflow engine errors.
 * 
 * This class wraps error codes and messages for consistent error handling
 * throughout the workflow engine.
 */
public class NodeCustomException extends RuntimeException {
    private int code;
    private String message;
    
    public NodeCustomException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public NodeCustomException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.message = errorCode.getMsg();
    }
    
    public NodeCustomException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}