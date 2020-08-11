require "string"
local os = require 'os'

--[[
     This is the default method name for Lua handlers, see the optional
     function-name in the LuaMapHandler directive to choose a different
     entry point.
--]]
function handle(r)
    r.content_type = "text/plain"
    r:puts("Cache-Control: Private at " .. os.date() .. "\n")
    r.headers_out["Cache-Control"] = 'Private'
    return apache2.OK
end