#!/usr/bin/env python3
"""Independent real-method font kernels with A/A controls; never game FPS evidence."""
import argparse
import json
import pathlib
import random
import statistics
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("java8-home", "minecraft-jar", "optifine-jar", "classpath-file", "output"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--forks", type=int, default=5)
    parser.add_argument("--x-batch", action="store_true")
    args = parser.parse_args()
    if args.forks < 1:
        parser.error("forks must be positive")
    output = pathlib.Path(args.output)
    output.mkdir(parents=True, exist_ok=False)
    classpath = pathlib.Path(args.classpath_file).read_text().strip()
    flags = ["-Xms512m", "-Xmx512m"] + (["-Xbatch"] if args.x_batch else [])
    command = [str(pathlib.Path(args.java8_home) / "bin/java"), *flags, "-cp", classpath,
               "org.polydevs.opusmc.optim.smoke.PipelineVerify", "trial", args.minecraft_jar, args.optifine_jar]
    roles = ["aa-control", "aa-repeat", "ab-control", "ab-treatment"]
    reports = []
    rng = random.Random(20261014)
    checksums = {}
    for fork in range(args.forks):
        schedule = [(workload, role) for workload in ("ascii", "unicode") for role in roles]
        rng.shuffle(schedule)
        for workload, role in schedule:
            arm = "treatment" if role == "ab-treatment" else "control"
            completed = subprocess.run(command + [arm, workload], text=True, capture_output=True, check=True)
            row = json.loads(completed.stdout)
            if row["scope"] != "REAL_METHOD_KERNELS_WITH_OWNED_SUBSTITUTES_NOT_GAME":
                raise ValueError("Unexpected scope")
            wanted = checksums.setdefault(workload, row["checksums"][0])
            if row["checksums"] != [wanted] * 10 or row["blackhole"] != wanted:
                raise ValueError("Checksum mismatch")
            if len(row["sampleNanos"]) != 10 or min(row["sampleNanos"]) <= 0:
                raise ValueError("Invalid samples")
            row.update(role=role, forkIndex=fork, jvmArguments=flags)
            (output / f"{workload}-{role}-{fork}.json").write_text(json.dumps(row, indent=2) + "\n")
            reports.append(row)
            print(f"fork {fork+1}/{args.forks} {workload} {role}: "
                  f"{statistics.median(row['sampleNanos'])/row['queriesPerSample']:.3f} ns/query", flush=True)
    if len({r["sessionId"] for r in reports}) != len(reports):
        raise ValueError("Reused sessions")
    keys = ("java", "arch", "minecraftSha256", "optifineSha256", "targets", "helperSha256")
    identities = [json.dumps({k: r[k] for k in keys}, sort_keys=True) for r in reports]
    if len(set(identities)) != 1:
        raise ValueError("Scope identity changed")
    summary = {"schemaVersion": 1, "scope": reports[0]["scope"], "independentForks": len(reports),
               "forksPerArmPerWorkload": args.forks, "jvmArguments": flags,
               "gameFpsMeasured": False, "promotion": False, "workloads": {}}
    for workload in ("ascii", "unicode"):
        arms = {}
        for role in roles:
            times = [statistics.median(r["sampleNanos"]) / r["queriesPerSample"]
                     for r in reports if r["workload"] == workload and r["role"] == role]
            arms[role] = {"forkMedianNsPerQuery": times, "medianNsPerQuery": statistics.median(times)}
        control = arms["ab-control"]["medianNsPerQuery"]
        effect = 100 * (control - arms["ab-treatment"]["medianNsPerQuery"]) / control
        aa = arms["aa-control"]["medianNsPerQuery"]
        noise = 100 * (arms["aa-repeat"]["medianNsPerQuery"] - aa) / aa
        arms.update(queryCostReductionPercent=effect, aaMedianShiftPercent=noise)
        arms["interpretation"] = "REGRESSION" if effect < 0 else (
            "INCONCLUSIVE" if effect <= abs(noise) else "OBSERVED_LOCAL_QUERY_REDUCTION_NO_GAME_ACCEPTANCE")
        summary["workloads"][workload] = arms
    (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps({k: {x: v[x] for x in ("queryCostReductionPercent", "aaMedianShiftPercent", "interpretation")}
                      for k, v in summary["workloads"].items()}, indent=2), flush=True)


if __name__ == "__main__":
    main()
