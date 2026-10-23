#!/usr/bin/env python3
"""Same pinned font kernel, old/new O2 core JARs; AA/AB forks, no game FPS claim."""
import argparse
import hashlib
import json
from pathlib import Path
import statistics
import subprocess
import os


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('java8-home', 'minecraft-jar', 'optifine-jar', 'classpath-file',
                 'baseline-core', 'candidate-core', 'output'):
        p.add_argument('--' + name, type=Path, required=True)
    p.add_argument('--pairs', type=int, default=5)
    p.add_argument('--aa-pairs', type=int, default=3)
    a = p.parse_args()
    if min(a.pairs, a.aa_pairs) < 1 or a.output.exists():
        p.error('Positive pair counts and a new output path required')
    cp = a.classpath_file.read_text().strip()
    cores = dict(baseline=a.baseline_core.resolve(), treatment=a.candidate_core.resolve())
    rows = []
    identity = None
    for workload in ('ascii', 'unicode', 'glyphs'):
        for phase, pairs in (('AA', a.aa_pairs), ('AB', a.pairs)):
            for pair in range(pairs):
                roles = [('control', 'baseline'), ('repeat' if phase == 'AA' else 'treatment',
                                                  'baseline' if phase == 'AA' else 'treatment')]
                if pair % 2:
                    roles.reverse()
                for role, variant in roles:
                    command = [str(a.java8_home / 'bin/java'), '-Xms512m', '-Xmx512m', '-cp',
                               str(cores[variant]) + os.pathsep + cp,
                               'org.polydevs.opusmc.optim.smoke.PipelineVerify', 'trial',
                               str(a.minecraft_jar), str(a.optifine_jar), 'treatment', workload]
                    row = json.loads(subprocess.check_output(command, text=True))
                    current = {k: row[k] for k in ('scope', 'java', 'arch', 'minecraftSha256',
                                                   'optifineSha256', 'targets')}
                    if identity is None:
                        identity = current
                    if current != identity or len(set(row['checksums'])) != 1:
                        raise ValueError('Scope identity/checksum changed')
                    if row['blackhole'] != row['checksums'][0] or min(row['sampleNanos']) <= 0:
                        raise ValueError('Invalid timed samples')
                    rows.append(dict(row, phase=phase, pair=pair, role=role, variant=variant,
                                     medianNsPerQuery=statistics.median(row['sampleNanos']) / row['queriesPerSample']))
        print(workload + ': AA/AB forks complete', flush=True)
    summaries = {}
    for workload in ('ascii', 'unicode', 'glyphs'):
        summaries[workload] = {}
        selected = [r for r in rows if r['workload'] == workload]
        if len({r['checksums'][0] for r in selected}) != 1:
            raise ValueError('Cross-arm checksum mismatch')
        for phase, pairs in (('AA', a.aa_pairs), ('AB', a.pairs)):
            ratios = []
            for pair in range(pairs):
                runs = [r for r in selected if r['phase'] == phase and r['pair'] == pair]
                control = next(r for r in runs if r['role'] == 'control')
                other = next(r for r in runs if r['role'] != 'control')
                ratios.append(other['medianNsPerQuery'] / control['medianNsPerQuery'])
            summaries[workload][phase] = dict(pairedRatios=ratios, medianRatio=statistics.median(ratios))
    report = dict(schemaVersion=1, scope='REAL_FONT_WIDTH_KERNEL_OLD_VS_NEW_O2_NOT_GAME_FPS',
                  identity=identity, coreSha256={k: hashlib.sha256(v.read_bytes()).hexdigest() for k, v in cores.items()},
                  javaVersion=subprocess.check_output([str(a.java8_home / 'bin/java'), '-version'],
                                                     stderr=subprocess.STDOUT, text=True).strip(),
                  summaries=summaries, runs=rows)
    a.output.parent.mkdir(parents=True, exist_ok=True)
    a.output.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(summaries, indent=2))


if __name__ == '__main__':
    main()
