# Cold start

Duration is the time from JavaFX startup until the stage is showing and the file tree is filled. It does not include Maven startup.

- OS: Windows 11 amd64
- Command: `CCMM_COLD_START=true mvn -B -ntp -pl ccmm-app -am test -Dtest=ColdStartHarness`
- Files: 50
- Duration: 994.979 ms
- Budget: 2000 ms

go
