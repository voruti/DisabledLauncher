package de.redno.disabledlauncher.data

import de.redno.disabledlauncher.model.AppEntryInList

class Datasource {

    fun loadAppList(): List<AppEntryInList> {
        return listOf(
            AppEntryInList("App 1", "de.redno.app1", "icon1", false),
            AppEntryInList("App 2", "de.redno.app2", "icon2", true),
            AppEntryInList("App 3", "de.redno.app3", "icon3", false),
            AppEntryInList("App 4", "de.redno.app4", "icon4", true),
            AppEntryInList("App 5", "de.redno.app5", "icon5", false),
            AppEntryInList("App 6", "de.redno.app6", "icon6", true),
            AppEntryInList("App 7", "de.redno.app7", "icon7", false),
            AppEntryInList("App 8", "de.redno.app8", "icon8", true),
            AppEntryInList("App 9", "de.redno.app9", "icon9", false),
            AppEntryInList("App 10", "de.redno.app10", "icon10", true),
            AppEntryInList("App 11", "de.redno.app11", "icon11", false),
            AppEntryInList("App 12", "de.redno.app12", "icon12", true),
            AppEntryInList("App 13", "de.redno.app13", "icon13", false),
            AppEntryInList("App 14", "de.redno.app14", "icon14", true),
            AppEntryInList("App 15", "de.redno.app15", "icon15", false),
            AppEntryInList("App 16", "de.redno.app16", "icon16", true),
            AppEntryInList("App 17", "de.redno.app17", "icon17", false),
            AppEntryInList("App 18", "de.redno.app18", "icon18", true),
            AppEntryInList("App 19", "de.redno.app19", "icon19", false),
            AppEntryInList("App 20", "de.redno.app20", "icon20", true),
            AppEntryInList("App 21", "de.redno.app21", "icon21", false)
        )
    }
}
