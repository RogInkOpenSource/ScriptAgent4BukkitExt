package superitem.rogink

import org.bukkit.Bukkit
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import superitem.lib.events.SuperItemEvent

require(ItemInfo(Material.PAPER, "RogInk 试纸", listOf(
        "该物品作为测试阶段的入场券，右键开始游戏",
        "游戏中左键可以退出游戏",
        "蹲下 + 左键查看当前房间的调试信息",
        "蹲下 + 右键强制通关",
        "这个物品不掉落"
)))

listen<PlayerInteractEvent> {
    if (it.action == Action.PHYSICAL) return@listen
    if (isItem(it.player.inventory.itemInMainHand)) {
        @Suppress("UNCHECKED_CAST")
        val handler = PlaceHoldApi.GlobalContext.getVar("RogInk._onPlayerInteractEvent") as? (PlayerInteractEvent)->Unit
        handler?.invoke(it)
    }
}