#!/usr/bin/env python3
"""Generates data/opencomputers/advancements/*.json from the 1.12 achievement tree
(li.cil.oc.common.Achievement in OpenComputers master-MC1.12) and adds
advancements.opencomputers.<name>.title/.description lang keys mapped from the
existing achievement.oc.<name>(.desc) strings. Run from the repository root."""
import json
import os
import re

RES = "common/src/main/resources"
ADV = RES + "/data/opencomputers/advancements"
LANG = RES + "/assets/opencomputers/lang"

# (1.12 name, parent, trigger, items); trigger "obtain" = minecraft:inventory_changed,
# "assemble" = opencomputers:assembled (fired when the assembler finishes / the item is picked up).
TREE = [
    ("transistor", None, "obtain", ["transistor"]),
    ("disassembler", "transistor", "obtain", ["disassembler"]),
    ("chip", "transistor", "obtain", ["chip1", "chip2", "chip3"]),
    ("capacitor", "chip", "obtain", ["capacitor"]),
    ("assembler", "capacitor", "obtain", ["assembler"]),
    ("microcontroller", "assembler", "assemble", ["microcontroller"]),
    ("robot", "assembler", "assemble", ["robot"]),
    ("drone", "assembler", "assemble", ["drone"]),
    ("tablet", "assembler", "assemble", ["tablet"]),
    ("charger", "capacitor", "obtain", ["charger"]),
    ("cpu", "chip", "obtain", ["cpu1", "cpu2", "cpu3"]),
    ("motionSensor", "cpu", "obtain", ["motionsensor"]),
    ("geolyzer", "cpu", "obtain", ["geolyzer"]),
    ("redstoneIO", "cpu", "obtain", ["redstone"]),
    ("eeprom", "chip", "obtain", ["eeprom"]),
    ("ram", "chip", "obtain", ["ram1", "ram2", "ram3", "ram4", "ram5", "ram6"]),
    ("hdd", "chip", "obtain", ["hdd1", "hdd2", "hdd3"]),
    ("case", "chip", "obtain", ["case1", "case2", "case3"]),
    ("rack", "case", "obtain", ["rack"]),
    ("server", "rack", "obtain", ["server1", "server2", "server3"]),
    ("screen", "chip", "obtain", ["screen1", "screen2", "screen3"]),
    ("keyboard", "screen", "obtain", ["keyboard"]),
    ("hologram", "screen", "obtain", ["hologram1", "hologram2"]),
    ("diskDrive", "chip", "obtain", ["diskdrive"]),
    ("floppy", "diskDrive", "obtain", ["floppy"]),
    ("openOS", "floppy", "openos", ["floppy"]),
    ("raid", "diskDrive", "obtain", ["raid"]),
    ("card", None, "obtain", ["card"]),
    ("redstoneCard", "card", "obtain", ["redstonecard1", "redstonecard2"]),
    ("graphicsCard", "card", "obtain", ["graphicscard1", "graphicscard2", "graphicscard3"]),
    ("networkCard", "card", "obtain", ["lancard"]),
    ("wirelessNetworkCard", "networkCard", "obtain", ["wlancard1", "wlancard2"]),
    ("cable", None, "obtain", ["cable"]),
    ("powerDistributor", "cable", "obtain", ["powerdistributor"]),
    ("switch", "cable", "obtain", ["relay"]),
    ("adapter", "cable", "obtain", ["adapter"]),
]
OPENOS_NBT = '{"oc:lootFactory":"opencomputers:openos"}'


def file_name(name):
    return re.sub(r"(?<=[a-z])([A-Z]+)", r"_\1", name).lower()


def oc(item):
    return "opencomputers:" + item


def display(name, icon, frame="task", **extra):
    d = {
        "icon": icon,
        "title": {"translate": "advancements.opencomputers.%s.title" % file_name(name)},
        "description": {"translate": "advancements.opencomputers.%s.description" % file_name(name)},
        "frame": frame,
        "show_toast": True,
        "announce_to_chat": True,
        "hidden": False,
    }
    d.update(extra)
    return d


def write(name, data):
    with open("%s/%s.json" % (ADV, file_name(name)), "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    os.makedirs(ADV, exist_ok=True)
    for f in os.listdir(ADV):
        os.remove(os.path.join(ADV, f))

    # Root: shown as soon as the player has the manual (given on first join) or any of the
    # 1.12 tree roots.
    root_items = ["manual", "transistor", "card", "cable"]
    write("root", {
        "display": display("root", {"item": oc("case1")}, background="opencomputers:textures/blocks/generic_top.png",
                           show_toast=False, announce_to_chat=False),
        "criteria": {i: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [oc(i)]}]}}
                     for i in root_items},
        "requirements": [root_items],
    })

    for name, parent, trigger, items in TREE:
        if trigger == "assemble":
            criterion = {"trigger": "opencomputers:assembled", "conditions": {"item": {"items": [oc(i) for i in items]}}}
        else:
            pred = {"items": [oc(i) for i in items]}
            if trigger == "openos":
                pred["nbt"] = OPENOS_NBT
            criterion = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [pred]}}
        icon = {"item": oc(items[0])}
        if trigger == "openos":
            icon["nbt"] = OPENOS_NBT
        write(name, {
            "parent": "opencomputers:" + file_name(parent or "root"),
            "display": display(name, icon, frame="goal" if trigger == "assemble" else "task"),
            "criteria": {trigger: criterion},
            "requirements": [[trigger]],
        })

    # Lang keys: insert after the last achievement.oc.* line of each file that has them.
    for lang in sorted(os.listdir(LANG)):
        path = os.path.join(LANG, lang)
        with open(path, encoding="utf-8") as f:
            text = f.read()
        data = json.loads(text)
        if "achievement.oc.transistor" not in data:
            continue
        lines = [l for l in text.split("\n") if '"advancements.opencomputers.' not in l]
        new = []
        if lang == "en_us.json":
            new.append(("root", "OpenComputers", "Computers, robots and drones you program in Lua."))
        for name, *_ in TREE:
            new.append((name, data["achievement.oc." + name], data["achievement.oc." + name + ".desc"]))
        last = max(i for i, l in enumerate(lines) if '"achievement.oc.' in l)
        indent = re.match(r"\s*", lines[last]).group(0)
        add = []
        for name, title, desc in new:
            for key, value in (("title", title), ("description", desc)):
                add.append('%s"advancements.opencomputers.%s.%s": %s,' % (
                    indent, file_name(name), key, json.dumps(value, ensure_ascii=False)))
        if not lines[last].rstrip().endswith(","):
            lines[last] += ","
            add[-1] = add[-1][:-1]
        lines[last + 1:last + 1] = add
        out = "\n".join(lines)
        json.loads(out)  # still valid
        with open(path, "w", encoding="utf-8") as f:
            f.write(out)


if __name__ == "__main__":
    main()
