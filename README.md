# CCMM IDE

Desktop IDE written in Java with live code-complexity metrics (cyclomatic complexity, Halstead, maintainability index, LOC), quality gates for CI and PRs, and real-time collaboration. Windows, Linux, macOS.

The previous web version lives at tag `legacy-web-v1`.

## Install
Download the installer for your OS from [Releases](https://github.com/BilalR4M/CodeComplexityMeasuringMachine/releases). Verify with `SHA256SUMS-*.txt`.

## Use as a library
Maven (GitHub Packages):
```xml
<repository><id>github</id><url>https://maven.pkg.github.com/BilalR4M/CodeComplexityMeasuringMachine</url></repository>
<dependency><groupId>dev.bilal.ccmm</groupId><artifactId>ccmm-core</artifactId><version>VERSION</version></dependency>
```
GitHub Packages requires a token with `read:packages` in `~/.m2/settings.xml`.

## Collaboration server
```
docker run -p 8080:8080 ghcr.io/bilalr4m/ccmm-collab-server:latest
```

## Develop
See [AGENTS.md](AGENTS.md) and [docs/PLAN.md](docs/PLAN.md).
```
mvn -B -ntp verify
mvn -pl ccmm-app -am javafx:run
```
