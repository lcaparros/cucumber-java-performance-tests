package com.lcaparros.test.glue;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class StepDefinitions {
    private Map<Integer, Result> results = new ConcurrentHashMap<Integer, Result>();
    private Long tic;
    private Long toc;

    @When("^(\\d+) requests are sent to (.*) simultaneously$")
    public void n_requests_sent_simultaneously(Integer numberOfRequests, String endpoint) {
        Map<Integer, Long> map = new ConcurrentHashMap<>();

        tic = System.currentTimeMillis();

        log.info("Sending requests");
        CompletableFuture.allOf(
                IntStream.range(0, numberOfRequests)
                        .mapToObj(i -> {
                            final int j = i;
                            map.put(j, System.currentTimeMillis());
                            return Unirest.get(endpoint).asStringAsync(httpResponse -> {
                                results.put(
                                        j,
                                        Result.builder()
                                                .executionTime(System.currentTimeMillis() - map.get(j))
                                                .httpResponse(httpResponse)
                                                .build()
                                );
                            });
                        })
                        .toArray(CompletableFuture[]::new)).join();

        toc = System.currentTimeMillis();
        log.info(String.format("Requests served in %d miliseconds", toc - tic));
    }

    @Then("^(\\d+) success responses have been received$")
    public void n_success_responses_received(int numberOfRequests) {
        AtomicInteger numberOfOkResponses = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);
        results.forEach((integer, result) -> {
            if (HttpStatus.SC_OK == result.getHttpResponse().getStatus()) {
                numberOfOkResponses.incrementAndGet();
            }
            totalTime.addAndGet(result.getExecutionTime());
            assertEquals(HttpStatus.SC_OK, result.getHttpResponse().getStatus());
        });
        assertEquals(
                String.format("%d 200 OK responses received for %d requests", numberOfOkResponses.get(), numberOfRequests),
                numberOfRequests,
                numberOfOkResponses.get()
        );
        log.info(String.format("The average loading time of the individual requests is %d miliseconds", totalTime.get() / results.size()));
    }

    @Then("^Response time for each request is less than (\\d+) miliseconds$")
    public void response_time_less_than(int responseTime) {
        AtomicInteger totalResponsesInTime = new AtomicInteger(0);
        this.results.forEach((integer, result) -> {
            boolean res = result.getExecutionTime() < responseTime;
            if (res) {
                totalResponsesInTime.getAndIncrement();
            }
            assertTrue(res);
        });
        assertEquals(
                String.format("All responses have been received before of %d miliseconds", responseTime),
                results.size(),
                totalResponsesInTime.get()
        );
        log.info("All responses have been received before of %d miliseconds");
    }

    @Then("^All the request have been served before of (\\d+) miliseconds$")
    public void total_response_time_less_than(int totalResponseTime) {
        assertTrue(String.format("All requests have been served before %d miliseconds", totalResponseTime),
                (toc - tic) < totalResponseTime);
    }
}
