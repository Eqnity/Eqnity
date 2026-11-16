"""Post-pass over rendered frames: haze, captions, ticking counter (PIL)."""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

TOPIC = None  # set via load


def load_font(size, variable_axes=None):
    path = os.path.join(os.path.dirname(__file__), "fonts", "Baloo2.ttf")
    f = ImageFont.truetype(path, size)
    if variable_axes:
        try:
            f.set_variation_by_axes(variable_axes)
        except Exception:
            pass
    return f


def day_at(t, day_start_t, day_end_t, day_max):
    if t <= day_start_t:
        return 1.0
    p = min(1.0, (t - day_start_t) / (day_end_t - day_start_t))
    return 1.0 + (day_max - 1.0) * p


def fmt_mult(m):
    return f"x{int(m):,}"


def text_block(draw, xy, lines, font, fill=(255, 255, 255), stroke=8,
               stroke_fill=(0, 0, 0), anchor="ma", spacing=6):
    x, y = xy
    for i, line in enumerate(lines):
        if not line:
            continue
        draw.text((x, y), line, font=font, fill=fill, anchor=anchor,
                  stroke_width=stroke, stroke_fill=stroke_fill)
        bbox = draw.textbbox((x, y), line, font=font, anchor=anchor)
        y = bbox[3] + spacing + stroke


def main(frames_dir, topic_file):
    import importlib.util
    spec = importlib.util.spec_from_file_location("topic", topic_file)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    T = mod.TOPIC
    fps = T["fps"]
    W, H = T["width"], T["height"]
    day_start, day_end, day_max = T["day_start_t"], T["day_end_t"], T["day_max"]
    captions = T["captions"]
    n_frames = int(T["duration"] * fps)

    f_cap = load_font(74, [800])
    f_cap2 = load_font(54, [700])
    f_q = load_font(96, [800])
    f_day = load_font(120, [800])
    f_mult = load_font(64, [700])

    for n in range(n_frames):
        t = n / fps
        path = os.path.join(frames_dir, f"frame_{n:04d}.png")
        if not os.path.exists(path):
            print(f"overlay: MISSING {path}", flush=True)
            continue
        im = Image.open(path).convert("RGB")
        day = day_at(t, day_start, day_end, day_max)

        # ---- haze: grows with log10(multiplier), warm poison tint ----
        m = 2.0 ** max(0.0, day - 1.0)
        lm = math.log10(m + 1) / 9.0            # 0..~1 over the video
        haze_a = min(0.72, lm * 0.85)
        if haze_a > 0.01:
            tint = (120, 80, 30) if day < 20 else (60, 20, 15)
            haze = Image.new("RGB", (W, H), tint)
            im = Image.blend(im, haze, haze_a)
        # darkening toward end
        if day > 24:
            dark = Image.new("RGB", (W, H), (0, 0, 0))
            im = Image.blend(im, dark, min(0.55, (day - 24) / 14.0))

        draw = ImageDraw.Draw(im)

        # ---- ticking counter, fixed spot (top center) ----
        if t >= day_start:
            dl = int(min(day_max, day_at(t, day_start, day_end, day_max)))
            # tick pop: flash scale-ish by stroke bump every change
            prev = int(min(day_max, day_at(max(day_start, t - 1 / fps),
                                           day_start, day_end, day_max)))
            pop = 4 if dl != prev else 0
            cy = 150
            text_block(draw, (W // 2, cy), [f"DAY {dl}"], f_day,
                       stroke=10 + pop, anchor="ma")
            text_block(draw, (W // 2, cy + 135), [fmt_mult(2.0 ** (dl - 1))],
                       f_mult, fill=(255, 220, 90), stroke=7 + pop, anchor="ma")

        # ---- captions, bottom third ----
        cap = None
        for c in captions:
            if c[0] <= t <= c[1]:
                cap = c
                break
        if cap:
            lines = [l for l in cap[2:] if l]
            font = f_q if len(lines) == 1 and len(lines[0]) < 40 and cap[0] < 2.2 else f_cap
            # fade in/out
            edge = min(t - cap[0], cap[1] - t)
            alpha = min(1.0, edge / 0.25)
            block = Image.new("RGBA", (W, H), (0, 0, 0, 0))
            bd = ImageDraw.Draw(block)
            y0 = H - 330 if len(lines) > 1 or font is f_cap else H - 300
            text_block(bd, (W // 2, y0), lines[:1], font, anchor="ma", stroke=9)
            if len(lines) > 1:
                text_block(bd, (W // 2, y0 + 105), lines[1:2], f_cap2,
                           fill=(255, 240, 180), anchor="ma", stroke=7)
            if alpha < 1.0:
                block.putalpha(int(255 * alpha))
            im = Image.alpha_composite(im.convert("RGBA"), block).convert("RGB")

        im.save(path)
        if n % 60 == 0:
            print(f"overlay: {n}/{n_frames}", flush=True)
    print("OVERLAY_DONE")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
