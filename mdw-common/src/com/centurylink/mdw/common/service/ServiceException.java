package com.centurylink.mdw.common.service;

import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;

import io.limberest.validate.Result;
import io.limberest.validate.Result.Consolidator;

public class ServiceException extends MdwException {

    // taken from HTTP response codes
    public static final int BAD_REQUEST = 400;
    public static final int NOT_AUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int NOT_ALLOWED = 405;
    public static final int CONFLICT = 409;
    public static final int INTERNAL_ERROR = 500;
    public static final int NOT_IMPLEMENTED = 501;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(int code, String message) {
        super(code, message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceException(int code, Throwable cause) {
        super(code, cause.getMessage(), cause);
    }

    public ServiceException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public StatusResponse getStatusResponse() {
        return new StatusResponse(getCode(), getMessage());
    }

    public ServiceException(Status status) {
        super(status.getCode(), status.getMessage());
    }

    public ServiceException(Result validationResult) {
        super(validationResult.getWorstCode(), validationResult.getStatus().getMessage());
    }

    public ServiceException(Result validationResult, Consolidator consolidator) {
        super(validationResult.getWorstCode(),
                validationResult.getStatus(consolidator).getMessage());
    }
}
