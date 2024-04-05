package com.hit.userms.response;

import org.springframework.http.HttpStatus;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ApiResponse {
    private HttpStatus status;

    private String msg;
}
