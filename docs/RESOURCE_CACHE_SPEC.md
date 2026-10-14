# O2 immutable resource text cache

Continue the owner's wider core-runtime/resource engineering request. The exact
local raw M5 jy.class SHA256 is
4dcb02692da8c3ae7d6137281b1a25e5f249514e2bc7d1efeb7b9400f857dd22.
Its protected final String a/b fields are domain/path. Every original toString()
constructs a new builder/String; content depends only on these immutable fields.

Implement runtime.resource_string_cache as an independent EXPERIMENTAL startup
candidate, OFF without an explicit flag and required raw-target/ASM capabilities.
The factory retains per-instance NOT_SEEN/APPLIED/UNSUPPORTED_INPUT state. Unknown
or modified bytes are returned by identity. No install or promotion occurs here.

Add private volatile String o2$resourceText. On a cache hit return it. On a miss
execute the original method body, retain its returned String, and return it.
Constructor normalization, getters, equals/hashCode, subclass override dispatch
and original cold text content remain unchanged. The immutable-fields contract
excludes reflective/unsafe mutation. String object freshness is deliberately
changed; toString() has no fresh-object contract. Races may duplicate the first
allocation but publish only equivalent immutable content. No global intern table,
cross-resource sharing or lifecycle hooks. Objects/strings are collected normally.

Cost: one reference field per resource instance, and one cached String retained
only on queried instances. This can increase live heap; allocation/throughput
benefit must be reported with that cost. This does not claim zip/decode/texture
upload acceleration, pack cache invalidation, memory leak repair or FPS gains.

Use owned bytecode fixture tests for original cold result, warm identity reuse,
constructor effects and instance isolation; OFF/missing flag/capability and public
pin rejection. On actual Java8 load original/patched complete raw ResourceLocation
classes with the real existing commons-lang3 3.3.2 dependency, not a game session.
Exercise real constructors, malformed/null paths, normalization, equals/hashCode,
subclasses and multiple readers. Run independent A/A/A/B string-query forks with
consumed content checksums and controlled cold/warm workloads. Preserve variation
and unfavorable results. Full suite, class/JAR/license guards; keep exact
opus-optim, Opus LICENSE, Claude's Loader and published gitlink.

The previous independent reviewer hit a usage limit before returning findings.
Its review has no verdict. Review remains outstanding for both increments; author
checks must not be relabeled independent review. Keep all candidates OFF/EXPERIMENTAL
and do not publish, merge or claim suite/game completion while that gate is open.
