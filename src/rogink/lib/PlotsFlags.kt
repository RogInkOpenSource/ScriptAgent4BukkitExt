package rogink.lib

import com.github.intellectualsites.plotsquared.plot.`object`.Plot
import com.github.intellectualsites.plotsquared.plot.flag.Flag
import com.github.intellectualsites.plotsquared.plot.flag.FlagManager
import com.google.gson.Gson

object PlotsFlags {
    private val gson by lazy { Gson() }

    class MyFlag<T>(private val cls: Class<T>, name: String){
        val flag: Flag<*> = FlagManager.getOrCreateFlag(name)
        operator fun get(plot: Plot):T?{
            return  plot.getFlag(flag).orElse(null)?.let { gson.fromJson(it.toString(),cls) }
        }
        operator fun set(plot: Plot,v:T){
            plot.setFlag(flag, gson.toJson(v))
        }
    }

    val Flag_Border by lazy { MyFlag(BorderData::class.java, "RagInk_Border") }
    val Flag_Spawn by lazy { MyFlag(SpawnData::class.java,"RagInk_Spawns") }
    val Flag_MonsterSpawn by lazy { MyFlag(Array<MonsterSpawnData>::class.java,"RagInk_MonsterSpawns") }
    val Flag_RoomPossibility by lazy { MyFlag(Double::class.java,"RogInk_Possibility") }
}
