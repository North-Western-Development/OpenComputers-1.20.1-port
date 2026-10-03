-- Server blade in a rack: lists its components and reports energy.
local t = {}
for _, n in component.list() do t[#t + 1] = n end
table.sort(t)
error("RESULT server comps=" .. table.concat(t, ",") .. " mem=" .. computer.totalMemory() .. " energy=" .. tostring(computer.energy() > 0))
