#!/usr/bin/env python3
"""Independent JVM8 query trials. Results are local microbenchmarks, never FPS."""
import argparse
import json
import pathlib
import random
import statistics
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java8-home", required=True)
    parser.add_argument("--minecraft-jar", required=True)
    parser.add_argument("--optifine-jar", required=True)
    parser.add_argument("--classpath-file", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--forks", type=int, default=5)
    parser.add_argument("--x-batch", action="store_true", help="Control compilation scheduling; label this separate JVM scope")
    args = parser.parse_args()
    if args.forks < 1:
        parser.error("forks must be positive")
    output = pathlib.Path(args.output)
    output.mkdir(parents=True, exist_ok=False)
    classpath = pathlib.Path(args.classpath_file).read_text().strip()
    jvm_args = ["-Xms512m", "-Xmx512m"] + (["-Xbatch"] if args.x_batch else [])
    command = [str(pathlib.Path(args.java8_home) / "bin/java")] + jvm_args + ["-cp", classpath,
               "org.polydevs.opusmc.optim.smoke.FrustumVerify", "trial", args.minecraft_jar, args.optifine_jar]
    results = []
    expected = {"inside": 1048576, "mixed-chunks": 185088, "outside": 0}
    roles = ["aa-control", "aa-repeat", "ab-control", "ab-treatment"]
    rng = random.Random(20261013)
    for fork in range(args.forks):
        schedule = [(workload, role) for workload in expected for role in roles]
        rng.shuffle(schedule)
        for workload, role in schedule:
            arm = "treatment" if role == "ab-treatment" else "control"
            completed = subprocess.run(command + [arm, workload], text=True, capture_output=True, check=True)
            report = json.loads(completed.stdout)
            assert report["scope"] == "LOCAL_QUERY_MICROBENCHMARK_NOT_GAME"
            assert report["visibleChecksums"] == [expected[workload]] * 10, (workload, role)
            report["role"] = role
            report["forkIndex"] = fork
            report["jvmArguments"] = jvm_args
            with (output / f"{workload}-{role}-{fork}.json").open("x") as stream:
                json.dump(report, stream, indent=2)
                stream.write("\n")
            results.append(report)
            nanos = statistics.median(report["sampleNanos"]) / report["queriesPerSample"]
            print(f"fork {fork + 1}/{args.forks} {workload} {role}: {nanos:.3f} ns/query", flush=True)
    assert len({r["sessionId"] for r in results}) == len(results)
    scope = ("java", "vm", "os", "arch", "targetSha256", "minecraftJarSha256", "optifineJarSha256", "helperSha256")
    assert all(tuple(r[k] for k in scope) == tuple(results[0][k] for k in scope) for r in results)
    summary = {"schemaVersion": 1, "scope": "LOCAL_QUERY_MICROBENCHMARK_NOT_GAME", "independentForks": len(results),
               "forksPerArmPerWorkload": args.forks, "jvmArguments": jvm_args,
               "gameFpsMeasured": False, "promotion": False, "workloads": {}}
    for workload in expected:
        arms = {}
        for role in roles:
            runs = [statistics.median(r["sampleNanos"]) / r["queriesPerSample"]
                    for r in results if r["workload"] == workload and r["role"] == role]
            arms[role] = {"forkMedianNsPerQuery": runs, "medianNsPerQuery": statistics.median(runs),
                          "min": min(runs), "max": max(runs)}
        control = arms["ab-control"]["medianNsPerQuery"]
        treatment = arms["ab-treatment"]["medianNsPerQuery"]
        aa = arms["aa-control"]["medianNsPerQuery"]
        arms["queryCostReductionPercent"] = 100 * (control - treatment) / control
        arms["aaMedianShiftPercent"] = 100 * (arms["aa-repeat"]["medianNsPerQuery"] - aa) / aa
        summary["workloads"][workload] = arms
    with (output / "summary.json").open("x") as stream:
        json.dump(summary, stream, indent=2)
        stream.write("\n")
    print(json.dumps({k: {"costReductionPercent": v["queryCostReductionPercent"], "aaShiftPercent": v["aaMedianShiftPercent"]}
                      for k, v in summary["workloads"].items()}, indent=2), flush=True)


if __name__ == "__main__":
    main()
