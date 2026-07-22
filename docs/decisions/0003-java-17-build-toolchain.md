# ADR 0003: Java 17-compatible build toolchain

- Status: Accepted
- Date: 2026-07-16

Loom 1.15.3/1.17.12 and Babric extension 1.15.3/1.16.1 were rejected after Gradle metadata/build failures proved they require JVM 21. Maven module metadata showed 1.10.5 is the latest examined stable common version declaring JVM 17. Use Gradle 8.12.1, Loom 1.10.5, and Babric extension 1.10.5. Revisit only with client/server compatibility evidence; never switch the program build JVM to 21 silently.
