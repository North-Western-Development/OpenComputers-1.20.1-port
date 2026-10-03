#!/usr/bin/env python3
"""Generates <name>.txt server command files from the <name>.cmds templates in this directory.

In a template, @EEPROM(file.lua) is replaced by the NBT of an EEPROM item holding that Lua file
(so `item replace ... with opencomputers:eeprom@EEPROM(x.lua)` works), @BIOS by the NBT of an
EEPROM holding the Lua BIOS (boots the first bootable filesystem, e.g. the OpenOS floppy) and
@BYTES(file.lua) by a bare NBT byte array of the file. Lines starting with # are dropped.
"""
import glob
import os
import re

here = os.path.dirname(os.path.abspath(__file__))
bios = os.path.join(here, "..", "..", "..", "common", "src", "main", "resources", "assets",
                    "opencomputers", "lua", "bios.lua")


def byte_array(data: bytes) -> str:
    return "[B;" + ",".join("%dB" % (b if b < 128 else b - 256) for b in data) + "]"


def eeprom(data: bytes) -> str:
    assert len(data) <= 4096, len(data)
    return '{"oc:data":{"oc:eeprom":' + byte_array(data) + "}}"


def read(name: str) -> bytes:
    with open(os.path.join(here, name), "rb") as f:
        return f.read()


for template in sorted(glob.glob(os.path.join(here, "*.cmds"))):
    with open(template, encoding="utf-8") as f:
        text = f.read()
    text = re.sub(r"@EEPROM\(([^)]+)\)", lambda m: eeprom(read(m.group(1))), text)
    text = re.sub(r"@BYTES\(([^)]+)\)", lambda m: byte_array(read(m.group(1))), text)
    if "@BIOS" in text:
        with open(bios, "rb") as f:
            text = text.replace("@BIOS", eeprom(f.read()))
    text = "\n".join(l for l in text.split("\n") if l.strip() and not l.startswith("#")) + "\n"
    with open(template[:-5] + ".txt", "w", encoding="utf-8") as f:
        f.write(text)
