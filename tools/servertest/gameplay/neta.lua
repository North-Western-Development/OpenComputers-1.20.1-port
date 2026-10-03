-- Network test, sending side: wired broadcast through a relay, wireless broadcast, linked card.
local lan, wlan
for a in component.list("modem") do if component.invoke(a, "isWireless") then wlan = component.proxy(a) else lan = component.proxy(a) end end
local tun = component.proxy(component.list("tunnel")())
local got = {}
local function sleep(s)
  local t = computer.uptime() + s
  while computer.uptime() < t do
    local m = table.pack(computer.pullSignal(t - computer.uptime()))
    if m[1] == "modem_message" then
      local card = m[2] == lan.address and "lan" or m[2] == wlan.address and "wlan" or m[2] == tun.address and "tun" or "?"
      got[#got + 1] = card .. ":" .. m[4] .. ":" .. math.floor(m[5]) .. ":" .. tostring(m[6])
    end
  end
end
sleep(3)
lan.open(1)
wlan.open(2)
wlan.setStrength(64)
lan.broadcast(1, "wired")
sleep(1)
wlan.broadcast(2, "air")
sleep(1)
tun.send("linked")
sleep(5)
table.sort(got)
error("RESULT A wireless=" .. tostring(wlan.isWireless()) .. " channel=" .. tun.getChannel() .. " " .. table.concat(got, " "))
