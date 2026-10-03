#!/usr/bin/env python3
"""Generates the TIS-3D / Mekanism / EnderStorage / ProjectRed integration tests (see ../README.md).

Each test builds a creative OC computer whose EEPROM reports via error("RESULT ...").

tis3d.txt (Forge + Fabric), y=-60:
  28,5 powered lever (strong power) -> 29,5 TIS-3D controller -> 30,5 casing with two serial port modules,
  facing 31,5 adapter #1 (east face) and 30,4 adapter #2 (north face); 31,4 OC computer.
  The computer writes a value to one adapter's serial_port, which TIS-3D moves through the
  casing to the other serial port module / adapter, where the computer reads it (and back).
mekanism.txt (Forge): 50,5 basic chemical tank with 1234 mB hydrogen, 51,5 adapter, 52,5 computer.
enderstorage.txt (Forge): 60,5 ender chest, 61,5 adapter, 62,5 computer; reads/sets frequency.
projectred.txt (Forge): 70,5 OC redstone I/O <- 71,5 ProjectRed bundled cable (multipart) -> 72,5
  redstone I/O; 71,4 computer linked to both I/O blocks by OC cables at 70,4 and 72,4. The
  computer emits a bundled signal from one I/O block and reads it at the other through the cable.
  (connMap 0xF000 marks the wire's four sides as open, which a placed wire computes itself.)
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))

PRELUDE = r'''local o={}
local function add(s) o[#o+1]=tostring(s) end
local function sleep(t) local d=computer.uptime()+t while computer.uptime()<d do computer.pullSignal(d-computer.uptime()) end end
local function try(n,f) local ok,e=pcall(f) if not ok then add(n.."Err="..tostring(e)) end end
'''
EPILOGUE = '\nerror("RESULT "..table.concat(o," "))\n'

TIS3D = r'''try("tis",function()
 local sp={}
 local d=computer.uptime()+30
 while computer.uptime()<d do
  sp={} for a in component.list("serial_port") do sp[#sp+1]=a end
  if #sp>=2 then break end
  sleep(0.5)
 end
 add("ports="..#sp)
 if #sp<2 then return end
 local a,b=component.proxy(sp[1]),component.proxy(sp[2])
 a.setReading(true) b.setReading(true)
 add("write="..tostring(a.write(42)))
 local function await(p) local g d=computer.uptime()+10 while computer.uptime()<d and g==nil do g=p.read() if g==nil then sleep(0.2) end end return g end
 add("a->b="..tostring(await(b)))
 b.write(-7)
 add("b->a="..tostring(await(a)))
end)
'''

MEKANISM = r'''try("mek",function()
 local c
 for a,n in component.list() do if component.methods(a).getChemicalTanks~=nil then c=component.proxy(a) add("name="..n) end end
 for i,t in ipairs(c.getChemicalTanks()) do
  add("tank"..i.."="..tostring(t.type)..","..tostring(t.name)..","..tostring(t.label)..","..tostring(t.amount).."/"..tostring(t.capacity))
 end
end)
'''

ENDERSTORAGE = r'''try("ender",function()
 local c=component.proxy(component.list("ender_chest")())
 local f=c.getFrequency() add("freq0="..table.concat(f,","))
 c.setFrequency(1,2,3)
 f=c.getFrequency() add("freq1="..table.concat(f,","))
 add("colors="..table.concat(c.getFrequencyColors(),","))
 c.setFrequency(0x4AF)
 f=c.getFrequency() add("freq2="..table.concat(f,","))
 add("owner="..tostring(c.getOwner()))
 add("c14="..tostring(c.getColors()[14]))
 add("bad="..tostring(pcall(c.setFrequency,16,0,0)))
end)
'''

PROJECTRED = r'''try("pr",function()
 local rs={} for a in component.list("redstone") do rs[#rs+1]=component.proxy(a) end
 add("n="..#rs)
 local function inp(r) return math.max(r.getBundledInput(4,1),r.getBundledInput(5,1)) end
 add("before="..inp(rs[2]))
 rs[1].setBundledOutput(4,1,200) rs[1].setBundledOutput(5,1,200)
 sleep(1)
 add("on="..inp(rs[2]))
 rs[1].setBundledOutput(4,1,0) rs[1].setBundledOutput(5,1,0)
 sleep(1)
 add("off="..inp(rs[2]))
 rs[2].setBundledOutput(4,14,255) rs[2].setBundledOutput(5,14,255)
 sleep(1)
 add("back="..math.max(rs[1].getBundledInput(4,14),rs[1].getBundledInput(5,14)))
end)
'''


def eeprom(code: str) -> str:
    data = code.encode("utf-8")
    assert len(data) <= 4096, len(data)
    return '{"oc:data":{"oc:eeprom":[B;' + ",".join(f"{b}B" for b in data) + "]}}"


def computer(x, z, body, card=None):
    lines = [f"setblock {x} -60 {z} opencomputers:casecreative"]
    if card:
        lines.append(f"item replace block {x} -60 {z} container.0 with opencomputers:{card}")
    lines += [
        f"item replace block {x} -60 {z} container.3 with opencomputers:ram6",
        f"item replace block {x} -60 {z} container.8 with opencomputers:cpu3",
        f"item replace block {x} -60 {z} container.9 with opencomputers:eeprom{eeprom(PRELUDE + body + EPILOGUE)}",
    ]
    return "\n".join(lines)


def write(name, setup, x, z, waits=4, after=""):
    cmds = "forceload add 0 0 96 16\n" + setup.strip() + "\nWAIT\n" + f"oc_debug start {x} -60 {z}\n" \
           + "WAIT\n" * waits + f"oc_debug status {x} -60 {z}\n" + after
    with open(os.path.join(HERE, name), "w") as f:
        f.write(cmds)


write("tis3d.txt", f"""
setblock 30 -60 5 tis3d:casing
setblock 29 -60 5 tis3d:controller
setblock 31 -60 5 opencomputers:adapter
setblock 30 -60 4 opencomputers:adapter
{computer(31, 4, TIS3D)}
item replace block 30 -60 5 container.5 with tis3d:serial_port_module
item replace block 30 -60 5 container.2 with tis3d:serial_port_module
setblock 28 -60 5 minecraft:lever[face=wall,facing=west,powered=true]
""", 31, 4, waits=5)

write("mekanism.txt", f"""
setblock 50 -60 5 mekanism:basic_chemical_tank{{GasTanks:[{{Tank:0b,stored:{{gasName:"mekanism:hydrogen",amount:1234L}}}}]}}
setblock 51 -60 5 opencomputers:adapter
{computer(52, 5, MEKANISM)}
""", 52, 5, waits=2)

write("enderstorage.txt", f"""
setblock 60 -60 5 enderstorage:ender_chest
setblock 61 -60 5 opencomputers:adapter
{computer(62, 5, ENDERSTORAGE)}
""", 62, 5, waits=2, after="data get block 60 -60 5 Frequency\n")

write("projectred.txt", f"""
setblock 71 -60 5 cb_multipart:multipart{{parts:[{{id:"projectred_transmission:neutral_bundled_wire",side:0b,connMap:61440,signal:[B;0B,0B,0B,0B,0B,0B,0B,0B,0B,0B,0B,0B,0B,0B,0B,0B]}}]}}
setblock 70 -60 5 opencomputers:redstone
setblock 72 -60 5 opencomputers:redstone
setblock 70 -60 4 opencomputers:cable
setblock 72 -60 4 opencomputers:cable
{computer(71, 4, PROJECTRED)}
""", 71, 4, waits=3, after="data get block 71 -60 5 parts\n")
