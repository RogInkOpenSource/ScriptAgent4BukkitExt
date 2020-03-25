@file:ImportScript("rogink/lib/PlotsFlags.kt")
@file:ImportByClass("com.github.intellectualsites.plotsquared.plot.PlotSquared")

package superitem.rogink

import com.github.intellectualsites.plotsquared.plot.`object`.Location
import superitem.rogink.lib.*
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.util.Vector

require(ItemInfo(Material.NAME_TAG, "地皮扩展_出生点设置", listOf(
        "&e&l使用说明",
        "&e手拿该物品在出生点右键设置出生点",
        "&e请面朝中心方向,自动判断东南西北",
        "&c左键删除"
)))

listen<PlayerInteractEvent> {
    if(it.action == Action.PHYSICAL)return@listen
    val add = it.action == Action.RIGHT_CLICK_AIR || it.action == Action.RIGHT_CLICK_BLOCK
    val p = it.player
    if (isItem(p.inventory.itemInMainHand)&&get<Permission>().hasPermission(p)) {
        val loc = with(p.location) { Location(world!!.name, blockX, blockY, blockZ) }
        val plot = loc.plot ?: return@listen p.sendMessage("§c请站在合适位置使用")
        if (!plot.hasOwner()) return@listen p.sendMessage("§c请先认领地皮")
        var flag = PlotsFlags.Flag_Spawn[plot]?:SpawnData(emptyMap())
        val newLoc = p.location.toVector().toBlockVector().subtract(with(plot.center) { Vector(x, y, z) })
        val facing = p.facing.oppositeFace
        if(add){
            flag = SpawnData(flag.dir.plus(facing to newLoc))
            p.sendMessage("§a设置出生点: $facing")
        } else {
            flag = SpawnData(flag.dir.minus(facing))
            p.sendMessage("§a移除出生点: $facing")
        }
        PlotsFlags.Flag_Spawn[plot] = flag
    }
}