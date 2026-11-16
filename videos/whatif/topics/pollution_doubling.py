"""Topic: What if world pollution doubled every day?

FACT CHECK (sources, checked at script time):
- 99.3 Mt CO2 emitted per day global average, 2024  -> Global Carbon Budget 2024 /
  Nature "Global carbon emissions and decarbonization in 2024" (36.3 Gt/yr).
- ~7 million premature deaths per year from air pollution -> WHO air pollution health topic.
- Air pollution really does double daily during winter inversions ->
  Utah Division of Air Quality (UCAIR public statements).
- Doubling math: multiplier = 2^(day-1). Day 7 = 64x, Day 14 = 8,192x,
  Day 20 = 524,288x, Day 30 = 536,870,912x, Day 31 = 1,073,741,824x.
"""

TOPIC = {
    "slug": "pollution-doubling",
    "question": "What if world pollution doubled every day?",
    "fps": 30,
    "width": 1080,
    "height": 1920,
    "duration": 38.0,          # seconds
    "day_start_t": 2.0,        # counter starts when question is done
    "day_end_t": 37.0,
    "day_max": 31.0,
    "voice": "en-US-GuyNeural",

    # narration: (start_seconds, text)
    "lines": [
        (0.15, "What if world pollution doubled every day?"),
        (2.2,  "Day one. We already pump 99 million tons of CO2 into the sky "
               "every single day. And air pollution kills 7 million people a year."),
        (9.0,  "Day seven. Sixty four times worse. The sky turns yellow. "
               "Hospitals start to overflow."),
        (14.0, "Day fourteen. Eight thousand times worse. The air is thick as smoke. "
               "Nobody can go outside anymore."),
        (20.0, "Day twenty. Half a million times worse. Noon looks like midnight. "
               "The oceans turn to acid."),
        (25.0, "Day thirty. Over half a billion times worse. The air is pure poison. "
               "Nothing survives."),
        (30.0, "Day thirty one. It doubles again. And again. And again."),
        (33.8, "Funny part? During winter inversions, air pollution really does "
               "double every single day."),
    ],

    # captions: (start, end, line1, line2)
    "captions": [
        (0.15, 2.1,  "What if world pollution doubled every day?", ""),
        (2.2,  9.0,  "DAY 1 — 99,000,000 tons CO2 / day", "7,000,000 deaths / year (WHO)"),
        (9.0,  14.0, "DAY 7 — x64 pollution", "Sky turns yellow"),
        (14.0, 20.0, "DAY 14 — x8,192", "Air thicker than smoke"),
        (20.0, 25.0, "DAY 20 — x524,288", "Noon looks like midnight"),
        (25.0, 30.0, "DAY 30 — x536,870,912", "Air is pure poison"),
        (30.0, 33.7, "DAY 31 — x1,073,741,824", "It doubles again. And again."),
        (33.8, 37.8, "Real fact: inversions double", "pollution daily (Utah DAQ)"),
    ],
}
