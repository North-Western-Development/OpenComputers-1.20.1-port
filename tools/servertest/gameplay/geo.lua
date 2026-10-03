-- Geolyzer (east of the computer, on grass): column scan, analyze, detect.
local g = component.proxy(component.list("geolyzer")())
local o = {}
local function add(k, f, ...) local r = table.pack(pcall(f, ...)) for i = 1, r.n do r[i] = tostring(r[i]) end o[#o + 1] = k .. "=" .. table.concat(r, ",", 2) end
add("scan", function()
  local s = g.scan(0, 0)
  -- index 33 is the geolyzer's own layer, 32 the block below it (grass), 30 two below (dirt), 29 bedrock-ish
  return #s .. ":" .. (s[32] > 0 and "solid" or "air") .. ":" .. (s[34] == 0 and "air" or "solid")
end)
add("analyze", function() local a = g.analyze(0) return a.name .. ":" .. tostring(a.hardness) end)
add("detect", g.detect, 0)
add("detectUp", g.detect, 1)
add("sky", g.canSeeSky)
error("RESULT " .. table.concat(o, " "))
