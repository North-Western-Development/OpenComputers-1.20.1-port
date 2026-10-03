-- Redstone I/O block east of the computer: redstone block on top (input), lamp east (output).
local rs
for a in component.list("redstone") do rs = component.proxy(a) end
local o = {}
local function add(k, f, ...) local r = table.pack(pcall(f, ...)) for i = 1, r.n do r[i] = tostring(r[i]) end o[#o + 1] = k .. "=" .. table.concat(r, ",", 2) end
add("in", rs.getInput, 1)
add("set", rs.setOutput, 5, 15)
add("out", rs.getOutput, 5)
add("cmpIn", rs.getComparatorInput, 1)
add("bundled", rs.getBundledOutput, 5, 0)
add("wake", rs.getWakeThreshold)
error("RESULT " .. table.concat(o, " "))
