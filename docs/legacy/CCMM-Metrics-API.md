# CCMM-Metrics-API

This page describes the Spring Boot calculator service on git tag `legacy-web-v1`. That directory is not on `master`. The desktop IDE calculates metrics in-process and does not call this service.

To run it beside this checkout:

```sh
git worktree add ../ccmm-legacy legacy-web-v1
```

Then work in `../ccmm-legacy/CCMM-Metrics-API`. The React client on the same tag calls it at `http://localhost:8080`. See [CCMM](CCMM.md) for the pages that use each endpoint.

Spring Boot 3.1.2. `pom.xml` sets `java.version` to 17. The Maven compiler plugin sets source and target to 19, so the build needs a JDK 19 or newer. `src/main/resources/application.properties` is empty, so the server uses the Spring Boot default port `8080`. CORS is open on each controller (`@CrossOrigin`).

## Run

From `CCMM-Metrics-API` in the legacy checkout:

```sh
./mvnw spring-boot:run
```

On Windows:

```bat
mvnw.cmd spring-boot:run
```

`./mvnw test` runs the generated Spring context test (`CCMMApplicationTests`). There are no calculator unit tests.

A sample input on that tag is `sampleCode.txt` at the checkout root.

## Endpoints

All four are `POST`. None require authentication.

### `POST /calculate-halstead`

Body: raw source text, `Content-Type: text/plain`.

Response JSON:

| Field | Meaning |
|---|---|
| `vocabulary` | Unique operators plus unique operands (`n1 + n2`) |
| `length` | Total operator occurrences plus total operand occurrences (`N1 + N2`) |
| `volume` | `length * log2(vocabulary)` |
| `difficulty` | `(n1 / 2) * (N2 / n2)` |
| `effort` | `difficulty * volume` |

`//` comments and a single-line `/* */` are stripped per line. Operators are matched from a fixed regex list. A unique operator is recorded as that regex, so overlapping patterns such as `+` and `++` can both count. Operands are identifiers. Numeric literals are ignored. Java keywords are excluded.

Used by `/halsteadmetriccalc`.

### `POST /calculate-line-count`

Body:

```json
{ "code": "source text" }
```

Response JSON:

| Field | Meaning |
|---|---|
| `codeLines` | Lines that are not blank and not a full-line comment |
| `commentLines` | `//` lines and `/* */` lines, including a block-comment flag across lines |
| `blankLines` | Empty lines after trim |
| `functionCount` | Heuristic method count |

A line counts as a method when it contains a modifier or primitive-type word and a `(`. A signature split across lines is remembered until `{`. Brace-only lines count as code. The word lists are Java-oriented; the scan is not a parser.

Used by `/loccal`.

### `POST /calculate-maintainability-index`

Body: raw source text, `Content-Type: text/plain`.

Response: a JSON array of three numbers:

| Index | Meaning |
|---|---|
| `0` | Maintainability index, rounded, and clamped at 0 |
| `1` | Cyclomatic complexity |
| `2` | Count of non-empty lines |

Cyclomatic complexity starts at 1 and adds 1 for each match of `if`, `else`, `for`, `while`, `do`, `switch`, `case`, `catch`, `throw`, `return`, `&&`, and `||`.

Halstead volume on this endpoint uses its own operator list (including `?` and `:`) and treats every word-character token as an operand, including keywords. It is not the same calculation as `/calculate-halstead`. The line count here is non-empty lines, which is not the same as `codeLines` from `/calculate-line-count`.

The index is:

```
(171 - 5.2 * ln(volume) - 0.23 * cyclomatic - 16.2 * ln(loc)) * 100 / 171
```

`ln` is the natural log. The client paints 0–9 red, 10–19 yellow, and 20–100 green.

Used by `/maintainabilitycal` and by the editor complexity sidebar.

### `POST /calculate-loop-depth`

Body:

```json
{ "code": "source text" }
```

Response: one JSON integer, the maximum nesting depth seen.

The scan strips `public`, `private`, `protected`, `static`, and `final`, then strips comments. Depth increases when the remaining text starts with `for`, `while`, or `do`, and decreases on every `}`. A word such as `double` matches `do`. Braces that belong to classes, methods, or `if` statements also change the depth.

Used by `/complexitycal` and by the editor complexity sidebar.

## Classes that are not endpoints

| Class | Status |
|---|---|
| `CCMMApplication` | Spring Boot entry point |
| `CodeRequest` | JSON body `{ "code": "..." }` for line count and loop depth |
| `LineCountResponse` | JSON body for line count |
| `Report` | Empty class. No mapping. |
| `RecursionReport` | Data holder. No mapping. |
| `DatabaseConnection` | Entirely commented out. Persistence is not active. Do not revive that file; it contains a connection string in a comment. |

## Example

With the API running:

```sh
curl -s -X POST http://localhost:8080/calculate-line-count -H "Content-Type: application/json" -d "{\"code\":\"public class A {\\n void m() {\\n }\\n}\\n\"}"
```
