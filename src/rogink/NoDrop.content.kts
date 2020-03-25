import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

name.set("RogInk掉落物控制")
/**
 * 被怪击杀无掉落
 * 被玩家补刀有掉落物
 */

val lastDamageType = mutableMapOf<Player,EntityDamageEvent.DamageCause>()
val lastDamageSource = mutableMapOf<Player,EntityType>()

listen<EntityDamageEvent> {
    if(it.entityType != EntityType.PLAYER)return@listen
    lastDamageType[it.entity as Player] = it.cause
}

listen<EntityDamageByEntityEvent> {
    if(it.entityType != EntityType.PLAYER)return@listen
    lastDamageSource[it.entity as Player] = it.damager.type
}

listen<PlayerDeathEvent> {event->
    val p = event.entity
    if(p.world.name.startsWith("spawn",true))return@listen
    //选择性保留
    val itemToKeep = mutableListOf<ItemStack>()
    fun keep(item:ItemStack) = item.itemMeta?.lore?.contains("死亡不掉落") == true
    event.itemsToKeep.forEach {
        if(keep(it))itemToKeep.add(it)
    }
    event.drops.forEach {
        if(keep(it))itemToKeep.add(it)
    }
    //清除背包和经验
    event.keepInventory=false
    event.keepLevel=false
    event.newTotalExp = 0
    event.itemsToKeep.clear()
    //判断是否掉落
    if (lastDamageType[p] != EntityDamageEvent.DamageCause.ENTITY_ATTACK || lastDamageSource[p] != EntityType.PLAYER) {
        event.drops.clear()
        event.droppedExp = 0
    }
    event.drops.removeAll(itemToKeep)
    event.itemsToKeep.addAll(itemToKeep)
    lastDamageSource.remove(p)
    lastDamageType.remove(p)
}

listen<PlayerQuitEvent> {
    lastDamageSource.remove(it.player)
    lastDamageType.remove(it.player)
}
