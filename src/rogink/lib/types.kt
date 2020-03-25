package rogink.lib

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.util.Vector
import java.io.Serializable

class Room:Serializable{
    var id = 0
    val links = mutableMapOf<BlockFace, Room>()
    var inD = BlockFace.SOUTH
    var mainD:BlockFace? =null
    @Transient private val _id:Int
    init {
        count++
        _id = count
    }

    override fun toString(): String {
        return "$_id $id"
    }
    companion object{
        var count = 0
    }
}

typealias RelativePos = Vector

data class BorderData(val center: RelativePos, val range: Int)
data class SpawnData(val dir:Map<BlockFace, RelativePos>)
data class MonsterSpawnData(val pos: RelativePos, val type: String)

data class RoomInfo(val directions:Collection<BlockFace>, val possibility:Double)

const val possibilityPrepareRoom = -1.0
const val possibilityBossRoom = -2.0

data class PlayerState(
        val room: Room,
        val joinPos: Location // 开始游戏前大厅位置,用于退出
) : Serializable
