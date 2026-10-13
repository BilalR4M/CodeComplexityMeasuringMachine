# Keystroke latency

p95 is the time `CodeArea.insertText` blocked the JavaFX thread for one character, after a 10000-line buffer was on screen. It does not include waiting for the next vsync.

- OS: Windows 11 amd64
- Command: `CCMM_KEYSTROKE=true mvn -B -ntp -pl ccmm-app -am test -Dtest=KeystrokeLatencyHarness,KeystrokeReportTest`
- Buffer: 10000 lines
- Samples: 200 single-character inserts
- p95: 1.547 ms
- memory: 490.6 MB committed

go
