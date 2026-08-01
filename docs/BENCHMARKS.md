# Benchmarks

Lumina's first performance baseline is `mvn -q clean verify` plus the integration suite. Before
publishing 1.0, benchmark snapshot construction, tree diffing, and WebSocket patch delivery with a
representative application. Record JVM version, machine details, session count, and payload sizes so
future results remain comparable.
