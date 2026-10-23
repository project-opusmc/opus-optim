# Independent frozen-patch review

Read-only review completed with no required correctness fixes. O2 code/test/
probe patch SHA-256:
`9ef6e79c24f0a938de8e7f26a6dfa3997af546a08dfaa32e75388b541fce29ff`.
Launcher patch SHA-256 after Rust formatting:
`f51a850a7c5a2041742cb9806f340d76744c7e741b2d0a42fa309acd639b52d5`.
Both patches matched their staged diffs and passed whitespace checks.

The reviewer checked paged glyph duplicate/missing/surrogate behavior, unsigned
offsets, dense-table bounds, the actual pinned bfg/bfd constructor/growth
contract, four-anchor bytecode rewriting, explicit Launcher selection, tests,
and measurement boundaries. Native workload execution is author evidence;
the reviewer did not rerun those workloads.

The review considers this suitable to retain as EXPERIMENTAL/OFF. It explicitly
declines a whole-process RAM/RSS, live-heap, CPU-speed, FPS, release or physical
GUI claim. In particular, both candidate final-unload RSS observations are
higher than the controls, and direct-capacity savings diminish after rebuilds.

Optional follow-up: enforce entry-level core-JAR isolation inside the glyph
benchmark script. The author compared all ZIP entries before this experiment;
only GlyphIndex.class differed. The script itself records whole-JAR hashes and
validates workload/checksum identity but does not enforce that entry-level
constraint. This limitation is recorded in verification.json.
