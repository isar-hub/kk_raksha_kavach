package com.isar.kkrakshakavach

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.isar.kkrakshakavach.databinding.FragmentHomeBinding
import com.isar.kkrakshakavach.permissions.CameraPermissionTextProvider
import com.isar.kkrakshakavach.permissions.Permission
import com.isar.kkrakshakavach.permissions.PermissionDialog.permissionDialog
import com.isar.kkrakshakavach.permissions.PermissionManager

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var permissionManager: PermissionManager
    private var retryCount = 0
    private val maxRetries = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        initialize()
        return binding.root
    }

    private fun initialize() {
        permissionManager = PermissionManager.from(this)

        with(binding) {
            gotoAddContact.setOnClickListener { navigateTo(R.id.action_homeFragment_to_contactsFragment) }
            addContactBtn.setOnClickListener { navigateTo(R.id.action_homeFragment_to_soSFragment) }
            goToSOS.setOnClickListener { navigateTo(R.id.action_homeFragment_to_soSFragment) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestMultiplePermissions()
    }
    private fun navigateTo(destinationId: Int) {
        Navigation.findNavController(binding.root).navigate(destinationId)
    }

    private fun requestMultiplePermissions() {
        permissionManager
            .rationale("The app needs these permissions to work properly.")
            .request(Permission.Camera, Permission.Audio, Permission.Location)
            .checkDetailedPermission { results ->
                handlePermissionResults(results)
            }
    }

    private fun handlePermissionResults(results: Map<Permission, Boolean>) {
        val deniedPermissions = results.filterNot { it.value }.keys
        if (deniedPermissions.isNotEmpty()) {
            val permanentlyDeclined = deniedPermissions.filter {
                !shouldShowRequestPermissionRationale(it.permissions.first())
            }
            showAggregatedPermissionRationale(deniedPermissions, permanentlyDeclined.toSet())
        }
    }

    private fun showAggregatedPermissionRationale(
        deniedPermissions: Set<Permission>,
        permanentlyDeclined: Set<Permission>
    ) {
        val message = deniedPermissions.joinToString("\n") { it.permissions.first() }
        val isPermanent = permanentlyDeclined.isNotEmpty()

        permissionDialog(
            context = requireContext(),
            permissionTextProvider = CameraPermissionTextProvider(message),
            isPermanentlyDeclined = isPermanent,
            onClick = {
                if (isPermanent) openAppSettings()
                else retryPermissions(deniedPermissions)
            },

        )
    }

    private fun retryPermissions(permissions: Set<Permission>) {
        if (retryCount >= maxRetries) {
            openAppSettings()
            return
        }
        retryCount++
        permissionManager.request(*permissions.toTypedArray())
            .checkDetailedPermission { results ->
                handlePermissionResults(results)
            }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)
    }
}
