import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.*;

class MainLua {

    fun run () {
        var globals: Globals = JsePlatform.standardGlobals()
        //var chunk: LuaValue = globals.load("print 'hello, lua'")
        var chunk: LuaValue = globals.loadfile("lua/main.lua")
        chunk.call()
    }

}