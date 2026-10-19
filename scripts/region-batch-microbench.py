#!/usr/bin/env python3
"""Independent Java8 A/A and A/B forks for O2 CPU draw queue/packing only."""
import argparse
import hashlib
import json
from pathlib import Path
import statistics
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
REGION = 'renderer-celeritas/src/main/java/org/polydevs/opusmc/optim/renderer/RegionRenderer.java'
DRIVER = ROOT / 'renderer-celeritas/src/smoke/java/org/polydevs/opusmc/optim/renderer/RegionBatchBench.java'
MAIN = 'org.polydevs.opusmc.optim.renderer.RegionBatchBench'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java8-home', type=Path, required=True)
    parser.add_argument('--baseline', default='faf630393fa04a78f3d969743fe22df790433c4a')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--pairs', type=int, default=5)
    parser.add_argument('--aa-pairs', type=int, default=3)
    parser.add_argument('--warmup-batches', type=int, default=30000)
    parser.add_argument('--batches', type=int, default=100000)
    args = parser.parse_args()
    if min(args.pairs, args.aa_pairs, args.warmup_batches, args.batches) < 1:
        parser.error('Pair and batch counts must be positive')
    if args.output.exists():
        parser.error('Output already exists; preserve the earlier receipt')
    baseline_revision = subprocess.check_output(['git', 'rev-parse', args.baseline], cwd=ROOT, text=True).strip()
    baseline = subprocess.check_output(['git', 'show', f'{baseline_revision}:{REGION}'], cwd=ROOT)
    treatment = (ROOT / REGION).read_bytes()
    results = []
    with tempfile.TemporaryDirectory(prefix='o2-region-bench-') as temporary:
        classes = {}
        for variant, source in [('baseline', baseline), ('treatment', treatment)]:
            folder = Path(temporary) / variant
            folder.mkdir()
            source_file = folder / 'RegionRenderer.java'
            source_file.write_bytes(source)
            subprocess.run([str(args.java8_home / 'bin/javac'), '-d', str(folder),
                            str(source_file), str(DRIVER)], check=True)
            classes[variant] = folder
        for layout in ['contiguous', 'fragmented', 'reverse']:
            for phase, pairs in [('AA', args.aa_pairs), ('AB', args.pairs)]:
                for pair in range(pairs):
                    roles = [('control', 'baseline'), ('repeat' if phase == 'AA' else 'treatment',
                                                     'baseline' if phase == 'AA' else 'treatment')]
                    if pair % 2:
                        roles.reverse()
                    for role, variant in roles:
                        command = [str(args.java8_home / 'bin/java'), '-Xms128m', '-Xmx128m',
                                   '-cp', str(classes[variant]), MAIN, layout,
                                   str(args.warmup_batches), str(args.batches)]
                        run = json.loads(subprocess.check_output(command, text=True))
                        expected = 1 if variant == 'treatment' and layout == 'contiguous' else 128
                        if run['commandsPerBatch'] != expected:
                            raise AssertionError(f'Incorrect command stream: {run}')
                        results.append(dict(run, phase=phase, pair=pair, role=role, variant=variant))
            print(f'{layout}: independent AA/AB forks complete', flush=True)
    summaries = {}
    for layout in ['contiguous', 'fragmented', 'reverse']:
        summaries[layout] = {}
        for phase in ['AA', 'AB']:
            ratios = []
            for pair in range(args.aa_pairs if phase == 'AA' else args.pairs):
                runs = [r for r in results if r['layout'] == layout and r['phase'] == phase and r['pair'] == pair]
                control = next(r for r in runs if r['role'] == 'control')
                other = next(r for r in runs if r['role'] != 'control')
                ratios.append(other['nanosPerBatch'] / control['nanosPerBatch'])
            summaries[layout][phase] = {'ratios': ratios, 'medianRatio': statistics.median(ratios),
                                         'minRatio': min(ratios), 'maxRatio': max(ratios)}
    record = {'schemaVersion': 1, 'scope': 'CPU queue/command packing; no GL driver/game/FPS',
              'baselineRevision': baseline_revision,
              'baselineSourceSha256': hashlib.sha256(baseline).hexdigest(),
              'treatmentSourceSha256': hashlib.sha256(treatment).hexdigest(),
              'driverSha256': hashlib.sha256(DRIVER.read_bytes()).hexdigest(),
              'javaVersion': subprocess.check_output([str(args.java8_home / 'bin/java'), '-version'],
                                                     stderr=subprocess.STDOUT, text=True).strip(),
              'summaries': summaries, 'runs': results}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(record, indent=2) + '\n')
    print(json.dumps(summaries, indent=2))


if __name__ == '__main__':
    main()
