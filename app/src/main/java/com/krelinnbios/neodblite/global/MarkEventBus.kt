package com.krelinnbios.neodblite.global

/**
 * 标记增删改的全局脏标记：详情页、书架、发现/搜索长按快速标记等任一入口成功保存或删除标记后置位，
 * 个人主页下次进入时据此判断是否需要重新拉取书架统计与最近完成条目，避免用户手动下拉刷新才能看到最新数据。
 */
object MarkEventBus {
    @Volatile
    private var dirty: Boolean = false

    fun markDirty() {
        dirty = true
    }

    fun consumeDirty(): Boolean {
        val was = dirty
        dirty = false
        return was
    }
}
