-- Navigation, sign and tractor beam upgrades on a robot facing south (sign in front of it,
-- waypoint "home" nearby, items on the ground). The navigation upgrade holds map #0, which the
-- robot creates first by using the empty map in its tool slot.
local r = component.proxy(component.list("robot")())
local function C(n) local a = component.list(n)() return a and component.proxy(a) or {} end
local nav, sign, tb, ic = C("navigation"), C("sign"), C("tractor_beam"), C("inventory_controller")
local o = {}
local function str(v)
  if type(v) ~= "table" then return (tostring(v):gsub("\n", "|")) end
  local t = {}
  for k, x in pairs(v) do t[#t + 1] = tostring(k) .. ":" .. str(x) end
  table.sort(t)
  return "{" .. table.concat(t, ";") .. "}"
end
local function add(k, f, ...)
  if not f then o[#o + 1] = k .. "=missing" return end
  local res = table.pack(pcall(f, ...))
  for i = 1, res.n do res[i] = str(res[i]) end
  o[#o + 1] = k .. "=" .. table.concat(res, ",", 2)
end
add("posNoMap", nav.getPosition)
add("makeMap", r.use, 1) -- use the empty map in the tool slot (on the air above)
add("pos", nav.getPosition)
add("facing", nav.getFacing)
add("range", nav.getRange)
add("wp", nav.findWaypoints, 16)
add("signGet", sign.getValue, 3)
add("signSet", sign.setValue, "robot\nwas here", 3)
add("signGet2", sign.getValue, 3)
r.select(4)
add("suck", tb.suck)
add("suck2", tb.suck)
add("count4", r.count, 4)
add("item4", function() local s = ic.getStackInInternalSlot(4) return s and s.name .. "x" .. s.size end)
error("RESULT " .. table.concat(o, " "))
