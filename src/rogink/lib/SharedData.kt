package rogink.lib

import cf.wayzer.script_agent.util.DSLBuilder
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*

object SharedData:DSLBuilder() {
    interface WorldBorderUtil{
        fun sendToPlayer(p: Player, center: Location?, size: Double)
        fun addSize(p: Player, oldsize: Double, newsize: Double)
    }
    lateinit var worldBorderUtil:WorldBorderUtil
    lateinit var mapGenerator: (roomInfo: List<RoomInfo>, maxSize:Int)-> Room
    lateinit var mobGroups : ()->Map<String,List<String>>
    val playerStates = mutableMapOf<UUID,PlayerState>()
}