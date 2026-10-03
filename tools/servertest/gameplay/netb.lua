-- Network test, receiving side: answers every message on the card it came in through.
local lan = component.proxy(component.list("modem")())
local wlan, tun
for a in component.list("modem") do if component.invoke(a, "isWireless") then wlan = component.proxy(a) else lan = component.proxy(a) end end
tun = component.proxy(component.list("tunnel")())
lan.open(1)
wlan.open(2)
wlan.setStrength(64)
local got = {}
local t = computer.uptime() + 25
while computer.uptime() < t do
  local s = table.pack(computer.pullSignal(t - computer.uptime()))
  if s[1] == "modem_message" then
    local card = s[2] == lan.address and "lan" or s[2] == wlan.address and "wlan" or s[2] == tun.address and "tun" or "?"
    got[#got + 1] = card .. ":" .. s[4] .. ":" .. math.floor(s[5]) .. ":" .. tostring(s[6])
    if card == "tun" then tun.send("re:" .. tostring(s[6]))
    else component.invoke(s[2], "send", s[3], s[4], "re:" .. tostring(s[6])) end
  end
end
table.sort(got)
error("RESULT B " .. table.concat(got, " "))
