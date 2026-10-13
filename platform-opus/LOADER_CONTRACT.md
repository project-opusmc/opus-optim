# Draft Loader snapshot

Archived 2026-10-13 from the working tree at `/Users/zvwgvx/Project/Opus/runtime`,
base commit `34714870c4ccdc6e00025a0e3176e92027f1d3cc`. ClassTransformer and
TransformerChain are published at that base; ClassTransformerProvider,
TransformerRegistration and TransformerPlan are uncommitted draft additions.
`loader-contract-sha256.json` pins the exact five source files. The fixture uses
the common Opus license; no additional third-party implementation is copied.

This is a separate compile/test source set, excluded from the main platform
artifact. No snapshot type may be bundled, relocated or staged as an O2-owned
Loader implementation. A future published Loader API dependency must replace
this source set before declaring managed integration. Tests characterize the
draft's empty-provider rejection; they do not silently patch the snapshot.
