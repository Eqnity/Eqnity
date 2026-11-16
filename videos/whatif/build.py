#!/usr/bin/env python3
"""Orchestrator: TTS -> Blender frames -> overlay -> ffmpeg assembly.

Usage: python3 build.py topics/<topic>.py [--engine cycles|workbench] [--samples N]
"""
import argparse
import importlib.util
import os
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))


def run(cmd, **kw):
    print("+", " ".join(str(c) for c in cmd), flush=True)
    subprocess.run(cmd, check=True, **kw)


def load_topic(path):
    spec = importlib.util.spec_from_file_location("topic", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod.TOPIC


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("topic")
    ap.add_argument("--engine", default="workbench", choices=["cycles", "workbench"])
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--skip-audio", action="store_true")
    args = ap.parse_args()

    T = load_topic(args.topic)
    slug = T["slug"]
    work = os.path.join(HERE, "out", slug)
    frames = os.path.join(work, "frames")
    os.makedirs(frames, exist_ok=True)

    # ---- 1. TTS ----
    if not args.skip_audio:
        run([sys.executable, os.path.join(HERE, "tts.py"), args.topic])

    # ---- 2. Blender frames ----
    n = int(T["duration"] * T["fps"]) - 1
    blender = shutil.which("blender") or "blender"
    run([blender, "-b", "-P", os.path.join(HERE, "scene.py"), "--",
         "--out", frames, "--frames", f"0-{n}", "--engine", args.engine,
         "--samples", str(args.samples), "--fps", str(T["fps"]),
         "--day-start-t", str(T["day_start_t"]),
         "--day-end-t", str(T["day_end_t"]),
         "--day-max", str(T["day_max"])])

    # ---- 3. overlay ----
    run([sys.executable, os.path.join(HERE, "overlay.py"), frames, args.topic])

    # ---- 4. audio mix: narration lines at offsets ----
    audio_dir = os.path.join(os.path.dirname(os.path.abspath(args.topic)), "audio")
    import json
    meta = json.load(open(os.path.join(audio_dir, "meta.json")))
    inputs, filters, mixes = [], [], []
    for i, m in enumerate(meta):
        inputs += ["-i", m["path"]]
        start_ms = int(m["start"] * 1000)
        filters.append(f"[{i}:a]adelay={start_ms}|{start_ms}[a{i}]")
        mixes.append(f"[a{i}]")
    dur = T["duration"]
    af = ";".join(filters) + ";" + "".join(mixes) + \
        f"amix=inputs={len(meta)}:normalize=0,apad=whole_dur={dur},aformat=sample_rates=44100:channel_layouts=stereo[aout]"
    audio_out = os.path.join(work, "narration.wav")
    run(["ffmpeg", "-y", *inputs, "-filter_complex", af, "-map", "[aout]", audio_out])

    # ---- 5. video from frames ----
    video_raw = os.path.join(work, "video_raw.mp4")
    run(["ffmpeg", "-y", "-framerate", str(T["fps"]),
         "-i", os.path.join(frames, "frame_%04d.png"),
         "-c:v", "libx264", "-preset", "medium", "-crf", "18",
         "-pix_fmt", "yuv420p", video_raw])

    # ---- 6. mux ----
    final = os.path.join(work, f"{slug}.mp4")
    run(["ffmpeg", "-y", "-i", video_raw, "-i", audio_out,
         "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
         "-shortest", "-movflags", "+faststart", final])
    print(f"FINAL: {final}")


if __name__ == "__main__":
    main()
