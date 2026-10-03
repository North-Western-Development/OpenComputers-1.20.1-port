#!/usr/bin/env python3
"""Generates computercraft.txt (CC: Tweaked integration test, see ../README.md).

Layout (y=-60):
  19,0  OC disk drive with CC floppy (DiskId 7)      20,0  OC creative case (lancard, EEPROM below)
  19,1  CC monitor     20,1 OC adapter                21,0  OC relay
  20,2  CC disk drive with floppy 7 (seen through the adapter)
  22,0  CC computer #1 (boots disk/startup.lua)       22,1  CC disk drive with floppy 7
  22,-59,0 redstone lamp, lit by the CC computer after it got OC's reply through the relay.

The OC EEPROM (a) calls the CC monitor and disk drive through the adapter, (c) writes the CC
program to the CC floppy in the OC disk drive, then (b) waits for the CC computer's message sent
through the relay (CC sees the relay as a "modem" peripheral), replies, and reports everything
via error("RESULT ...").
"""
import os

CC_PROG = r'''local function log(s) local f=fs.open("disk/cclog.txt","a") f.writeLine(s) f.close() end
log("names="..table.concat(peripheral.getNames(),","))
local ok,err=pcall(function()
local m=peripheral.find("modem")
m.open(98)
local types={}
for _,n in ipairs(m.getNamesRemote()) do types[#types+1]=m.getTypeRemote(n) end
table.sort(types)
m.transmit(99,98,"hi:"..peripheral.getName(m)..":ap="..tostring(m.isAccessPoint())..":remote="..table.concat(types,","))
local t=os.startTimer(30)
while true do
 local e={os.pullEvent()}
 if e[1]=="modem_message" then
  log("ack="..tostring(e[3]).."/"..tostring(e[4]).."/"..tostring(e[5]))
  redstone.setOutput("top",true)
  break
 elseif e[1]=="timer" and e[2]==t then log("timeout") break end
end
end)
if not ok then log("err="..tostring(err)) end
'''

OC_PROG = r'''local o={}
local function add(s) o[#o+1]=tostring(s) end
local function try(n,f) local ok,e=pcall(f) if not ok then add(n.."Err="..tostring(e)) end end
try("mon",function()
 local m=component.proxy(component.list("monitor")())
 local w,h=m.getSize() m.write("OC")
 add("mon="..w.."x"..h..",scale="..tostring(m.getTextScale()))
end)
try("drive",function()
 local d=component.proxy(component.list("drive",true)())
 d.setDiskLabel("occc")
 add("drive="..tostring(d.isDiskPresent())..",label="..tostring((d.getDiskLabel()))..",id="..tostring((d.getDiskID()))..",mount="..tostring((d.getMountPath())))
end)
local fs
try("floppy",function()
 local a=component.proxy(component.list("disk_drive")()).media()
 fs=component.proxy(a)
 local h=fs.open("startup.lua","w") fs.write(h,[==[CCPROG]==]) fs.close(h)
 h=fs.open("startup.lua","r") local s=fs.read(h,4096) fs.close(h)
 add("floppy=ro:"..tostring(fs.isReadOnly())..",total="..fs.spaceTotal()..",used="..fs.spaceUsed()..",read="..#s..",list="..table.concat(fs.list("/"),"|"))
 local n=0
 for b in component.list("filesystem") do if b~=a and b~=computer.tmpAddress() and component.invoke(b,"exists","startup.lua") then n=n+1 end end
 add("ccDriveMounts="..n)
end)
try("modem",function()
 local m=component.proxy(component.list("modem")())
 m.open(99)
 local d=computer.uptime()+45 local g
 while computer.uptime()<d do
  local s=table.pack(computer.pullSignal(d-computer.uptime()))
  if s[1]=="modem_message" and s[4]==99 then g=s break end
 end
 if not g then add("ccmsg=none") return end
 add("ccmsg="..tostring(g[6])..",from="..tostring(g[3])..",reply="..tostring(g[7]))
 m.send(g[3],98,"pong")
 local t=computer.uptime()+3
 while computer.uptime()<t do computer.pullSignal(t-computer.uptime()) end
end)
if fs and fs.exists("cclog.txt") then local h=fs.open("cclog.txt","r") add((("cclog="..tostring(fs.read(h,300))):gsub("\n",";"))) fs.close(h) end
error("RESULT "..table.concat(o," "))
'''


def eeprom(code: str) -> str:
    data = code.encode("utf-8")
    assert len(data) <= 4096, len(data)
    return '{"oc:data":{"oc:eeprom":[B;' + ",".join(f"{b}B" for b in data) + "]}}"


code = OC_PROG.replace("CCPROG", CC_PROG)
cmds = f"""forceload add 0 0 32 16
setblock 19 -60 0 opencomputers:diskdrive
item replace block 19 -60 0 container.0 with computercraft:disk{{DiskId:7}}
setblock 20 -60 0 opencomputers:casecreative
item replace block 20 -60 0 container.0 with opencomputers:lancard
item replace block 20 -60 0 container.3 with opencomputers:ram6
item replace block 20 -60 0 container.8 with opencomputers:cpu3
item replace block 20 -60 0 container.9 with opencomputers:eeprom{eeprom(code)}
setblock 20 -60 1 opencomputers:adapter
setblock 19 -60 1 computercraft:monitor_normal
setblock 20 -60 2 computercraft:disk_drive{{Item:{{id:"computercraft:disk",Count:1b,tag:{{DiskId:7}}}}}}
setblock 21 -60 0 opencomputers:relay
setblock 22 -60 0 computercraft:computer_normal{{ComputerId:1}}
setblock 22 -60 1 computercraft:disk_drive{{Item:{{id:"computercraft:disk",Count:1b,tag:{{DiskId:7}}}}}}
setblock 22 -59 0 minecraft:redstone_lamp
WAIT
oc_debug start 20 -60 0
WAIT
computercraft turn-on #1
WAIT
WAIT
WAIT
WAIT
WAIT
WAIT
oc_debug status 20 -60 0
computercraft dump
execute if block 22 -59 0 minecraft:redstone_lamp[lit=true] run say LAMP-LIT
execute unless block 22 -59 0 minecraft:redstone_lamp[lit=true] run say LAMP-OFF
"""
with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "computercraft.txt"), "w") as f:
    f.write(cmds)
