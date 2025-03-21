package com.topjohnwu.magisk.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.forEach
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.ObservableArrayList
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import com.topjohnwu.magisk.MainDirections
import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.arch.BaseViewModel
import com.topjohnwu.magisk.arch.startAnimations
import com.topjohnwu.magisk.arch.viewModel
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.Config.context
import com.topjohnwu.magisk.core.Config.fileName
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.isRunningAsStub
import com.topjohnwu.magisk.core.ktx.reboot
import com.topjohnwu.magisk.core.ktx.synchronized
import com.topjohnwu.magisk.core.ktx.toast
import com.topjohnwu.magisk.core.model.module.LocalModule
import com.topjohnwu.magisk.core.tasks.HideAPK
import com.topjohnwu.magisk.core.tasks.MagiskInstaller
import com.topjohnwu.magisk.databinding.ActivityMainMd2Binding
import com.topjohnwu.magisk.ui.flash.ConsoleItem
import com.topjohnwu.magisk.ui.home.HomeFragmentDirections
import com.topjohnwu.magisk.view.MagiskDialog
import com.topjohnwu.magisk.view.Shortcuts
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel : BaseViewModel()

class MainActivity : SplashActivity<ActivityMainMd2Binding>() {

    override val layoutRes = R.layout.activity_main_md2
    override val viewModel by viewModel<MainViewModel>()
    override val navHostId: Int = R.id.main_nav_host
    override val snackbarView: View
        get() {
            val fragmentOverride = currentFragment?.snackbarView
            return fragmentOverride ?: super.snackbarView
        }
    override val snackbarAnchorView: View?
        get() {
            val fragmentAnchor = currentFragment?.snackbarAnchorView
            return when {
                fragmentAnchor?.isVisible == true -> fragmentAnchor
                binding.mainNavigation.isVisible -> return binding.mainNavigation
                else -> null
            }
        }

    private var isRootFragment = true

    private var isFirstInstall = false

    private var hasInstalled = false

    private var handler = Handler()

    @SuppressLint("InlinedApi")
    override fun showMainUI(savedInstanceState: Bundle?) {
        setContentView()
        showUnsupportedMessage()
        askForHomeShortcut()
        checkStubComponent()

        // Ask permission to post notifications for background update check
        if (Config.checkUpdate) {
            withPermission(Manifest.permission.POST_NOTIFICATIONS) {
                Config.checkUpdate = it
            }
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        navigation.addOnDestinationChangedListener { _, destination, _ ->
            isRootFragment = when (destination.id) {
                R.id.homeFragment,
                R.id.modulesFragment,
                R.id.superuserFragment,
                R.id.logFragment -> true

                else -> false
            }

            setDisplayHomeAsUpEnabled(!isRootFragment)
            requestNavigationHidden(!isRootFragment)

            binding.mainNavigation.menu.forEach {
                if (it.itemId == destination.id) {
                    it.isChecked = true
                }
            }
        }

        setSupportActionBar(binding.mainToolbar)

        binding.mainNavigation.setOnItemSelectedListener {
            getScreen(it.itemId)?.navigate()
            true
        }
        binding.mainNavigation.setOnItemReselectedListener {
            // https://issuetracker.google.com/issues/124538620
        }
        binding.mainNavigation.menu.apply {
            findItem(R.id.superuserFragment)?.isEnabled = Info.showSuperUser
            findItem(R.id.modulesFragment)?.isEnabled = Info.env.isActive && LocalModule.loaded()
        }

        val section =
            if (intent.action == Intent.ACTION_APPLICATION_PREFERENCES)
                Const.Nav.SETTINGS
            else
                intent.getStringExtra(Const.Key.OPEN_SECTION)

        getScreen(section)?.navigate()

        if (!isRootFragment) {
            requestNavigationHidden(requiresAnimation = savedInstanceState == null)
        }

        isFirstInstall = Config.isFirstInstall
        hasInstalled = Config.hasInstalled


        val checkCommand = "magisk -V"
        val result = ShellUtils.fastCmd(checkCommand).trim()
        println("check magisk command output: $result")


        Log.e("hjy", "isFirstInstall: " + isFirstInstall)
        if (isFirstInstall) {
            Config.zygisk = true

            if (result.isNotEmpty() && result.contains("27001")) {
                println("Magisk has installed,don't need to install again")
                Config.hasInstalled = true
                reboot()
            } else {
                println("Magisk environment has not installed")
                Config.hasInstalled = false
                HomeFragmentDirections.actionHomeFragmentToInstallFragment().navigate()
            }


        } else {
            if (!Info.isZygiskEnabled) {
                Config.zygisk = true

                if (result.isNotEmpty() && result.contains("27001")) {
                    println("Magisk has installed,don't need to install again")
                    Config.hasInstalled = true
                } else {
                    println("Magisk environment has not installed")
                    Config.hasInstalled = false
                    HomeFragmentDirections.actionHomeFragmentToInstallFragment().navigate()
                }
//                val checkCommand = "magisk --install-module /sdcard/LSPosed-v1.10.0-7089-zygisk-release.zip"
//                val result = ShellUtils.fastCmd(checkCommand).trim()
//                println("install command output: $result")
//
//                if (result.contains("Done")) {
//                    reboot()
//                } else {
//                    println("install lsposed failed")
//                }

            }
            if (hasInstalled) {
                deleteSuIfExists()
            }
        }

    }


    fun deleteSuIfExists() {

        ShellUtils.fastCmd("mount -o rw,remount /")

        val checkCommand = "if [ -f /system/xbin/su ]; then echo 'exists'; fi"
        val result = ShellUtils.fastCmd(checkCommand).trim()

        if (result == "exists") {
            val deleteCommand = "rm -f /system/xbin/su"
            val deleteResult = ShellUtils.fastCmdResult(deleteCommand)
            println("Delete command output: $deleteResult")

            if (deleteResult) {
                println("/system/xbin/su has been deleted.")
            } else {
                println("Failed to delete /system/xbin/su. Error code: $deleteResult")
            }
        } else {
            println("/system/xbin/su does not exist.")
        }

    }


    fun createSU() {

        ShellUtils.fastCmd("mount -o rw,remount /")

        val checkCommand = "if [ -f /system/xbin/su ]; then echo 'exists'; fi"
        val result = ShellUtils.fastCmd(checkCommand).trim()

        if (result == "exists") {

        } else {

            val createCommand = "cd /system/xbin && ln -sf ./cusu su 2>&1"
            val createResult = ShellUtils.fastCmd(createCommand).trim()
            println("Create command output: $createResult")

            if (createResult.isNotEmpty()) {
                println("/system/xbin/su has been created.")
            } else {
                println("Failed to create /system/xbin/su. Error details: $createResult")
            }

        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun setDisplayHomeAsUpEnabled(isEnabled: Boolean) {
        binding.mainToolbar.startAnimations()
        when {
            isEnabled -> binding.mainToolbar.setNavigationIcon(R.drawable.ic_back_md2)
            else -> binding.mainToolbar.navigationIcon = null
        }
    }

    internal fun requestNavigationHidden(hide: Boolean = true, requiresAnimation: Boolean = true) {
        val bottomView = binding.mainNavigation
        if (requiresAnimation) {
            bottomView.isVisible = true
            bottomView.isHidden = hide
        } else {
            bottomView.isGone = hide
        }
    }

    fun invalidateToolbar() {
        //binding.mainToolbar.startAnimations()
        binding.mainToolbar.invalidate()
    }

    private fun getScreen(name: String?): NavDirections? {
        return when (name) {
            Const.Nav.SUPERUSER -> MainDirections.actionSuperuserFragment()
            Const.Nav.MODULES -> MainDirections.actionModuleFragment()
            Const.Nav.SETTINGS -> HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
            else -> null
        }
    }

    private fun getScreen(id: Int): NavDirections? {
        return when (id) {
            R.id.homeFragment -> MainDirections.actionHomeFragment()
            R.id.modulesFragment -> MainDirections.actionModuleFragment()
            R.id.superuserFragment -> MainDirections.actionSuperuserFragment()
            R.id.logFragment -> MainDirections.actionLogFragment()
            else -> null
        }
    }

    private fun showUnsupportedMessage() {
        if (Info.env.isUnsupported) {
            MagiskDialog(this).apply {
                setTitle(R.string.unsupport_magisk_title)
                setMessage(R.string.unsupport_magisk_msg, Const.Version.MIN_VERSION)
                setButton(MagiskDialog.ButtonType.POSITIVE) { text = android.R.string.ok }
                setCancelable(false)
            }.show()
        }

        if (!Info.isEmulator && Info.env.isActive && System.getenv("PATH")
                ?.split(':')
                ?.filterNot { File("$it/magisk").exists() }
                ?.any { File("$it/su").exists() } == true
        ) {
            toast("安装成功", Toast.LENGTH_SHORT)
//            MagiskDialog(this).apply {
//                setTitle(R.string.unsupport_general_title)
//                setMessage(R.string.unsupport_other_su_msg)
//                setButton(MagiskDialog.ButtonType.POSITIVE) { text = android.R.string.ok }
//                setCancelable(false)
//            }.show()
        }

        if (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            MagiskDialog(this).apply {
                setTitle(R.string.unsupport_general_title)
                setMessage(R.string.unsupport_system_app_msg)
                setButton(MagiskDialog.ButtonType.POSITIVE) { text = android.R.string.ok }
                setCancelable(false)
            }.show()
        }

        if (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0) {
            MagiskDialog(this).apply {
                setTitle(R.string.unsupport_general_title)
                setMessage(R.string.unsupport_external_storage_msg)
                setButton(MagiskDialog.ButtonType.POSITIVE) { text = android.R.string.ok }
                setCancelable(false)
            }.show()
        }
    }

    private fun askForHomeShortcut() {
        if (isRunningAsStub && !Config.askedHome &&
            ShortcutManagerCompat.isRequestPinShortcutSupported(this)
        ) {
            // Ask and show dialog
            Config.askedHome = true
            MagiskDialog(this).apply {
                setTitle(R.string.add_shortcut_title)
                setMessage(R.string.add_shortcut_msg)
                setButton(MagiskDialog.ButtonType.NEGATIVE) {
                    text = android.R.string.cancel
                }
                setButton(MagiskDialog.ButtonType.POSITIVE) {
                    text = android.R.string.ok
                    onClick {
                        Shortcuts.addHomeIcon(this@MainActivity)
                    }
                }
                setCancelable(true)
            }.show()
        }
    }

    @SuppressLint("InlinedApi")
    private fun checkStubComponent() {
        if (intent.component?.className?.contains(HideAPK.PLACEHOLDER) == true) {
            // The stub APK was not properly patched, re-apply our changes
            withPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES) { granted ->
                if (granted) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val apk = File(applicationInfo.sourceDir)
                        HideAPK.upgrade(this@MainActivity, apk)?.let {
                            startActivity(it)
                        }
                    }
                }
            }
        }
    }

}
