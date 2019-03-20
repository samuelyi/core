package com.dotcms.rest.exception;

import com.dotcms.repackage.javax.ws.rs.WebApplicationException;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

/**
 * Parent class for HTTP Status Code oriented exceptions.
 * Subclasses will represent specific HTTP Status codes, such as Bad Request and Forbidden.
 *
 *
 * Note 1:
 * The message provided to constructors for subclasses is provided to the client via response headers.
 * Therefore the message should be specific to 'throw' case, and should take care to not expose potentially sensitive details. It should also not
 * be overly verbose.
 * In the ideal case, the message should be a simple error code, or message key ("rest.host.not_found") that the consumer can look up on their own
 *
 * Note 2:
 * This probably shouldn't extend WebApplicationException, as it brings with it a number of assumptions we
 * don't necessarily agree with. However, existing exceptions in the .rest.* package seem likely to be relying on the behaviour
 * included with WebApplicationException, so not using it here needs a bit of research. There's also a tug at the memory centers that makes me think
 * that the JAX-RS spec won't play nice without it.
 *
 * @todo ggranum: Revisit this after discussion with interested parties.
 */
public abstract class HttpStatusCodeException extends WebApplicationException {

    private static final long serialVersionUID = 1L;

    HttpStatusCodeException(Response.Status status, String key, String message, String... messageArgs) {
        this(null, status, key, message, messageArgs);
    }

    HttpStatusCodeException(Throwable cause, Response.Status status, String key, String message, String... messageArgs) {
        super(cause, toResponse(status, key, getFormattedMessage( message, messageArgs )));
        if(Response.Status.NOT_FOUND == status ){
        	Logger.getLogger(this.getClass()).debug(this.getResponse().getEntity().toString(),this);
        }
        else{
        	Logger.getLogger(this.getClass()).warn(this.getResponse().getEntity().toString(), this);
        }
    }

    HttpStatusCodeException(final Throwable cause, final Response.Status status, String key, final Object entity, final String message) {
        super(cause, toResponse(status, key, message));
        if(Response.Status.NOT_FOUND == status ){
            Logger.getLogger(this.getClass()).debug(this.getResponse().getEntity().toString(),this);
        }
        else{
            Logger.getLogger(this.getClass()).warn(this.getResponse().getEntity().toString(), this);
        }
    }

    private static String getFormattedMessage(String message, String... messageArgs){

        String messageFormatted;

        if (message == null){
            messageFormatted =  null;
        }else{
            if (messageArgs == null){
                messageFormatted = message;
            }else{
                messageFormatted = String.format(message, (Object[])messageArgs);
            }
        }

        return messageFormatted;
    }

    private static Response toResponse(Response.Status status,
                                       String key,
                                       String message) {
        return ErrorResponseHelper.INSTANCE.getErrorResponse(status, message, key);
    }

}
