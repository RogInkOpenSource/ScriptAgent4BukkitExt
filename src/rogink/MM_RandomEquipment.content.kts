package rogink

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicReloadedEvent
import io.lumine.xikage.mythicmobs.drops.EquipSlot
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.items.MythicItem
import io.lumine.xikage.mythicmobs.logging.MythicLogger
import io.lumine.xikage.mythicmobs.skills.INoTargetSkill
import io.lumine.xikage.mythicmobs.skills.SkillMechanic
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.util.annotations.MythicMechanic
import kotlin.random.Random

name=("MM_技能_随机装备")

val regex = Regex("([A-Z])([0-5])")
val items = mutableMapOf<String, MutableSet<MythicItem>>()

fun tryLoad(inst: MythicMobs) {
    items.clear()
    var sum = 0
    inst.itemManager.items.forEach {
        val v = it.config.getString("RandomEquip")
        if (v != null) {
            if (!regex.matches(v))
                return MythicLogger.errorItemConfig(it, it.config, "RandomEquip must be like A0,E5,C3...")
            items.getOrPut(v) { mutableSetOf() }.add(it)
            sum++
        }
    }
    logger.info("loaded $sum items")
}

@MythicMechanic(author = "Way__Zer", name = "randomEquip")
inner class RandomEquipMechanic(config: MythicLineConfig) : SkillMechanic("randomEquip", config), INoTargetSkill {
    override fun cast(p0: SkillMetadata): Boolean {
        if (items.isEmpty()) {
            tryLoad(MythicMobs.inst())
        }
        val match = Regex(".*\\{(.*)}").matchEntire(config.line)
        if (match == null) {
            MythicLogger.errorMechanicConfig(this, config, "Must give params")
            return true
        }
        match.groupValues[1].split(' ').forEach {
            val sp = it.split(':')
            if (sp.size != 2)
                MythicLogger.errorMechanicConfig(this, config, "Must be like {A1:0.5 B1:0.3}: get $it")
            else {
                val list = items[sp[0]]
                if (list == null || list.isEmpty())
                    MythicLogger.errorMechanicConfig(this, config, "Empty List ${sp[0]}")
                else {
                    val m2 = regex.matchEntire(sp[0])!!
                    if (Random.nextDouble() < sp[1].toDouble()) {
                        EquipSlot.values()[m2.groupValues[2].toInt()].equip(p0.caster.entity, list.random().generateItemStack(1))
                        return true
                    }
                }
            }
        }
        return true
    }
}

listen<MythicMechanicLoadEvent> {
    if (it.mechanicName.toLowerCase() != "randomEquip".toLowerCase()) return@listen
    logger.info("mechanic ${it.mechanicName} ${it.config.line}")
    it.register(RandomEquipMechanic(it.config))
}

listen<MythicReloadedEvent> { e ->
    tryLoad(e.instance)
}
