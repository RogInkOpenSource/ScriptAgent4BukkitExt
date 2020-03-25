package rogink

import com.github.intellectualsites.plotsquared.plot.PlotSquared
import com.github.intellectualsites.plotsquared.plot.`object`.Plot
import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDespawnEvent
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.util.Vector
import rogink.lib.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.random.Random

name.set("RagInk 地牢控制主脚本")

val monsterIdentifierKey = "roginkMonsterTeamId"
val maxRoom = 16

data class RoomDescriptor(
        val world: World,
        val plot: Plot,
        val spawnData: SpawnData,
        val borderData: BorderData?,
        val monsterSpawnData: List<MonsterSpawnData>,
        val possibility: Double
)

data class VolatilePlayerData(
        var totalMonsterCount: Int = 0, // 当前房间生成怪物总量
        var isMonsterSpawned: Boolean = false, // 当前房间是否已经生成
        var monsters: List<Monster> = emptyList() // 生成的怪物
)

var roomDescriptors: List<RoomDescriptor> = emptyList()

@Suppress("UNCHECKED_CAST")
val playerStates
    get() = SharedData.playerStates
val mobs get() = SharedData.mobGroups()
val volatilePlayerData = ConcurrentHashMap<String, VolatilePlayerData>()

PlaceHoldApi.registerGlobalVar("RogInk._onPlayerInteractEvent",::onPlayerInteractEvent)
fun onPlayerInteractEvent(event: PlayerInteractEvent){
    if(!enabled)return
    val rightClicked = event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK
    val player = event.player

    event.isCancelled = true
    val playerState = playerStates[player.uniqueId]
    val vPlayerData = volatilePlayerData[player.uniqueId.toString()]
    // some debug info 调试信息只给有权限的人使用
    if (player.isSneaking && player.hasPermission("RogInk.debug")) {
        if (playerState == null || vPlayerData == null) {
            player.sendMessage("${ChatColor.YELLOW}游戏尚未开始")
        } else {
            if (rightClicked) {
                val id = playerState.room.id
                val plot = roomDescriptors[id].plot
                player.sendMessage("d:RoomId=${playerState.room.id}")
                player.sendMessage("d:PlotId=${plot.id}")
                player.sendMessage("d:PlotCenter=${plot.center}")
                player.sendMessage("d:BorderCenter=${PlotsFlags.Flag_Border[plot]}")
                player.sendMessage("d:${vPlayerData}")
                player.sendMessage("d:${vPlayerData.monsters.map { monster -> "${monster.location}:${monster.isValid}" }}")
            } else {
                vPlayerData.isMonsterSpawned = true
                vPlayerData.monsters.forEach { monster -> monster.remove() }
                vPlayerData.monsters = emptyList()
                SharedData.worldBorderUtil.sendToPlayer(player, player.location, 10000.0)
            }
        }
    } else {
        if (rightClicked) {
            if (player.uniqueId in playerStates) return player.sendMessage("${ChatColor.YELLOW}你已经在游戏中了")
            if (roomDescriptors.isEmpty()) loadData()
            val room = SharedData.mapGenerator(roomDescriptors.map { roomInfo ->
                RoomInfo(roomInfo.spawnData.dir.keys, roomInfo.possibility)
            }, maxRoom)
            playerStates[player.uniqueId] = PlayerState(room, player.location)
            player.teleport(player.location.apply { yaw = 180f })
            enterRoom(player, room)
        } else {
            if (player.uniqueId !in playerStates) return player.sendMessage("${ChatColor.YELLOW}你未在游戏中")
            quit(player)
        }
    }
}

listen<AsyncPlayerChatEvent> {
    if (it.message == "!help" && it.player.uniqueId in playerStates) {
        it.isCancelled = true
        val playerState = playerStates[it.player.uniqueId]!!
        val vPlayerData = volatilePlayerData[it.player.uniqueId.toString()]!!
        if (vPlayerData.monsters.isEmpty()) return@listen
        logger.info("d:PlotId=${roomDescriptors[playerState.room.id].plot.id}")
        logger.info("d:${vPlayerData.monsters.map { monster -> "${monster.location}:${monster.isValid}" }}")
        vPlayerData.monsters = vPlayerData.monsters.mapNotNull { monster ->
            if (monster.isValid) monster else {
                monster.remove();null
            }
        }
        if (vPlayerData.monsters.isEmpty())
            finishRoom(it.player)
    }
}

fun loadData() {
    // 从地皮加载所有房间信息(当前只处理第一层)
    // 区分出Boss准备房等特殊房间
    // save to roomInfo (0 as BOSS)
    // TODO: You have to mark out the boss/prep room
    // in addition to floor number, etc.
    val plots = PlotSquared.get().plots.filter {
        PlotsFlags.Flag_Spawn[it] != null
    }
    val plotWorlds = plots.map {
        Bukkit.getWorld(it.worldName) ?: error("World does not exist")
    }
    val processedPlots = mutableListOf<RoomDescriptor>()
    plots.forEachIndexed { index: Int, plot: Plot ->
        if (plot.worldName != "dl-1") return@forEachIndexed //Only Enable dl-1
        // debug
        logger.info("Loading plot #${plot.id} in world ${plot.worldName}")
        try {
            processedPlots.add(RoomDescriptor(plotWorlds[index], plot,
                    PlotsFlags.Flag_Spawn[plot]!!, PlotsFlags.Flag_Border[plot],
                    PlotsFlags.Flag_MonsterSpawn[plot]?.toList() ?: emptyList(),
                    PlotsFlags.Flag_RoomPossibility[plot] ?: 1.0))
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error reading flag data for plot #${plot.id} in world ${plot.worldName}, ignored", e)
        }
    }
    //特殊房现在由Possibility判断
//	// 手动选定1;4作为boos房
//	val boosIndex = processedPlots.indexOfFirst { with(it.plot.id){x==1&&y==4} }
//	//交换Boos准备房至0号位
//	if(boosIndex != -1) {
//		val boosRoom = processedPlots[boosIndex]
//		processedPlots[boosIndex] = processedPlots[0]
//		processedPlots[0] = boosRoom
//	}
    roomDescriptors = processedPlots
}

onEnable {
    loadData()
}

onDisable {
    PlaceHoldApi.registerGlobalVar("RogInk._onPlayerInteractEvent",null)
    playerStates.keys.forEach { uuid ->
        val player = Bukkit.getPlayer(uuid) ?: return@forEach
        player.sendMessage("${ChatColor.RED}插件重载,强制退出")
        quit(player)
    }
    playerStates.clear()
}

listen<PlayerDeathEvent> { event ->
    if (event.entity.uniqueId !in playerStates) return@listen
    quit(event.entity)
}

listen<PlayerQuitEvent> { event ->
    if (event.player.uniqueId !in playerStates) return@listen
    quit(event.player)
}

fun enterRoom(player: Player, room: Room, inD: BlockFace = room.inD) {
    val playerState = playerStates[player.uniqueId]!!
    // 修改状态
    val newState = playerState.copy(room = room)
    playerStates[player.uniqueId] = newState
//	volatilePlayerData[player.uniqueId.toString()]?.monsters?.forEach { it.remove() }
    volatilePlayerData[player.uniqueId.toString()] = VolatilePlayerData(0, false)
    // 传送
    val descriptor = roomDescriptors[room.id]
    val plot = descriptor.plot
    val world = descriptor.world
    val spawnData = descriptor.spawnData
    //特殊化 准备房,强行南人口
    if (descriptor.possibility == possibilityPrepareRoom) {
        player.sendMessage("${ChatColor.GREEN}恭喜来到准备房")
        val vector = spawnData.dir[BlockFace.SOUTH] ?: error("准备房只准有南入口")
        val location = with(plot.center) { Vector(x, y, z) }.add(vector).toLocation(world, 180f, 0f)
        player.teleport(location)
        return
    }
    val vector = spawnData.dir[inD]
    if (vector != null) {
        fun desc(dir: BlockFace): String {
            val color = if (dir in room.links) ChatColor.GREEN else ChatColor.RED
            return "$color $dir"
        }

        val left = BlockFace.values()[(inD.ordinal + 1) % 4]
        player.sendMessage("""
			|
			|   ${desc(inD.oppositeFace)}
			|${desc(left)}§a▩${desc(left.oppositeFace)}
			|   ${desc(inD)}(IN)
		""".trimMargin())
        val location = with(plot.center) { Vector(x, y, z) }.add(vector).toLocation(world, player.location.yaw, player.location.pitch)
        player.teleport(location)
        // 生成边界，如果有的话
        val border = descriptor.borderData ?: return
        SharedData.worldBorderUtil.sendToPlayer(player, with(plot.center) { Vector(x, y, z) }.add(border.center).toLocation(world), border.range.toDouble())
    } else {
        player.sendMessage("§c你不能向这个方向走")
    }
}

fun quit(player: Player) {
    val playerState = playerStates[player.uniqueId]!!
    val vPlayerData = volatilePlayerData[player.uniqueId.toString()]!!
    // 删除状态
    playerStates.remove(player.uniqueId)
    volatilePlayerData.remove(player.uniqueId.toString())
    // 回到主城
    player.teleport(playerState.joinPos)
    // 清除边界
    // XXX: This just sets a 'big enough' border, it doesn't clear it anyway
    SharedData.worldBorderUtil.sendToPlayer(player, player.location, 10000.0)
    // 清除怪物
    vPlayerData.monsters.forEach { it.remove() }
}

listen<PlayerMoveEvent> { event ->
    if (event.player.uniqueId !in playerStates) return@listen
    // XXX: Watch out for performance overhead
    val player = event.player
    val playerState = playerStates[player.uniqueId]!!
    val vPlayerData = volatilePlayerData[player.uniqueId.toString()]!!
    val descriptor = roomDescriptors[playerState.room.id]
    val plot = descriptor.plot
    val room = playerState.room

    //刷怪检测
    if (!vPlayerData.isMonsterSpawned) {
        if (descriptor.monsterSpawnData.isEmpty()) {
            vPlayerData.isMonsterSpawned = true
            finishRoom(player)
        } else {
            val borderData = descriptor.borderData
            val spawn = borderData == null || let {
                val borderVector = with(plot.center) { Vector(x, y, z) }.add(borderData.center)
                val deltaPos = player.location.toVector().subtract(borderVector)
                //采用边界的方形距离判定(-1防止玩家卡边界处)
                abs(deltaPos.x) * 2 < (borderData.range - 1) && abs(deltaPos.z) * 2 < (borderData.range - 1)
            }
            if (spawn) {
                val plotCenterLocation = with(plot.center) { Vector(x, y, z).toLocation(descriptor.world) }
                val monsters = mutableListOf<Monster>()
                descriptor.monsterSpawnData.forEach {
                    monsters.addAll(spawnMonsterAt(it.type, plotCenterLocation.clone().add(it.pos), player))
                }
                vPlayerData.totalMonsterCount = monsters.size
                vPlayerData.monsters = monsters
                vPlayerData.isMonsterSpawned = true
            }
        }
    }
    //传送门检测
    if (player.eyeLocation.block.type == Material.END_GATEWAY) {
        if (volatilePlayerData[player.uniqueId.toString()]!!.monsters.isNotEmpty())
            return@listen player.sendTitle("§c请先清空怪物", "", 0, 10, 10)
        val plotCenterVector = with(plot.center) { Vector(x, y, z) }
        descriptor.borderData?.let { plotCenterVector.add(it.center) }
        val normalizedVector = player.location.toVector().subtract(plotCenterVector).normalize()
        val normalizedAngle = with(normalizedVector) {
            atan2(z, x) / PI
        }
        // Player might be facing other ways when entering portal
        // Manual calculation is required here
        val direction = when {
            //特殊处理准备房,因为无法判断正确方位
            descriptor.possibility == possibilityPrepareRoom -> room.inD
            normalizedAngle < -3.0 / 4.0 || normalizedAngle > 3.0 / 4.0 -> BlockFace.WEST
            normalizedAngle >= -3.0 / 4.0 && normalizedAngle < -1.0 / 4.0 -> BlockFace.NORTH
            normalizedAngle >= -1.0 / 4.0 && normalizedAngle < 1.0 / 4.0 -> BlockFace.EAST
            else -> BlockFace.SOUTH
        }
        val nextRoom = room.links[direction]
        if (nextRoom != null) {
            enterRoom(player, nextRoom, direction.oppositeFace)
        } else {
            player.sendTitle("§c这个传送门不通往任何房间", "($direction)", 0, 10, 10)
            // XXX: Override for boss/prep room?
        }
    }
}

listen<MythicMobDeathEvent> {
    handleMonsterDeath(it.entity)
}
listen<MythicMobDespawnEvent> {
    handleMonsterDeath(it.entity)
}
listen<EntityDeathEvent> {
    handleMonsterDeath(it.entity)
}

fun handleMonsterDeath(entity: Entity) {
    if (entity is Monster && entity.hasMetadata(monsterIdentifierKey)) {
        val key = entity.getMetadata(monsterIdentifierKey)[0].asString()
        val player = Bukkit.getPlayer(UUID.fromString(key)) ?: return
        val vPlayerData = volatilePlayerData[key] ?: return
        vPlayerData.monsters -= entity
        if (vPlayerData.isMonsterSpawned && vPlayerData.monsters.isEmpty())
            finishRoom(player)
    }
}

fun finishRoom(player: Player) {
    SharedData.worldBorderUtil.sendToPlayer(player, player.location, 10000.0)
}

fun spawnMonsterAt(descriptor: String, location: Location, player: Player): List<Monster> {
    fun parseRange(str: String): Int {
        val parts = str.split('-')
        if (parts.size == 1) return parts[0].toInt()
        return Random.nextInt(parts[0].toInt(), parts[1].toInt() + 1)
    }

    val parts = descriptor.split(':')
    val groupName = parts[0]
    val level = parseRange(parts[1])
    val count = parseRange(parts[2])
    val mobList = mobs[groupName]
    if (mobList.isNullOrEmpty()) {
        logger.warning("怪物组 $groupName 为空")
        return emptyList()
    }
    val spawnedMonsters = mutableListOf<Monster>()
    (1..count).forEach { _ ->
        val mobName = mobList.toList().random().toString()
        val loc = location.clone().apply {
            x += 0.5 + 0.5 * Random.nextDouble()
            z += 0.5 + 0.5 * Random.nextDouble()
            y += 1.5
        }
//		val loc = MythicMobs.inst().spawnerManager.findSpawningLocation(location,1)
        val monster = MythicMobs.inst().apiHelper.spawnMythicMob(mobName, loc, level) as Monster
        if (monster.isValid) {
            monster.setMetadata(monsterIdentifierKey, FixedMetadataValue(Manager.pluginMain, player.uniqueId.toString()))
            monster.target = player
            spawnedMonsters.add(monster)
        }
    }
    return spawnedMonsters
}
