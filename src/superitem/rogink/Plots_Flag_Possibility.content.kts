@file:ImportScript("rogink/lib/PlotsFlags.kt")
@file:ImportByClass("com.github.intellectualsites.plotsquared.plot.PlotSquared")

package superitem.rogink

import com.github.intellectualsites.plotsquared.plot.`object`.Location
import org.bukkit.event.player.AsyncPlayerChatEvent
import superitem.rogink.lib.PlotsFlags

require(ItemInfo(Material.NAME_TAG,"地皮扩展_生成可能性设置", listOf(
        "手持物品输入即可",
        "格式为小数例 0.5",
        "若未设置,默认为1",
        "特殊房间: -1 代表准备房",
        "特殊房间: -2 代表BOSS房"
)))

listen<AsyncPlayerChatEvent> {
    val p = it.player
    if (isItem(p.inventory.itemInMainHand)&&get<Permission>().hasPermission(p)) {
        it.isCancelled = true
        val num = it.message.toDoubleOrNull() ?: return@listen p.sendMessage("§c请输入正确的数字")
        val loc = with(p.location) { Location(world!!.name, blockX, blockY, blockZ) }
        val plot = loc.plot ?: return@listen p.sendMessage("§c请站在合适位置使用")
        if (!plot.hasOwner()) return@listen p.sendMessage("§c请先认领地皮")
        PlotsFlags.Flag_RoomPossibility[plot] = num
        p.sendMessage("§a设置成功")
    }
}