#!/usr/bin/env python3
"""Run PR verification on a disposable Apple simulator owned by this process."""
import json
from pathlib import Path
import subprocess
import sys
import uuid

root = Path(__file__).resolve().parents[1]
target = sys.argv[1] if len(sys.argv) == 2 else ""
if target not in {"iosSimulatorArm64", "iosX64"}:
    raise SystemExit("Usage: python3 verification/apple-ci.py iosSimulatorArm64|iosX64")
devices = json.loads(subprocess.check_output(["xcrun", "simctl", "list", "devices", "available", "--json"]))
source = next(device["udid"] for runtime, entries in devices["devices"].items()
              if "iOS" in runtime for device in entries
              if device.get("isAvailable") and "iPhone" in device["name"] and device["state"] == "Shutdown")
clone = subprocess.check_output(["xcrun", "simctl", "clone", source, "Publication-" + uuid.uuid4().hex], text=True).strip()
try:
    subprocess.run(["xcrun", "simctl", "boot", clone], check=True)
    subprocess.run(["xcrun", "simctl", "bootstatus", clone, "-b"], check=True)

    subprocess.run([sys.executable, str(root / "tooling/verify-publication.py"),
                    "--platform", target, "--ios-device", clone], cwd=root, check=True)
finally:
    subprocess.run(["xcrun", "simctl", "shutdown", clone], check=False)
    subprocess.run(["xcrun", "simctl", "delete", clone], check=True)
