#!/usr/bin/env python3
"""Independent full-ResourceLocation query and allocation forks; not game/FPS evidence."""
import argparse
import json
import pathlib
import random
import statistics
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("java8-home", "minecraft-jar", "optifine-jar", "commons-lang-jar", "classpath-file", "output"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--forks", type=int, default=5)
    args = parser.parse_args()
    if args.forks < 1:
        parser.error("forks must be positive")
    output = pathlib.Path(args.output)
    output.mkdir(parents=True, exist_ok=False)
    flags = ["-Xms512m", "-Xmx512m"]
    command = [str(pathlib.Path(args.java8_home) / "bin/java"), *flags, "-cp",
               pathlib.Path(args.classpath_file).read_text().strip(),
               "org.polydevs.opusmc.optim.smoke.ResourceVerify", "trial",
               args.minecraft_jar, args.optifine_jar, args.commons_lang_jar]
    fixture = [f"opus{i % 17}:textures/{i}/resource.png" for i in range(4096)]
    expected = 64 * sum(len(text) * 31 + ord(text[0]) + ord(text[-1]) for text in fixture)
    roles = ["aa-control", "aa-repeat", "ab-control", "ab-treatment"]
    reports = []
    rng = random.Random(20261014)
    for fork in range(args.forks):
        schedule = [(workload, role) for workload in ("warm", "cold") for role in roles]
        rng.shuffle(schedule)
        for workload, role in schedule:
            arm = "treatment" if role == "ab-treatment" else "control"
            done = subprocess.run(command + [arm, workload], capture_output=True, text=True, check=True)
            row = json.loads(done.stdout)
            if row["scope"] != "FULL_RAW_RESOURCE_CLASS_NOT_GAME" or row["checksums"] != [expected] * 8:
                raise ValueError("Scope/checksum mismatch")
            if row["blackhole"] != expected or len(row["sampleNanos"]) != 8 or min(row["sampleNanos"]) <= 0:
                raise ValueError("Invalid sample")
            if len(row["sampleAllocatedBytes"]) != 8 or min(row["sampleAllocatedBytes"]) < 0:
                raise ValueError("Invalid allocation counter")
            row.update(role=role, forkIndex=fork, jvmArguments=flags)
            (output / f"{workload}-{role}-{fork}.json").write_text(json.dumps(row, indent=2) + "\n")
            reports.append(row)
            print(f"fork {fork+1}/{args.forks} {workload} {role}: "
                  f"{statistics.median(row['sampleNanos'])/row['queriesPerSample']:.3f} ns, "
                  f"{statistics.median(row['sampleAllocatedBytes'])/row['queriesPerSample']:.3f} B/query", flush=True)
    if len({row["sessionId"] for row in reports}) != len(reports):
        raise ValueError("Reused sessions")
    keys = ("java", "arch", "minecraftSha256", "optifineSha256", "commonsLangSha256", "originalSha256", "patchedSha256", "fixture")
    if len({json.dumps({k: row[k] for k in keys}, sort_keys=True) for row in reports}) != 1:
        raise ValueError("Changed scope identity")
    summary = {"schemaVersion": 1, "scope": reports[0]["scope"], "independentForks": len(reports),
               "forksPerArmPerWorkload": args.forks, "jvmArguments": flags,
               "gameFpsMeasured": False, "liveHeapMeasured": False, "promotion": False, "workloads": {}}
    for workload in ("warm", "cold"):
        arms = {}
        for role in roles:
            rows = [r for r in reports if r["workload"] == workload and r["role"] == role]
            times = [statistics.median(r["sampleNanos"]) / r["queriesPerSample"] for r in rows]
            allocations = [statistics.median(r["sampleAllocatedBytes"]) / r["queriesPerSample"] for r in rows]
            arms[role] = {"forkMedianNsPerQuery": times, "medianNsPerQuery": statistics.median(times),
                          "forkMedianAllocatedBytesPerQuery": allocations, "medianAllocatedBytesPerQuery": statistics.median(allocations)}
        baseline = arms["ab-control"]["medianNsPerQuery"]
        effect = 100 * (baseline - arms["ab-treatment"]["medianNsPerQuery"]) / baseline
        aa = arms["aa-control"]["medianNsPerQuery"]
        noise = 100 * (arms["aa-repeat"]["medianNsPerQuery"] - aa) / aa
        arms.update(queryCostReductionPercent=effect, aaMedianShiftPercent=noise)
        arms["interpretation"] = "REGRESSION" if effect < 0 else (
            "INCONCLUSIVE" if effect <= abs(noise) else "OBSERVED_LOCAL_QUERY_REDUCTION_NO_GAME_ACCEPTANCE")
        summary["workloads"][workload] = arms
    (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps({w: {k: d[k] for k in ("queryCostReductionPercent", "aaMedianShiftPercent", "interpretation")}
                      for w, d in summary["workloads"].items()}, indent=2), flush=True)


if __name__ == "__main__":
    main()
