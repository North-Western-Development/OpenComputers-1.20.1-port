-- Persistence test: counts in a loop, writing the counter to the HDD and the tmpfs. The server
-- is restarted while this runs; with working persistence the Lua state (locals, a coroutine,
-- the program token) survives and the program finishes with starts=1.
local fs, tmp
for a in component.list("filesystem") do
  if a == computer.tmpAddress() then tmp = component.proxy(a)
  elseif not component.invoke(a, "isReadOnly") then fs = component.proxy(a) end
end
local function rd(f, p) if not f.exists(p) then return "" end local h = f.open(p, "r") local s = f.read(h, 10000) or "" f.close(h) return s end
local function wr(f, p, s, m) local h = f.open(p, m or "w") f.write(h, s) f.close(h) end
local tok = tostring(math.random(1, 1000000000))
local ee = component.proxy(component.list("eeprom")())
ee.setData(tok)
fs.setLabel("ptest")
wr(fs, "starts", tok .. "\n", "a")
local co = coroutine.wrap(function() local i = 0 while true do i = i + 1 coroutine.yield(i) end end)
local n, c = 0, 0
while n < 80 do
  n = n + 1
  c = co()
  wr(fs, "counter", tostring(n))
  wr(tmp, "counter", tostring(n))
  computer.pullSignal(0.5)
end
local _, starts = rd(fs, "starts"):gsub("\n", "")
error("RESULT n=" .. n .. " co=" .. c .. " hdd=" .. rd(fs, "counter") .. " tmp=" .. rd(tmp, "counter") ..
  " starts=" .. starts .. " eeprom=" .. tostring(ee.getData() == tok) .. " label=" .. tostring(fs.getLabel()))
