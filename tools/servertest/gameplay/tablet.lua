-- Tablet: draws on its screen and idles.
local gpu = component.proxy(component.list("gpu")())
gpu.bind((component.list("screen")()))
gpu.fill(1, 1, 50, 16, " ")
gpu.set(2, 2, "Tablet OK")
local t = {}
for _, n in component.list() do t[#t + 1] = n end
table.sort(t)
gpu.set(2, 4, table.concat(t, ","))
while true do computer.pullSignal() end
