-- AE2 crafting + import bus test (see ae2_crafting.py). Reports through error("RESULT ...").
local c, computer = component, computer
local out, fails = {}, 0
local function add(s) out[#out + 1] = tostring(s) end
local function step(name, f)
  local ok, err = pcall(f)
  if not ok then fails = fails + 1; add(name .. ":FAIL(" .. tostring(err) .. ")") end
end
local function sleep(t)
  local d = computer.uptime() + t
  repeat computer.pullSignal(d - computer.uptime()) until computer.uptime() >= d
end
local function count(name)
  local s = me.getItemsInNetwork({name = name})[1]
  return s and s.size or 0
end
local db = c.list("database")()
me = c.proxy(c.list("me_controller")())
local bus = c.proxy(c.list("me_importbus")())
local craft
step("craftables", function()
  local list = me.getCraftables()
  add("craftables=" .. #list)
  for _, cr in ipairs(list) do
    local st = cr.getItemStack()
    if st.name == "minecraft:oak_planks" then craft = cr end
  end
  add("planksCraftable=" .. tostring(craft ~= nil))
  local f = me.getCraftables({name = "minecraft:oak_planks"})
  add("filtered=" .. #f)
  add("logs=" .. count("minecraft:oak_log") .. " planks=" .. count("minecraft:oak_planks"))
end)
step("request", function()
  local st = craft.request(8)
  local d, why = st.isDone()
  add("first=" .. tostring(d) .. "," .. tostring(why))
  local busy
  for _ = 1, 150 do
    d, why = st.isDone()
    if d == true then break end
    if d == false and busy == nil then
      local cpu = me.getCpus()[1]
      busy = tostring(cpu.busy) .. ":" .. tostring(cpu.crafting and cpu.crafting.name)
    end
    sleep(0.2)
  end
  add("cpuWhileCrafting=" .. tostring(busy))
  add("done=" .. tostring(d) .. "," .. tostring(why) .. " canceled=" .. tostring(st.isCanceled()))
  add("cancelDone=" .. tostring(st.cancel()))
  add("after logs=" .. count("minecraft:oak_log") .. " planks=" .. count("minecraft:oak_planks"))
  add("requesting=" .. tostring(craft.requesting()))
end)
step("missing", function()
  local st = craft.request(1000)
  local d, why
  for _ = 1, 50 do
    d, why = st.isDone()
    if d ~= nil then break end
    sleep(0.1)
  end
  add("missing=" .. tostring(d) .. "," .. tostring(why))
end)
step("importbus", function()
  local c0 = bus.getImportConfiguration(1)
  add("cfg0=" .. tostring(c0 and c0.name))
  add("store=" .. tostring(me.store({name = "minecraft:dirt"}, db)))
  add("set=" .. tostring(bus.setImportConfiguration(1, db, 1)))
  local cfg = bus.getImportConfiguration(1)
  add("cfg=" .. tostring(cfg and cfg.name))
  add("cfg2=" .. tostring(bus.getImportConfiguration(1, 2)))
  local dirt0 = count("minecraft:dirt")
  sleep(6)
  add("dirt=" .. dirt0 .. "->" .. count("minecraft:dirt") .. " stone=" .. count("minecraft:stone"))
  add("clear=" .. tostring(bus.setImportConfiguration(1, 1)) .. ":" .. tostring(bus.getImportConfiguration(1)))
end)
error("RESULT " .. (fails == 0 and "OK " or ("FAILS=" .. fails .. " ")) .. table.concat(out, " "))
