# Metrics refresh

Refresh is the time from the last edit until that buffer's analysis result is applied on the JavaFX thread. The quiet time is 300 ms. Parsing runs after that, off the JavaFX thread.

- OS: Windows 11 amd64
- Command: `CCMM_METRICS_REFRESH=true mvn -B -ntp -pl ccmm-app -am test -Dtest=MetricsRefreshHarness`
- Quiet time: 300 ms
- Refresh: 439.072 ms
- Budget: 300 ms

go
