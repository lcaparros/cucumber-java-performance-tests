package com.lcaparros.test.glue;

import kong.unirest.HttpResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Result {
    private long executionTime;
    private HttpResponse httpResponse;
}
