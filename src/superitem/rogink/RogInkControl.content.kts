package superitem.rogink

import org.bukkit.Bukkit
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import superitem.lib.events.SuperItemEvent

require(ItemInfo(Material.PAPER, "§aRogInk 试纸", listOf(
        "该物品作为测试阶段的入场券，右键开始游戏",
        "游戏中左键可以退出游戏",
        "蹲下 + 左键查看当前房间的调试信息",
        "蹲下 + 右键强制通关",
        "§7死亡不掉落"
)))

val handler by PlaceHold.reference<(PlayerInteractEvent)->Unit>("RogInk._onPlayerInteractEvent")

listen<PlayerInteractEvent> {
    if (it.action == Action.PHYSICAL) return@listen
    if (isItem(it.player.inventory.itemInMainHand)) {
        handler.invoke(it)
    }
}