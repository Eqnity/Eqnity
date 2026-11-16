"""Narration via edge-tts (male neural voice). One mp3 per line."""
import asyncio
import json
import os
import sys

import edge_tts


async def synth(text: str, voice: str, out_path: str) -> None:
    tts = edge_tts.Communicate(text, voice)
    await tts.save(out_path)


async def synth_all(lines, voice: str, out_dir: str):
    os.makedirs(out_dir, exist_ok=True)
    meta = []
    for i, (start, text) in enumerate(lines):
        path = os.path.join(out_dir, f"line_{i:02d}.mp3")
        await synth(text, voice, path)
        meta.append({"index": i, "start": start, "path": path, "text": text})
        print(f"tts: line {i} -> {path}", flush=True)
    with open(os.path.join(out_dir, "meta.json"), "w") as f:
        json.dump(meta, f, indent=2)
    return meta


if __name__ == "__main__":
    topic_file = sys.argv[1]
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import importlib.util
    spec = importlib.util.spec_from_file_location("topic", topic_file)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    asyncio.run(synth_all(mod.TOPIC["lines"], mod.TOPIC["voice"],
                          os.path.join(os.path.dirname(topic_file), "audio")))
