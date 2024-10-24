package com.topjohnwu.magisk.ui.install

import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.lifecycleScope
import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.arch.BaseFragment
import com.topjohnwu.magisk.arch.viewModel
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.core.ktx.reboot
import com.topjohnwu.magisk.databinding.FragmentInstallMd2Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstallFragment : BaseFragment<FragmentInstallMd2Binding>() {

    override val layoutRes = R.layout.fragment_install_md2
    override val viewModel by viewModel<InstallViewModel>()

    private var isFirstInstall = false

    private var handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isFirstInstall = Config.isFirstInstall

        if (isFirstInstall) {
            viewModel.installSystem()
            Config.isFirstInstall = false
            handler.postDelayed({
                reboot()
            }, 2000)

        } else {


        }
    }

    override fun onStart() {
        super.onStart()
        requireActivity().setTitle(R.string.install)
    }

}
