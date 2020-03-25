package rogink

import org.bukkit.block.BlockFace
import rogink.lib.*
import java.io.File
import java.io.OutputStreamWriter
import kotlin.random.Random

val openDebug = File("randomMap.out")

fun generateMap(roomInfo: List<RoomInfo>, maxSize:Int = 16):Room{
    var roomCount = 1
    val debugOut: OutputStreamWriter? = if(openDebug.exists()) openDebug.writer() else null
    debugOut?.append("digraph first2{\n")
    /**
     * @return RoomId
     */
    fun getNextRoom(inD:BlockFace,mainWay:Boolean): Int {
        if(mainWay&&roomCount > maxSize)
            return roomInfo.indexOfFirst{ it.possibility == possibilityPrepareRoom}
                    .also { require(it!=-1){"Not Find PrepareRoom"} }
        var i = 100
        while (i>0){
            i--
            val id = Random.nextInt(roomInfo.size)
            //特殊房 always false
            if(roomInfo[id].possibility < Random.nextDouble())continue
            if(inD in roomInfo[id].directions &&(!mainWay || roomInfo[id].directions.size>=2))return id
        }
        error("Can't getNextRoom after 100 try")
    }
    fun run(room: Room, leafPossibly:Double=0.7, mainWay:Boolean=true){
        val leftDir = roomInfo[room.id].directions.filter { it!=room.inD }.toMutableList()
        leftDir.shuffle()
        if(mainWay)room.mainD = leftDir.random().also { leftDir.remove(it) }
        fun next(dir:BlockFace){
            val nMain = dir == room.mainD
            if(!nMain&&(roomCount> maxSize ||  Random.nextDouble() > leafPossibly))return
            roomCount++
            val next = Room().apply {
                inD=dir.oppositeFace
                id=getNextRoom(inD,nMain)
                links[inD]=room
            }
            debugOut?.append("\"$room\" -> \"$next\" [label=\"$dir\",${if (nMain)"color=red" else ""}];\n")
            room.links[dir] = next
            if(roomInfo[next.id].possibility != possibilityPrepareRoom) run(next,leafPossibly - (if(nMain)0.04 else 0.3),nMain)
        }
        leftDir.forEach(::next)
        room.mainD?.let(::next)
    }
    val ret = Room().apply { id = getNextRoom(inD,true) }.also { run(it) }
    debugOut?.append("}")
    debugOut?.close()
    return ret
}

SharedData.mapGenerator = ::generateMap