@file:ImportScript("rogink/lib/PlotsFlags.kt")
@file:ImportByClass("com.github.intellectualsites.plotsquared.plot.PlotSquared")

package superitem.rogink

import com.github.intellectualsites.plotsquared.plot.`object`.Location
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.util.Vector
import superitem.rogink.lib.*

require(ItemInfo(Material.SPAWNER, "地皮扩展_刷怪点设置", listOf(
        "&e&l使用说明",
        "&e右键放置,shift+右键清除",
        "&e请在聊天栏输入以设置类型",
        "&e例如(1-5级,4-6只): AA:1-5:4-6"
)))

val regex = Regex("(\\w+):(\\d+(-\\d+)?):(\\d+(-\\d+)?)")

listen<AsyncPlayerChatEvent> {
    val p = it.player
    val item = p.inventory.itemInMainHand
    if (isItem(item) && get<Permission>().hasPermission(p)) {
        it.isCancelled = true
        if (!regex.matches(it.message)) return@listen p.sendMessage("§c输入正确的格式")
        item.itemMeta = item.itemMeta!!.apply {
            val lore = this.lore ?: mutableListOf()
            lore[lore.size - 1] = "§e当前设置: ${it.message}"
            this.lore = lore
        }
        p.sendMessage("§a设置成功")
    }
}

listen<BlockPlaceEvent> {
    if (!isItem(it.itemInHand)) return@listen
    it.isCancelled = true
    val p = it.player
    if (get<Permission>().hasPermission(p)) {
        var type = it.itemInHand.itemMeta?.lore?.last()
        if (type?.startsWith("§e当前设置: ") != true) return@listen p.sendMessage("§c先设置类型")
        type = type.removePrefix("§e当前设置: ")
        val loc = with(it.blockPlaced.location) { Location(world!!.name, blockX, blockY, blockZ) }
        val plot = loc.plot ?: return@listen p.sendMessage("§c请站在合适位置使用")
        if (!plot.hasOwner()) return@listen p.sendMessage("§c请先认领地皮")
        val flag = PlotsFlags.Flag_MonsterSpawn[plot]?.toMutableList() ?: mutableListOf()
        val newLoc = it.blockPlaced.location.toVector().subtract(with(plot.center) { Vector(x, y, z) })
        if (p.isSneaking) flag.removeIf { d -> d.pos == newLoc } else flag.add(MonsterSpawnData(newLoc, type))
        PlotsFlags.Flag_MonsterSpawn[plot] = flag.toTypedArray()
        p.sendMessage("§a添加成功: $type")
    }
}