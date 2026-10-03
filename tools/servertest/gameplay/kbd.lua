-- Keyboard -> screen: echoes typed characters on the screen until Enter, then reports them.
local gpu = component.proxy(component.list("gpu")())
gpu.bind((component.list("screen")()))
gpu.setResolution(40, 10)
gpu.fill(1, 1, 40, 10, " ")
gpu.set(1, 1, "OC keyboard test, type + Enter:")
local s, clip = "", ""
while true do
  local e = table.pack(computer.pullSignal())
  if e[1] == "key_down" then
    if e[4] == 28 then break end
    if e[3] >= 32 and e[3] < 127 then s = s .. string.char(e[3]) end
    gpu.set(1, 3, "> " .. s)
  elseif e[1] == "clipboard" then clip = clip .. e[3]
  elseif e[1] == "touch" then gpu.set(1, 5, string.format("touch %d,%d", e[3], e[4]))
  end
end
gpu.set(1, 7, "RESULT typed=" .. s)
error("RESULT typed=" .. s .. " keyboards=" .. #component.invoke(component.list("screen")(), "getKeyboards"))
