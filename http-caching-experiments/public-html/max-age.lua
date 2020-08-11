require "string"

--[[
     This is the default method name for Lua handlers, see the optional
     function-name in the LuaMapHandler directive to choose a different
     entry point.
--]]
function handle(r)
    r.content_type = "text/plain"
    r:puts("max-age=5 at " .. os.time() .. "\n")
    r.headers_out["Cache-Control"] = 'max-age=5'
    return apache2.OK
end