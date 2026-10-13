# OT convergence

Two clients submit insert and delete operations against the same base revision. The server orders them and transforms the later operation against the earlier one. Each client rebuilds the document by replaying that log. There is no relay process and no JavaFX code.

- Command: `mvn -B -ntp -pl ccmm-collab-protocol test`
- Cases: concurrent inserts, same-index inserts, disjoint deletes, overlapping deletes, an insert concurrent with a delete, and 30 random pairs (seed 1)
- Result: for every server order, both clients match the server document

go
