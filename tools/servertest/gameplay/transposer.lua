-- Transposer: chest above it (side 1, 10 cobblestone in slot 1) -> chest east of it (side 5).
local t = component.proxy(component.list("transposer")())
local o = {}
local function add(k, f, ...) local r = table.pack(pcall(f, ...)) for i = 1, r.n do r[i] = tostring(r[i]) end o[#o + 1] = k .. "=" .. table.concat(r, ",", 2) end
add("size", t.getInventorySize, 1)
add("name", t.getInventoryName, 1)
add("cnt", t.getSlotStackSize, 1, 1)
add("item", function() local s = t.getStackInSlot(1, 1) return s.name .. "x" .. s.size end)
add("move", t.transferItem, 1, 5, 4)
add("cnt2", t.getSlotStackSize, 1, 1)
add("dst", t.getSlotStackSize, 5, 1)
add("cmp", t.compareStacks, 1, 1, 1)
error("RESULT " .. table.concat(o, " "))
