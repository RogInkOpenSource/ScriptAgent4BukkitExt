package rogink

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicReloadedEvent
import rogink.lib.*

val mobs = mutableMapOf<String, MutableList<String>>()

SharedData.mobGroups = {
    if (mobs.isEmpty()) tryLoad(MythicMobs.inst())
    mobs
}

fun tryLoad(inst: MythicMobs) {
    mobs.clear()
    var sum = 0
    inst.mobManager.mobTypes.forEach {
        val v = it.config.getString("SpawnGroup")
        if (v != null && v.length > 2) {
            v.split(' ').forEach { k ->
                mobs.getOrPut(k) { mutableListOf() }.add(it.internalName)
                sum++
            }
        }
    }
    logger.info("loaded $sum mobs")
}

listen<MythicReloadedEvent> { e ->
    tryLoad(e.instance)
}

onEnable{
    MythicMobs.inst()?.let(::tryLoad)
}