#!/usr/bin/env python3
"""Generates ae2.txt (server commands) from ae2.lua: a small ME network plus a computer whose
EEPROM runs ae2.lua through adapters next to the controller, an ME interface and a cable bus with
an interface part and an export bus."""
import os

here = os.path.dirname(os.path.abspath(__file__))
lua = open(os.path.join(here, "ae2.lua"), encoding="utf-8").read().encode("utf-8")
eeprom = "[B;" + ",".join("%dB" % (b if b < 128 else b - 256) for b in lua) + "]"

item_cell = ('{id:"ae2:item_storage_cell_1k",Count:1b,tag:{'
             'keys:[{"#c":"ae2:i",id:"minecraft:cobblestone"},{"#c":"ae2:i",id:"minecraft:diamond"},'
             '{"#c":"ae2:i",id:"ae2:item_storage_cell_1k"}],amts:[L;64L,10L,1L],ic:75L}}')
# 81000 units: 81 buckets on Forge (mB), 1 bucket on Fabric (droplets).
fluid_cell = ('{id:"ae2:fluid_storage_cell_1k",Count:1b,tag:{'
              'keys:[{"#c":"ae2:f",id:"minecraft:water"}],amts:[L;81000L],ic:81000L}}')

commands = [
    "forceload add -16 -16 16 16",
    # ME network: controller + creative cell + crafting storage (1 CPU) + drive + interface + cable bus.
    "setblock 2 -60 0 ae2:controller",
    "setblock 3 -60 0 ae2:creative_energy_cell",
    "setblock 2 -59 0 ae2:1k_crafting_storage",
    # Drives don't connect on their front face; face it away from the controller.
    "setblock 2 -60 1 ae2:drive[facing=south]{inv:{item0:%s,item1:%s}}" % (item_cell, fluid_cell),
    'setblock 2 -60 -1 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"}}',
    "setblock 2 -60 -2 ae2:interface",
    'setblock 2 -60 -3 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"}}',
    'setblock 2 -60 -4 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"},west:{id:"ae2:cable_interface"},north:{id:"ae2:export_bus"}}',
    "setblock 2 -60 -5 minecraft:chest",
    # OC side: computer, adapter (with database) at the controller, adapters at interface / cable bus.
    "setblock 0 -60 0 opencomputers:casecreative",
    "item replace block 0 -60 0 container.3 with opencomputers:ram6",
    "item replace block 0 -60 0 container.8 with opencomputers:cpu3",
    'item replace block 0 -60 0 container.9 with opencomputers:eeprom{"oc:data":{"oc:eeprom":%s}}' % eeprom,
    "setblock 1 -60 0 opencomputers:adapter",
    "item replace block 1 -60 0 container.0 with opencomputers:databaseupgrade1",
    # Adapters must not touch each other (an adapter is a Container, so adjacent adapters drive
    # each other and recurse); connect them through OC cables instead.
    "fill 0 -60 -1 0 -60 -4 opencomputers:cable",
    "setblock 1 -60 -2 opencomputers:adapter",
    "setblock 1 -60 -4 opencomputers:adapter",
    # A hopper feeding the interface, which passes items on into network storage.
    'setblock 2 -59 -2 minecraft:hopper[facing=down]{Items:[{Slot:0b,id:"minecraft:iron_ingot",Count:16b}]}',
    "WAIT",
    "WAIT",
    "data get block 2 -60 1 inv",
    "oc_debug start 0 -60 0",
    "WAIT",
    "WAIT",
    "oc_debug status 0 -60 0",
    "data get block 2 -60 -5 Items",
]
with open(os.path.join(here, "ae2.txt"), "w", encoding="utf-8") as f:
    f.write("\n".join(commands) + "\n")
