package rogink

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.WorldBorder
import org.bukkit.entity.Player
import rogink.lib.SharedData

object WorldBorderImpl : SharedData.WorldBorderUtil {
    private val version = Bukkit.getServer().javaClass.getPackage().name.substringAfterLast('.')
    private lateinit var handle: Any
    private val worldBorder: WorldBorder by lazy {
        val clsCraftWorldBorder = Class.forName("org.bukkit.craftbukkit.$version.CraftWorldBorder")
        val worldBorder = clsCraftWorldBorder.constructors[0].let {
            it.isAccessible = true
            it.newInstance(Bukkit.getWorlds()[0]) as WorldBorder
        }
        val clsWorldBorder = Class.forName("net.minecraft.server.$version.WorldBorder")
        handle = clsWorldBorder.newInstance()
        val clsCraftWorld = Class.forName("org.bukkit.craftbukkit.$version.CraftWorld")
        val craftWorld = clsCraftWorld.getDeclaredField("world").apply { isAccessible = true }.get(Bukkit.getWorlds()[0])
        clsWorldBorder.getField("world").set(handle, craftWorld)
        clsCraftWorldBorder.getDeclaredField("handle").apply { isAccessible = true }.set(worldBorder, handle)
        worldBorder.damageAmount = 0.0
        worldBorder.damageBuffer = 0.0
        worldBorder.warningDistance = 1
        worldBorder.warningTime = 10
        worldBorder
    }

    override fun sendToPlayer(p: Player, center: Location?, size: Double) {
        worldBorder.center = center!!
        worldBorder.size = size
        sendPacket(p, getEnum("INITIALIZE"))
    }

    override fun addSize(p: Player, oldsize: Double, newsize: Double) {
        worldBorder.size = oldsize
        worldBorder.setSize(newsize, ((newsize - oldsize) * 5).toLong())
        sendPacket(p, getEnum("LERP_SIZE"))
    }

    private fun getEnum(str: String): Enum<*> {
        val clsEnum = Class.forName("net.minecraft.server.$version.PacketPlayOutWorldBorder\$EnumWorldBorderAction")
        return clsEnum.enumConstants.first { (it as Enum<*>).name == str } as Enum<*>
    }

    private fun sendPacket(player: Player, enum: Enum<*>) {
        val clsEnum = Class.forName("net.minecraft.server.$version.PacketPlayOutWorldBorder\$EnumWorldBorderAction")
        val clsWorldBorder = Class.forName("net.minecraft.server.$version.WorldBorder")
        val packet = Class.forName("net.minecraft.server.$version.PacketPlayOutWorldBorder")
                .getConstructor(clsWorldBorder, clsEnum).newInstance(handle, enum)

        val clsCraftPlayer = Class.forName("org.bukkit.craftbukkit.$version.entity.CraftPlayer")
        val entity = clsCraftPlayer.getMethod("getHandle").invoke(player)
        val clsEntityPlayer = Class.forName("net.minecraft.server.$version.EntityPlayer")
        val con = clsEntityPlayer.getField("playerConnection")[entity]

        val clsPlayerConnection = Class.forName("net.minecraft.server.$version.PlayerConnection")
        val clsPacket = Class.forName("net.minecraft.server.$version.Packet")
        clsPlayerConnection.getMethod("sendPacket", clsPacket).invoke(con, packet)
    }
}

SharedData.worldBorderUtil = WorldBorderImpl
PlaceHoldApi.registerGlobalVar("RogInk._sendWorldBorder", WorldBorderImpl::sendToPlayer)
onDisable {
    PlaceHoldApi.registerGlobalVar("RogInk._sendWorldBorder", null)
}