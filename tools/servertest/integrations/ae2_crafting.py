#!/usr/bin/env python3
"""Generates ae2_crafting.txt (server commands) from ae2_crafting.lua: an ME network with a crafting
CPU, a pattern provider holding an encoded crafting pattern (1 oak log -> 4 oak planks) next to a
molecular assembler, a cell with oak logs, and a cable bus with an import bus below a chest. The
computer's adapter touches the controller and the cable bus. Uses x = 10..14, so it can run in the
same world as ae2.txt."""
import os

here = os.path.dirname(os.path.abspath(__file__))
lua = open(os.path.join(here, "ae2_crafting.lua"), encoding="utf-8").read().encode("utf-8")
assert len(lua) <= 4096, "EEPROM code too large: %d" % len(lua)
eeprom = "[B;" + ",".join("%dB" % (b if b < 128 else b - 256) for b in lua) + "]"

item_cell = ('{id:"ae2:item_storage_cell_1k",Count:1b,tag:{'
             'keys:[{"#c":"ae2:i",id:"minecraft:oak_log"},{"#c":"ae2:i",id:"minecraft:dirt"}],'
             'amts:[L;64L,1L],ic:65L}}')
pattern = ('{Slot:0,id:"ae2:crafting_pattern",Count:1b,tag:{'
           'in:[{id:"minecraft:oak_log",Count:1b},{},{},{},{},{},{},{},{}],'
           'out:{id:"minecraft:oak_planks",Count:4b},'
           'substitute:0b,substituteFluids:0b,recipe:"minecraft:oak_planks"}}')
# The import bus starts with a filter that matches nothing in the chest, so it doesn't import
# anything before the computer configures it.
import_bus = '{id:"ae2:import_bus",config:[{"#c":"ae2:i",id:"minecraft:clay_ball","#":1L}]}'

commands = [
    "forceload add -16 -16 16 16",
    "setblock 12 -60 0 ae2:controller",
    "setblock 13 -60 0 ae2:creative_energy_cell",
    "setblock 12 -59 0 ae2:1k_crafting_storage",
    "setblock 12 -60 1 ae2:drive[facing=south]{inv:{item0:%s}}" % item_cell,
    "setblock 13 -59 0 ae2:pattern_provider{patterns:[%s]}" % pattern,
    "setblock 14 -59 0 ae2:molecular_assembler",
    # Import bus on top of a cable bus, below a chest with dirt and stone.
    'setblock 11 -59 0 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"},up:%s}' % import_bus,
    'setblock 11 -58 0 minecraft:chest{Items:[{Slot:0b,id:"minecraft:dirt",Count:16b},{Slot:1b,id:"minecraft:stone",Count:16b}]}',
    "setblock 10 -60 0 opencomputers:casecreative",
    "item replace block 10 -60 0 container.3 with opencomputers:ram6",
    "item replace block 10 -60 0 container.8 with opencomputers:cpu3",
    'item replace block 10 -60 0 container.9 with opencomputers:eeprom{"oc:data":{"oc:eeprom":%s}}' % eeprom,
    "setblock 11 -60 0 opencomputers:adapter",
    "item replace block 11 -60 0 container.0 with opencomputers:databaseupgrade1",
    "WAIT",
    "WAIT",
    "data get block 13 -59 0 patterns",
    "oc_debug start 10 -60 0",
    "WAIT 30",
    "oc_debug status 10 -60 0",
    "WAIT 30",
    "oc_debug status 10 -60 0",
    "data get block 11 -58 0 Items",
]
with open(os.path.join(here, "ae2_crafting.txt"), "w", encoding="utf-8") as f:
    f.write("\n".join(commands) + "\n")
