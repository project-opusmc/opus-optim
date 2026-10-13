package org.polydevs.opusmc.optim.bench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.polydevs.opusmc.optim.api.Metrics;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkComparatorTest {
    @Test void reportsAnIndependentRunSignalWithNoiseAndUncertainty() {
        ComparisonResult result = compare(arm("aa1", Fixtures.CONTROL, 10), arm("aa2", Fixtures.CONTROL, 10),
                variedControl(), arm("b", Fixtures.TREATMENT, 8), evidence(EvidenceReceipt.Status.PASS));
        assertEquals(ComparisonResult.Verdict.PERFORMANCE_SIGNAL, result.verdict());
        ComparisonEstimate mean = result.estimates().get("meanFrameNanos");
        assertEquals(20, mean.effectPercent(), 1e-9);
        assertEquals(11.2184, mean.lowerPercent(), .01);
        assertEquals(0, mean.noiseFloorPercent());
        assertEquals(5, result.runCounts().get("control"));
        assertTrue(result.reasons().isEmpty());
    }
    @Test void aaLevelAndZeroEffectsStayInconclusive() {
        for (double treatment : new double[]{10, 9.9}) {
            ComparisonResult result = compare(arm("aa1", Fixtures.CONTROL, 10), arm("aa2", Fixtures.CONTROL, 10.2),
                    arm("a", Fixtures.CONTROL, 10), arm("b", Fixtures.TREATMENT, treatment), evidence(EvidenceReceipt.Status.PASS));
            assertEquals(ComparisonResult.Verdict.INCONCLUSIVE, result.verdict());
            assertTrue(result.reasons().contains("MEAN_NOT_BEYOND_NOISE"));
            assertEquals(2, result.estimates().get("meanFrameNanos").noiseFloorPercent(), 1e-9);
        }
    }
    @Test void reusedSessionsAndRunIdsCannotBecomeIndependentReplicates() {
        List<BenchmarkRun> treatment = arm("b", Fixtures.TREATMENT, 8);
        BenchmarkRun run = treatment.get(0);
        treatment.set(0, copy(run, new RunIdentity("b0", "session-aa10", Fixtures.TREATMENT, RunIdentity.Origin.RECORDED),
                run.scope(), 2, 2, 1000, 0, BenchmarkRun.Termination.COMPLETE));
        assertInvalid(treatment, "REUSED_SESSION");
        treatment.set(0, copy(run, new RunIdentity("aa10", "unique", Fixtures.TREATMENT, RunIdentity.Origin.RECORDED),
                run.scope(), 2, 2, 1000, 0, BenchmarkRun.Termination.COMPLETE));
        assertInvalid(treatment, "REUSED_RUN_ID");
    }
    @Test void scopeAndVariantDriftInvalidateTheComparison() {
        List<BenchmarkRun> treatment = arm("b", Fixtures.TREATMENT, 8);
        BenchmarkRun run = treatment.get(0);
        Map<String, String> fields = Fixtures.scopeFields(); fields.put("gpu", "other-gpu");
        treatment.set(0, copy(run, run.identity(), new BenchmarkScope(fields), 2, 2, 1000, 0, BenchmarkRun.Termination.COMPLETE));
        assertInvalid(treatment, "SCOPE_MISMATCH");
        treatment.set(0, copy(run, new RunIdentity("b0", "session-b0", Fixtures.CONTROL, RunIdentity.Origin.RECORDED),
                run.scope(), 2, 2, 1000, 0, BenchmarkRun.Termination.COMPLETE));
        assertInvalid(treatment, "VARIANT_MISMATCH");
    }
    @Test void droppedIncompleteInterruptedAndShortRunsFailClosed() {
        for (String failure : Arrays.asList("DROPPED_SAMPLES", "WARMUP_INCOMPLETE", "INTERRUPTED_RUN", "INSUFFICIENT_SAMPLES")) {
            List<BenchmarkRun> treatment = arm("b", Fixtures.TREATMENT, 8);
            BenchmarkRun run = treatment.get(0);
            if (failure.equals("INSUFFICIENT_SAMPLES")) { treatment.set(0, Fixtures.run("b0", Fixtures.TREATMENT, Fixtures.frames(999, 8_000_000))); }
            else { treatment.set(0, copy(run, run.identity(), run.scope(), 2,
                    failure.equals("WARMUP_INCOMPLETE") ? 1 : 2,
                    failure.equals("DROPPED_SAMPLES") ? 1001 : 1000, failure.equals("DROPPED_SAMPLES") ? 1 : 0,
                    failure.equals("INTERRUPTED_RUN") ? BenchmarkRun.Termination.INTERRUPTED : BenchmarkRun.Termination.COMPLETE)); }
            assertInvalid(treatment, failure);
        }
    }
    @Test void fiveRunsPerArmAreRequiredAndEmptyArmsDoNotCrash() {
        ComparisonResult shortArm = compare(arm("aa1", Fixtures.CONTROL, 10), arm("aa2", Fixtures.CONTROL, 10),
                arm("a", Fixtures.CONTROL, 10), arm("b", Fixtures.TREATMENT, 8).subList(0, 4), evidence(EvidenceReceipt.Status.PASS));
        assertEquals(ComparisonResult.Verdict.INVALID, shortArm.verdict());
        assertTrue(shortArm.reasons().contains("INSUFFICIENT_RUNS"));
        assertEquals(ComparisonResult.Verdict.INVALID, compare(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), evidence(EvidenceReceipt.Status.PASS)).verdict());
    }
    @Test void declaredEvidenceIsBoundToBothVariantsAndScope() {
        ExperimentEvidence evidence = new ExperimentEvidence("candidate", Fixtures.HASH_B, Fixtures.CONTROL.fingerprint(),
                Fixtures.TREATMENT.fingerprint(), receipt(EvidenceReceipt.Status.PASS), receipt(EvidenceReceipt.Status.PASS), receipt(EvidenceReceipt.Status.PASS));
        ComparisonResult result = standard(evidence);
        assertEquals(ComparisonResult.Verdict.INVALID, result.verdict());
        assertTrue(result.reasons().contains("EVIDENCE_SCOPE_MISMATCH"));
        evidence = new ExperimentEvidence("candidate", Fixtures.scope().fingerprint(), Fixtures.TREATMENT.fingerprint(),
                Fixtures.CONTROL.fingerprint(), receipt(EvidenceReceipt.Status.PASS), receipt(EvidenceReceipt.Status.PASS), receipt(EvidenceReceipt.Status.PASS));
        assertTrue(standard(evidence).reasons().contains("EVIDENCE_VARIANT_MISMATCH"));
    }
    @Test void missingOrFailedEvidenceCannotProduceASignal() {
        assertEquals(ComparisonResult.Verdict.INCONCLUSIVE, standard(evidence(EvidenceReceipt.Status.MISSING)).verdict());
        assertEquals(ComparisonResult.Verdict.REJECTED, standard(evidence(EvidenceReceipt.Status.FAIL)).verdict());
        ExperimentEvidence noMechanism = new ExperimentEvidence("candidate", Fixtures.scope().fingerprint(),
                Fixtures.CONTROL.fingerprint(), Fixtures.TREATMENT.fingerprint(), receipt(EvidenceReceipt.Status.PASS),
                receipt(EvidenceReceipt.Status.MISSING), receipt(EvidenceReceipt.Status.PASS));
        assertTrue(standard(noMechanism).reasons().contains("MECHANISM_MISSING"));
    }
    @Test void syntheticFixturesNeverBecomeRecordedPerformanceEvidence() {
        List<BenchmarkRun> treatment = arm("b", Fixtures.TREATMENT, 8);
        BenchmarkRun run = treatment.get(0);
        treatment.set(0, copy(run, new RunIdentity("b0", "session-b0", Fixtures.TREATMENT, RunIdentity.Origin.SYNTHETIC),
                run.scope(), 2, 2, 1000, 0, BenchmarkRun.Termination.COMPLETE));
        ComparisonResult result = compare(arm("aa1", Fixtures.CONTROL, 10), arm("aa2", Fixtures.CONTROL, 10),
                arm("a", Fixtures.CONTROL, 10), treatment, evidence(EvidenceReceipt.Status.PASS));
        assertEquals(ComparisonResult.Verdict.INCONCLUSIVE, result.verdict());
        assertTrue(result.reasons().contains("SYNTHETIC_DATA"));
    }
    @Test void aMeanImprovementCannotHideTailRegressions() {
        List<BenchmarkRun> treatment = new ArrayList<>();
        long[] frames = Fixtures.frames(1000, 5_000_000);
        Arrays.fill(frames, 980, 1000, 60_000_000);
        for (int i = 0; i < 5; i++) { treatment.add(Fixtures.run("b" + i, Fixtures.TREATMENT, frames)); }
        ComparisonResult result = compare(arm("aa1", Fixtures.CONTROL, 10), arm("aa2", Fixtures.CONTROL, 10),
                arm("a", Fixtures.CONTROL, 10), treatment, evidence(EvidenceReceipt.Status.PASS));
        assertEquals(ComparisonResult.Verdict.REJECTED, result.verdict());
        assertTrue(result.reasons().contains("P99_REGRESSION"));
    }
    @Test void runLengthDoesNotTurnCorrelatedFramesIntoExtraReplicates() {
        List<BenchmarkRun> treatment = new ArrayList<>();
        for (int i = 0; i < 4; i++) { treatment.add(Fixtures.run("b" + i, Fixtures.TREATMENT, Fixtures.frames(10_000, 9_000_000))); }
        treatment.add(Fixtures.run("b4", Fixtures.TREATMENT, Fixtures.frames(1000, 20_000_000)));
        ComparisonResult result = compare(arm("aa1", Fixtures.CONTROL, 10), arm("aa2", Fixtures.CONTROL, 10),
                arm("a", Fixtures.CONTROL, 10), treatment, evidence(EvidenceReceipt.Status.PASS));
        assertEquals(-12, result.estimates().get("meanFrameNanos").effectPercent(), 1e-9);
        assertEquals(ComparisonResult.Verdict.INCONCLUSIVE, result.verdict());
    }
    @Test void labelsAloneDoNotDeclareAnActualTreatmentDelta() {
        BenchmarkVariant relabel = new BenchmarkVariant("relabel", Fixtures.REV, Fixtures.HASH_A, Fixtures.HASH_A);
        ComparisonResult result = compare(arm("aa1", Fixtures.CONTROL, 10), arm("aa2", Fixtures.CONTROL, 10),
                arm("a", Fixtures.CONTROL, 10), arm("b", relabel, 8), evidence(EvidenceReceipt.Status.PASS));
        assertTrue(result.reasons().contains("NO_VARIANT_DELTA"));
    }
    @Test void boundsWholeExperimentMemoryBeforeComputingStatistics() {
        BenchmarkRun run = Fixtures.run("large", Fixtures.CONTROL, Fixtures.frames(1_000_000, 10));
        List<BenchmarkRun> six = Collections.nCopies(6, run);
        ComparisonResult result = compare(six, six, six, six, evidence(EvidenceReceipt.Status.PASS));
        assertEquals(ComparisonResult.Verdict.INVALID, result.verdict());
        assertTrue(result.reasons().contains("COMPARISON_SAMPLE_LIMIT")); assertTrue(result.estimates().isEmpty());
        result = compare(Collections.nCopies(1001, run), six, six, six, evidence(EvidenceReceipt.Status.PASS));
        assertTrue(result.reasons().contains("RUN_LIMIT"));
    }
    private ComparisonResult standard(ExperimentEvidence evidence) {
        return compare(arm("aa1", Fixtures.CONTROL, 10), arm("aa2", Fixtures.CONTROL, 10),
                arm("a", Fixtures.CONTROL, 10), arm("b", Fixtures.TREATMENT, 8), evidence);
    }
    private void assertInvalid(List<BenchmarkRun> treatment, String reason) {
        ComparisonResult result = compare(arm("aa1", Fixtures.CONTROL, 10), arm("aa2", Fixtures.CONTROL, 10),
                arm("a", Fixtures.CONTROL, 10), treatment, evidence(EvidenceReceipt.Status.PASS));
        assertEquals(ComparisonResult.Verdict.INVALID, result.verdict()); assertTrue(result.reasons().contains(reason));
    }
    static ExperimentEvidence evidence(EvidenceReceipt.Status parity) {
        return new ExperimentEvidence("candidate", Fixtures.scope().fingerprint(), Fixtures.CONTROL.fingerprint(),
                Fixtures.TREATMENT.fingerprint(), receipt(parity), receipt(EvidenceReceipt.Status.PASS), receipt(EvidenceReceipt.Status.PASS));
    }
    private static EvidenceReceipt receipt(EvidenceReceipt.Status status) {
        return new EvidenceReceipt(status, status == EvidenceReceipt.Status.MISSING ? null : Fixtures.HASH_A);
    }
    static List<BenchmarkRun> arm(String prefix, BenchmarkVariant variant, double milliseconds) {
        List<BenchmarkRun> runs = new ArrayList<>();
        for (int i = 0; i < 5; i++) { runs.add(Fixtures.run(prefix + i, variant, Fixtures.frames(1000, (long) (milliseconds * 1_000_000)))); }
        return runs;
    }
    private List<BenchmarkRun> variedControl() {
        List<BenchmarkRun> runs = new ArrayList<>();
        long[] values = {9_000_000, 10_000_000, 11_000_000, 10_000_000, 10_000_000};
        for (int i = 0; i < values.length; i++) { runs.add(Fixtures.run("a" + i, Fixtures.CONTROL, Fixtures.frames(1000, values[i]))); }
        return runs;
    }
    private ComparisonResult compare(List<BenchmarkRun> aa1, List<BenchmarkRun> aa2, List<BenchmarkRun> a,
            List<BenchmarkRun> b, ExperimentEvidence evidence) { return BenchmarkComparator.compare(aa1, aa2, a, b, evidence); }
    private BenchmarkRun copy(BenchmarkRun run, RunIdentity identity, BenchmarkScope scope, int warmupRequested,
            int warmupObserved, long observed, long dropped, BenchmarkRun.Termination termination) {
        return new BenchmarkRun(identity, scope, warmupRequested, warmupObserved, observed, dropped, run.frameNanos(),
                termination, new Metrics(Collections.emptyMap()));
    }
}
