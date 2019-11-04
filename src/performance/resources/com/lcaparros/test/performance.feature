Feature: Performance tests
  Skeleton for performance tests defined with Gherking

  Scenario Outline: Load test
    When <numberOfRequests> requests are sent to <endpoint> simultaneously
    Then <numberOfRequests> success responses have been received
    And Response time for each request is less than <responseTime> miliseconds
    And All the request have been served before of <totalResponseTime> miliseconds

    Examples:
    | numberOfRequests |              endpoint            |   responseTime   |  totalResponseTime  |
    |      10000       |    http://localhost:8080/ping    |       15000      |         15000       |