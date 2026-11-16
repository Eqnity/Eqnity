#!/usr/bin/env bash
# Codespace deps for whatif pipeline
set -euo pipefail
cd "$(dirname "$0")"

sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq blender ffmpeg fonts-dejavu-core >/dev/null

python3 -m pip install -q --break-system-packages edge-tts pillow || \
  python3 -m pip install -q edge-tts pillow

mkdir -p fonts
if [ ! -f fonts/Baloo2.ttf ]; then
  curl -fsSL -o fonts/Baloo2.ttf \
    "https://raw.githubusercontent.com/google/fonts/main/ofl/baloo2/Baloo2%5Bwght%5D.ttf" || \
  curl -fsSL -o fonts/Baloo2.ttf \
    "https://github.com/google/fonts/raw/main/ofl/baloo2/Baloo2%5Bwght%5D.ttf"
fi

blender --version | head -1
ffmpeg -version | head -1
python3 -c "import edge_tts, PIL; print('py deps ok')"
ls -la fonts/
