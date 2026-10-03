-- Robot upgrades: inventory controller (chest in front), crafting, generator, piston, tank,
-- experience, sign.
local r = component.proxy(component.list("robot")())
local function C(n) local a = component.list(n)() return a and component.proxy(a) or {} end
local ic = C("inventory_controller")
local o = {}
local function add(k, f, ...)
  if not f then o[#o + 1] = k .. "=missing" return end
  local res = table.pack(pcall(f, ...))
  for i = 1, res.n do res[i] = type(res[i]) == "table" and (res[i].name or "table") .. "x" .. tostring(res[i].size) or tostring(res[i]) end
  o[#o + 1] = k .. "=" .. table.concat(res, ",", 2)
end
add("icSize", ic.getInventorySize, 3)
add("icStack", ic.getStackInSlot, 3, 1)
add("icSuck", ic.suckFromSlot, 3, 1, 1)
add("icInternal", ic.getStackInInternalSlot, 1)
add("craft", C("crafting").craft, 4)
add("planks", ic.getStackInInternalSlot, 1)
add("sel2", r.select, 2)
add("icSuck2", ic.suckFromSlot, 3, 2, 4)
add("genInsert", C("generator").insert, 2)
add("genCount", C("generator").count)
add("turn", r.turn, false)
add("push", C("piston").push, 3)
add("tanks", r.tankCount)
add("xp", C("experience").level)
add("equip", ic.equip)
local t = {} for _, n in component.list() do t[#t + 1] = n end table.sort(t)
error("RESULT " .. table.concat(o, " ") .. " comps=" .. table.concat(t, ","))
