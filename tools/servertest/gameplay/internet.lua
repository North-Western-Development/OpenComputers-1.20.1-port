-- Internet card: HTTP(S) request to example.com.
local net = component.proxy(component.list("internet")())
local o = {}
local function add(k, f, ...) local r = table.pack(pcall(f, ...)) for i = 1, r.n do r[i] = tostring(r[i]) end o[#o + 1] = k .. "=" .. table.concat(r, ",", 2) end
add("http", net.isHttpEnabled)
add("tcp", net.isTcpEnabled)
add("get", function()
  local r = assert(net.request("https://example.com/"))
  local t = computer.uptime() + 15
  while not r.finishConnect() do
    if computer.uptime() > t then return "timeout" end
    computer.pullSignal(0.1)
  end
  local code = r.response()
  local body = ""
  while computer.uptime() < t do
    local chunk = r.read()
    if chunk == nil then break end
    body = body .. chunk
    if chunk == "" then computer.pullSignal(0.1) end
  end
  r.close()
  return tostring(code) .. ":" .. #body .. ":" .. tostring(body:find("Example Domain") ~= nil)
end)
error("RESULT " .. table.concat(o, " "))
