# Project scan

Duration is `Analyzer.scan` on 300 small compilation units. Memory is the process committed size after that scan, the same measure as `keystroke-latency.md`.

- OS: Windows 11 amd64
- Command: `mvn -B -ntp -pl ccmm-core test-compile` then `java -cp ccmm-core/target/test-classes;ccmm-core/target/classes;<javaparser> dev.bilal.ccmm.core.ProjectScanBenchmark`
- Files: 300
- Duration: 498.876 ms
- memory: 360.9 MB committed

go
