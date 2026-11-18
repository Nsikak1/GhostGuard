package ng.name.version5.service

import android.accessibilityservice.AccessibilityService
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/** Admin can be granted/revoked in system Settings or via adb, so tell the service the moment it changes. */
class GhostDeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: android.content.Intent) = ProtectionService.reload(context)
    override fun onDisabled(context: Context, intent: android.content.Intent) = ProtectionService.reload(context)
}

/** Optional lock path for API 28+ that needs no device admin. */
class GhostAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    companion object { @Volatile var instance: GhostAccessibilityService? = null }
}

object ScreenLocker {
    fun adminComponent(ctx: Context) = ComponentName(ctx, GhostDeviceAdmin::class.java)

    fun isAdminActive(ctx: Context): Boolean =
        (ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).isAdminActive(adminComponent(ctx))

    fun isAccessibilityReady(): Boolean = Build.VERSION.SDK_INT >= 28 && GhostAccessibilityService.instance != null

    fun canLock(ctx: Context) = isAdminActive(ctx) || isAccessibilityReady()

    /** Locks the screen without touching the touch pipeline, so a held ghost touch cannot stop it. */
    fun lock(ctx: Context): Boolean {
        if (isAdminActive(ctx)) {
            (ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).lockNow()
            return true
        }
        if (Build.VERSION.SDK_INT >= 28) {
            GhostAccessibilityService.instance?.let {
                return it.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            }
        }
        return false
    }
}
