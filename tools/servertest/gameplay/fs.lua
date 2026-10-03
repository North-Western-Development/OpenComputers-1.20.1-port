-- Filesystem test: HDD in the case, floppy in an adjacent disk drive, RAID with 3 HDDs, tmpfs.
local o = {}
local function add(s) o[#o + 1] = s end
local function test(fs)
  local r = {}
  local function ok(n, v) r[#r + 1] = n .. "=" .. tostring(v) end
  ok("mk", fs.makeDirectory("d/e"))
  local h = fs.open("d/e/f.txt", "w")
  fs.write(h, "hello ") fs.write(h, "world") fs.close(h)
  h = fs.open("d/e/f.txt", "a") fs.write(h, "!") fs.close(h)
  h = fs.open("d/e/f.txt", "r")
  fs.seek(h, "set", 6)
  ok("rd", fs.read(h, 100)) fs.close(h)
  ok("sz", fs.size("d/e/f.txt"))
  ok("ls", table.concat(fs.list("d/e"), "|"))
  ok("dir", fs.isDirectory("d/e"))
  ok("mv", fs.rename("d/e/f.txt", "d/g.txt"))
  ok("ex", tostring(fs.exists("d/g.txt")) .. "/" .. tostring(fs.exists("d/e/f.txt")))
  ok("rm", fs.remove("d"))
  ok("ex2", fs.exists("d"))
  ok("lbl", fs.setLabel and select(2, pcall(fs.setLabel, "lbl")) or "-")
  ok("used", fs.spaceUsed() >= 0)
  ok("tot", fs.spaceTotal())
  ok("ro", fs.isReadOnly())
  return table.concat(r, ",")
end
local dd = component.list("disk_drive")()
add("drive=" .. tostring(dd and not component.invoke(dd, "isEmpty")))
local floppy = dd and component.invoke(dd, "media")
local n = 0
for a in component.list("filesystem") do
  n = n + 1
  local fs = component.proxy(a)
  local kind = a == computer.tmpAddress() and "tmp" or a == floppy and "floppy" or fs.spaceTotal() > 6000000 and "raid" or "hdd"
  local ok, res = pcall(test, fs)
  add(kind .. "[" .. tostring(res) .. "]")
end
add("fsCount=" .. n)
local ee = component.proxy(component.list("eeprom")())
ee.setData("data!")
ee.setLabel("FS test")
add("eeprom=" .. ee.getData() .. "," .. ee.getLabel() .. "," .. ee.getSize() .. "," .. #ee.get() .. "," .. ee.getChecksum())
error("RESULT " .. table.concat(o, " "))
