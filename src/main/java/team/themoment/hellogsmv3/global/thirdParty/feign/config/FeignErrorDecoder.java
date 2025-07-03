package team.themoment.hellogsmv3.global.thirdParty.feign.config;

import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.data.HeaderConstant;

public class FeignErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        if (status >= 400) {
            switch (status) {
                case 400 -> throw new ExpectedException(response.reason(), HttpStatus.BAD_REQUEST);
                case 401 -> throw new ExpectedException(response.reason(), HttpStatus.UNAUTHORIZED);
                default -> throw new ExpectedException(response.reason(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return FeignException.errorStatus(methodKey, response);
    }
}
