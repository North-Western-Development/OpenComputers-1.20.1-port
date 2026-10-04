-- Permission checks for inventories in the world: in front (side 3) a chest minecart tagged
-- oc_protected (protection hooks deny entity interaction), above (side 1) an unprotected chest
-- minecart, below (side 0) a chest protected with /oc_debug protect.
local r = component.proxy(component.list("robot")())
local ic = component.proxy(component.list("inventory_controller")())
local o = {}
local function add(k, f, ...)
  local res = table.pack(pcall(f, ...))
  for i = 1, res.n do res[i] = type(res[i]) == "table" and (res[i].name or "table") .. "x" .. tostring(res[i].size) or tostring(res[i]) end
  o[#o + 1] = k .. "=" .. table.concat(res, ",", 2)
end
-- Unprotected entity inventory (gives the robot one diamond).
add("uSize", ic.getInventorySize, 1)
add("uName", ic.getInventoryName, 1)
add("uStack", ic.getStackInSlot, 1, 1)
add("uSuck", r.suck, 1, 1)
add("count", r.count, 1)
-- Protected entity inventory.
add("pSize", ic.getInventorySize, 3)
add("pName", ic.getInventoryName, 3)
add("pStack", ic.getStackInSlot, 3, 1)
add("pSuckSlot", ic.suckFromSlot, 3, 1, 1)
add("pSuck", r.suck, 3, 1)
add("pDropSlot", ic.dropIntoSlot, 3, 2, 1)
-- Protected block inventory.
add("bSize", ic.getInventorySize, 0)
add("bSuck", r.suck, 0, 1)
add("count2", r.count, 1)
error("RESULT " .. table.concat(o, " "))
