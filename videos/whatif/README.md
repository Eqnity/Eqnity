# What-If Videos Pipeline

30-45s vertical videos (1080x1920@30fps): question in first 2s, ticking day counter,
white bold rounded captions, escalating visuals, male narration (edge-tts), Roblox-style
3D city + blocky character (Blender), rendered on GitHub Codespaces.

## Run (inside codespace)

```bash
cd videos/whatif
bash setup.sh                                  # blender, ffmpeg, edge-tts, font
python3 build.py topics/pollution_doubling.py  # TTS -> frames -> overlay -> mp4
```

Output: `out/<slug>/<slug>.mp4`

## Files

| file | role |
|---|---|
| `topics/*.py` | topic: facts + script + captions (fact-check notes inline) |
| `tts.py` | edge-tts narration (male voice) per line |
| `scene.py` | Blender scene: city, character, escalation (day-driven) |
| `overlay.py` | haze + captions + ticking DAY counter (PIL) |
| `build.py` | orchestrator, ffmpeg assembly |
| `setup.sh` | codespace dependencies |

## Recipe

- question appears 0-2s
- `DAY n` counter ticks nonstop top-center from t=2s, `x2^(n-1)` under it
- caption every few seconds, white bold rounded (Baloo 2), with voice
- each step visually worse: sky blue->yellow->orange->red->black, smoke grows,
  character waves -> mask -> cough -> crouches -> gone
