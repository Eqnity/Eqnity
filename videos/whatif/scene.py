"""Blender scene: Roblox-style city + blocky character, pollution escalation.

Run: blender -b -P scene.py -- --out DIR --frames START-END --engine cycles|workbench
Day state driven by frame: t = frame/fps, day = ramp(t).
"""
import argparse
import math
import random
import sys

import bpy
from mathutils import Vector

# ---------------- args ----------------
argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
ap = argparse.ArgumentParser()
ap.add_argument("--out", required=True)
ap.add_argument("--frames", required=True)  # "0-1139"
ap.add_argument("--engine", default="cycles")
ap.add_argument("--fps", type=int, default=30)
ap.add_argument("--day-start-t", type=float, default=2.0)
ap.add_argument("--day-end-t", type=float, default=37.0)
ap.add_argument("--day-max", type=float, default=31.0)
ap.add_argument("--samples", type=int, default=24)
args = ap.parse_args(argv)
f0, f1 = (int(x) for x in args.frames.split("-"))
W, H = 1080, 1920


def day_at(t: float) -> float:
    if t <= args.day_start_t:
        return 1.0
    p = min(1.0, (t - args.day_start_t) / (args.day_end_t - args.day_start_t))
    return 1.0 + (args.day_max - 1.0) * p


def mult_at(t: float) -> float:
    return 2.0 ** (max(0.0, day_at(t) - 1.0))


# ---------------- clear ----------------
bpy.ops.wm.read_factory_settings(use_empty=True)
scene = bpy.context.scene
scene.render.resolution_x = W
scene.render.resolution_y = H
scene.render.fps = args.fps
scene.frame_start, scene.frame_end = f0, f1

# ---------------- materials ----------------
def mat(name, color, rough=0.7, emit=0.0, metal=0.0):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = (*color, 1)
    bsdf.inputs["Roughness"].default_value = rough
    bsdf.inputs["Metallic"].default_value = metal
    if emit:
        bsdf.inputs["Emission Color"].default_value = (*color, 1)
        bsdf.inputs["Emission Strength"].default_value = emit
    return m


M = {
    "ground": mat("ground", (0.13, 0.13, 0.14)),
    "road": mat("road", (0.07, 0.07, 0.08)),
    "stripe": mat("stripe", (0.9, 0.85, 0.4), emit=0.3),
    "bldg": [mat(f"bldg{i}", c) for i, c in enumerate([
        (0.55, 0.57, 0.62), (0.7, 0.5, 0.4), (0.45, 0.55, 0.65),
        (0.6, 0.6, 0.5), (0.5, 0.45, 0.55)])],
    "window": mat("window", (1.0, 0.85, 0.4), emit=2.0),
    "skin": mat("skin", (1.0, 0.8, 0.35)),
    "shirt": mat("shirt", (0.15, 0.5, 0.9)),
    "pants": mat("pants", (0.2, 0.25, 0.5)),
    "face": mat("face", (0.05, 0.05, 0.05)),
    "mask": mat("mask", (0.1, 0.1, 0.12)),
    "smoke": mat("smoke", (0.35, 0.33, 0.3), rough=1.0),
    "sun": mat("sun", (1.0, 0.9, 0.5), emit=30.0),
}


def box(name, loc, scale, material):
    bpy.ops.mesh.primitive_cube_add(size=1, location=loc)
    ob = bpy.context.object
    ob.name = name
    ob.scale = scale
    ob.data.materials.append(material)
    return ob


# ---------------- world / sky ----------------
world = bpy.data.worlds.new("World")
scene.world = world
world.use_nodes = True
bg = world.node_tree.nodes["Background"]
bg.inputs[0].default_value = (0.35, 0.6, 0.95, 1)
bg.inputs[1].default_value = 1.0

# ---------------- city ----------------
random.seed(42)
box("ground", (0, 0, -0.1), (400, 400, 0.2), M["ground"])
# roads: cross
box("road_h", (0, 0, 0.01), (400, 14, 0.02), M["road"])
box("road_v", (0, 0, 0.02), (14, 400, 0.02), M["road"])
for i in range(-18, 19):
    box(f"stripe_h{i}", (i * 10, 0, 0.04), (4, 0.5, 0.02), M["stripe"])
    box(f"stripe_v{i}", (0, i * 10, 0.04), (0.5, 4, 0.02), M["stripe"])

buildings = []
for gx in range(-6, 7):
    for gy in range(-6, 7):
        if abs(gx) <= 1 and abs(gy) <= 1:
            continue
        if gx == 0 or gy == 0:
            continue
        x = gx * 24 + random.uniform(-3, 3)
        y = gy * 24 + random.uniform(-3, 3)
        h = random.uniform(8, 45)
        w = random.uniform(9, 15)
        b = box(f"bldg_{gx}_{gy}", (x, y, h / 2), (w, w, h), random.choice(M["bldg"]))
        buildings.append(b)
        # windows strip
        box(f"win_{gx}_{gy}", (x, y - w / 2 - 0.05, h * 0.6),
            (w * 0.7, 0.1, h * 0.5), M["window"])

# factories with chimneys (smoke grows with day)
chimneys = []
for i, (fx, fy) in enumerate([(-70, 60), (75, -55), (-80, -75), (65, 80)]):
    box(f"fac_{i}", (fx, fy, 7), (22, 16, 14), M["bldg"][1])
    c = box(f"chim_{i}", (fx + 6, fy, 20), (3, 3, 26), M["bldg"][0])
    chimneys.append((fx + 6, fy))

# smoke puffs (scaled per frame)
puffs = []
for ci, (cx, cy) in enumerate(chimneys):
    for j in range(6):
        p = box(f"puff_{ci}_{j}", (cx, cy, 34 + j * 7),
                (2, 2, 2), M["smoke"])
        puffs.append((p, ci, j))

# ---------------- Roblox R6 character ----------------
char = bpy.data.objects.new("char_root", None)
bpy.context.collection.objects.link(char)
char.location = (0, -30, 0)

parts = {}
parts["torso"] = box("torso", (0, 0, 4.6), (4, 2, 4), M["shirt"])
parts["head"] = box("head", (0, 0, 8.1), (2.6, 2.6, 2.6), M["skin"])
parts["arm_l"] = box("arm_l", (-3.1, 0, 4.6), (2, 2, 4), M["skin"])
parts["arm_r"] = box("arm_r", (3.1, 0, 4.6), (2, 2, 4), M["skin"])
parts["leg_l"] = box("leg_l", (-1.1, 0, 0.6), (2, 2, 4), M["pants"])
parts["leg_r"] = box("leg_r", (1.1, 0, 0.6), (2, 2, 4), M["pants"])
# face: eyes + smile
parts["eye_l"] = box("eye_l", (-0.6, -1.35, 8.5), (0.45, 0.1, 0.55), M["face"])
parts["eye_r"] = box("eye_r", (0.6, -1.35, 8.5), (0.45, 0.1, 0.55), M["face"])
parts["smile"] = box("smile", (0, -1.35, 7.5), (1.3, 0.1, 0.25), M["face"])
# mask (hidden until day >= 7)
mask = box("mask", (0, -1.4, 7.7), (2.7, 0.35, 1.9), M["mask"])
mask.hide_render = True
mask.hide_viewport = True

for ob in list(parts.values()) + [mask]:
    ob.parent = char

# ---------------- sun ----------------
bpy.ops.object.light_add(type="SUN", location=(50, -50, 120))
sun = bpy.context.object
sun.data.energy = 4.0
sun.rotation_euler = (math.radians(50), 0, math.radians(30))

bpy.ops.object.light_add(type="AREA", location=(0, -40, 60))
fill = bpy.context.object
fill.data.energy = 800
fill.data.size = 60

# ---------------- camera ----------------
bpy.ops.object.camera_add(location=(0, -75, 22))
cam = bpy.context.object
scene.camera = cam


def look_at(obj, target):
    d = Vector(target) - obj.location
    obj.rotation_euler = d.to_track_quat("-Z", "Y").to_euler()


look_at(cam, (0, -20, 12))

# ---------------- per-frame state ----------------
SKY_STOPS = [  # (day, sky color)
    (1.0, (0.35, 0.60, 0.95)),
    (3.0, (0.75, 0.75, 0.45)),
    (7.0, (0.85, 0.65, 0.25)),
    (13.0, (0.8, 0.4, 0.15)),
    (19.0, (0.45, 0.12, 0.08)),
    (27.0, (0.12, 0.03, 0.03)),
    (31.0, (0.02, 0.01, 0.01)),
]


def sky_color(day):
    for i in range(len(SKY_STOPS) - 1):
        d0, c0 = SKY_STOPS[i]
        d1, c1 = SKY_STOPS[i + 1]
        if d0 <= day <= d1:
            f = (day - d0) / (d1 - d0)
            return tuple(c0[k] + (c1[k] - c0[k]) * f for k in range(3))
    return SKY_STOPS[-1][1]


def state(frame):
    t = frame / args.fps
    day = day_at(t)
    # character animation
    if day >= 30:
        char.location = (0, -30, -50)      # gone
    elif day >= 20:
        char.location = (0, -30, 0.8)      # crouched
        char.rotation_euler = (math.radians(15), 0, 0)
        char.scale = (1, 1, 0.8)
    else:
        char.location = (0, -30, 0)
        char.rotation_euler = (0, 0, 0)
        char.scale = (1, 1, 1)
    # cough shake 14..19
    if 14 <= day < 20:
        char.location.y = -30 + (0.4 if (frame // 4) % 2 else -0.4)
    # mask visible day 7..29
    show_mask = 7 <= day < 30
    mask.hide_render = not show_mask
    mask.hide_viewport = not show_mask
    # arms: wave early, cover face mid, drop late
    if day < 7:
        parts["arm_r"].rotation_euler = (0, math.radians(-140 + 20 * math.sin(t * 6)), 0)
        parts["arm_l"].rotation_euler = (0, math.radians(20), 0)
    elif day < 14:
        parts["arm_r"].rotation_euler = (math.radians(-120), 0, 0)
        parts["arm_l"].rotation_euler = (math.radians(-120), 0, 0)
    elif day < 20:
        parts["arm_r"].rotation_euler = (math.radians(-90), 0, 0)
        parts["arm_l"].rotation_euler = (math.radians(-90), 0, 0)
    else:
        parts["arm_r"].rotation_euler = (0, 0, 0)
        parts["arm_l"].rotation_euler = (0, 0, 0)
    # smoke puffs grow + rise
    for p, ci, j in puffs:
        g = min(1.0, max(0.15, (day - 1) / 12.0))
        s = 1.5 + j * 2.2 * g + (1.2 if day > 10 else 0)
        p.scale = (s, s, s * 0.8)
        p.location.z = 34 + j * 7 * (0.6 + 0.4 * g) + ((frame * 0.5 + j * 9) % 14) * (0.2 + g * 0.4)
    # sun dims out by day 13
    sun.data.energy = max(0.0, 4.0 * (1.0 - (day - 1) / 12.0)) if day > 1 else 4.0
    # sky
    c = sky_color(day)
    bg.inputs[0].default_value = (*c, 1)
    # window lights off at day 30+
    M["window"].node_tree.nodes["Principled BSDF"].inputs["Emission Strength"].default_value = \
        0.0 if day >= 30 else 2.0
    # camera slow push-in
    p = min(1.0, t / args.duration if hasattr(args, "duration") else t / 38.0)
    cam.location = (0, -75 + 25 * p, 22 - 6 * p)
    look_at(cam, (0, -25, 12))


def on_frame(sc, depsgraph=None):
    state(sc.frame_current)


bpy.app.handlers.frame_change_post.append(on_frame)

# ---------------- render config ----------------
out_dir = args.out
import os
os.makedirs(out_dir, exist_ok=True)
scene.render.filepath = os.path.join(out_dir, "frame_")
scene.render.image_settings.file_format = "PNG"
scene.render.film_transparent = False

if args.engine == "cycles":
    scene.render.engine = "CYCLES"
    scene.cycles.samples = args.samples
    scene.cycles.use_denoising = True
    scene.cycles.use_adaptive_sampling = True
    scene.cycles.adaptive_threshold = 0.05
    scene.cycles.device = "CPU"
else:
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.light = "STUDIO"
    scene.display.shading.show_cavity = True

state(f0)
bpy.ops.render.render(animation=True)
print("RENDER_DONE")
