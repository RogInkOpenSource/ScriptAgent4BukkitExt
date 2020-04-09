@file:ImportScript("rogink/lib/PlotsFlags.kt")
@file:ImportByClass("com.github.intellectualsites.plotsquared.plot.PlotSquared")

import cf.wayzer.script_agent.Config
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.util.Vector
import superitem.rogink.lib.BorderData
import superitem.rogink.lib.PlotsFlags
import com.github.intellectualsites.plotsquared.plot.`object`.Location as PlotLocation

require(ItemInfo(Material.NAME_TAG, "地皮扩展_边界范围设置", listOf(
        "&e&l使用说明",
        "&e手拿该物品站在地牢中心点",
        "&e输入一个整数来设置边界中心和范围"
)))

listen<AsyncPlayerChatEvent> {
    val p = it.player
    if (isItem(p.inventory.itemInMainHand) && get<Permission>().hasPermission(p)) {
        it.isCancelled = true
        val num = it.message.toIntOrNull() ?: return@listen p.sendMessage("§c请输入正确的数字")
        val loc = with(p.location) { PlotLocation(world!!.name, blockX, blockY, blockZ) }
        val plot = loc.plot ?: return@listen p.sendMessage("§c请站在合适位置使用")
        if (!plot.hasOwner()) return@listen p.sendMessage("§c请先认领地皮")
        val center = p.location.toVector().subtract(with(plot.center) { Vector(x, y, z) })
        PlotsFlags.Flag_Border[plot] = BorderData(center, num)
        p.sendMessage("§a设置成功,10s预览")
        sendWorldBorder(p, p.location, num.toDouble())
        sendWorldBorder(p, p.location, num.toDouble())
        createBukkitTask {
            if (!enabled) return@createBukkitTask
            sendWorldBorder(p, p.location, 10000.0)
            p.sendMessage("§a预览结束")
        }.runTaskLater(Config.pluginMain, 200)
    }
}

val handler by PlaceHold.reference<(Player, Location, Double) -> Unit>("RogInk._sendWorldBorder")

fun sendWorldBorder(p: Player, loc: Location, width: Double) {
    handler.invoke(p, loc, width)
}
