@file:ImportByClass("org.bukkit.Bukkit")
@file:ImportByClass("com.github.intellectualsites.plotsquared.plot.PlotSquared")

import cf.wayzer.script_agent.bukkit.Helper.baseConfig

name.set("RogInk模块")
addLibraryByClass("com.github.intellectualsites.plotsquared.plot.PlotSquared")
addLibraryByClass("io.lumine.xikage.mythicmobs.MythicMobs")
baseConfig()
generateHelper()