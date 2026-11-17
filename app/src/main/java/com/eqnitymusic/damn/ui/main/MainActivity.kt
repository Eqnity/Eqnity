package com.eqnitymusic.damn.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.data.model.MusicTrack
import com.eqnitymusic.damn.databinding.ActivityMainBinding
import com.eqnitymusic.damn.service.MediaPlaybackService
import com.eqnitymusic.damn.ui.home.HomeFragment
import com.eqnitymusic.damn.ui.playlists.AddSongsFragment
import com.eqnitymusic.damn.ui.playlists.PlaylistDetailFragment
import com.eqnitymusic.damn.ui.playlists.PlaylistsFragment
import com.eqnitymusic.damn.ui.player.PlayerFragment
import com.eqnitymusic.damn.ui.support.SupportFragment
import com.eqnitymusic.damn.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var homeFragment: HomeFragment? = null
    private var playlistsFragment: PlaylistsFragment? = null
    private var supportFragment: SupportFragment? = null
    private var playerJob: Job? = null
    private var lastTrackId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupWindowInsets()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setupBottomNavigation()
        setupMiniPlayer()
        setupPermissions()
        setupBackPress()

        if (savedInstanceState == null) {
            selectTab(R.id.nav_home)
        }
        startPlayerUpdates()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            binding.appBar.setPadding(0, statusBar, 0, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, navBar)
            insets
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            selectTab(item.itemId)
            true
        }
    }

    private fun selectTab(menuId: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        supportFragmentManager.fragments.forEach { transaction.hide(it) }

        when (menuId) {
            R.id.nav_home -> {
                val fragment = homeFragment ?: HomeFragment().also { homeFragment = it }
                if (!fragment.isAdded) transaction.add(R.id.nav_host_container, fragment) else transaction.show(fragment)
                binding.toolbarTitle.text = getString(R.string.app_name)
            }
            R.id.nav_playlists -> {
                val fragment = playlistsFragment ?: PlaylistsFragment().also { playlistsFragment = it }
                if (!fragment.isAdded) transaction.add(R.id.nav_host_container, fragment) else transaction.show(fragment)
                binding.toolbarTitle.text = getString(R.string.tab_playlists)
            }
            else -> {
                val fragment = supportFragment ?: SupportFragment().also { supportFragment = it }
                if (!fragment.isAdded) transaction.add(R.id.nav_host_container, fragment) else transaction.show(fragment)
                binding.toolbarTitle.text = getString(R.string.tab_support)
            }
        }
        transaction.commit()
        binding.bottomNavigation.menu.findItem(menuId)?.isChecked = true
    }

    private fun setupMiniPlayer() {
        binding.miniPlayer.setOnClickListener { expandPlayer() }
        binding.miniPlayerDragHandle.setOnClickListener { expandPlayer() }
        binding.miniPlayerPlayPause.setOnClickListener {
            MediaPlaybackService.getInstance()?.playPause()
            renderMiniPlayer()
        }
        binding.miniPlayerNext.setOnClickListener {
            MediaPlaybackService.getInstance()?.next()
            renderMiniPlayer()
        }
    }

    private fun startPlayerUpdates() {
        playerJob = lifecycleScope.launch {
            while (isActive) {
                renderMiniPlayer()
                delay(500)
            }
        }
    }

    private fun renderMiniPlayer() {
        val service = MediaPlaybackService.getInstance()
        val track = service?.currentTrack
        if (track == null) {
            binding.miniPlayer.visibility = View.GONE
            return
        }
        binding.miniPlayer.visibility = View.VISIBLE
        if (track.id != lastTrackId) {
            lastTrackId = track.id
            binding.miniPlayerTitle.text = track.getDisplayTitle()
            binding.miniPlayerArtist.text = track.getDisplayArtist()
            Glide.with(this)
                .load(track.artworkPath)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(binding.miniPlayerArt)
        }
        binding.miniPlayerPlayPause.setImageResource(
            if (service.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    fun playTracks(tracks: List<MusicTrack>, startIndex: Int) {
        MediaPlaybackService.playTracks(this, tracks, startIndex)
        binding.miniPlayer.visibility = View.VISIBLE
    }

    fun openPlaylistDetail(playlistId: Long) {
        val fragment = PlaylistDetailFragment.newInstance(playlistId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun openAddSongs(playlistId: Long) {
        val fragment = AddSongsFragment.newInstance(playlistId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun expandPlayer() {
        if (binding.fullPlayerContainer.visibility == View.VISIBLE) return
        binding.fullPlayerContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.fullPlayerContainer, PlayerFragment())
            .commit()
        binding.fullPlayerContainer.alpha = 0f
        binding.fullPlayerContainer.animate().alpha(1f).setDuration(200).start()
    }

    fun collapsePlayer() {
        binding.fullPlayerContainer.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.fullPlayerContainer.visibility = View.GONE
                supportFragmentManager.findFragmentById(R.id.fullPlayerContainer)?.let {
                    supportFragmentManager.beginTransaction().remove(it).commit()
                }
            }
            .start()
    }

    private fun setupPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.fullPlayerContainer.visibility == View.VISIBLE -> collapsePlayer()
                    supportFragmentManager.backStackEntryCount > 0 -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                    binding.miniPlayer.visibility == View.VISIBLE -> moveTaskToBack(true)
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.about_app)
                    .setMessage(getString(R.string.about_version) + "\n" + getString(R.string.about_developer))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        playerJob?.cancel()
        super.onDestroy()
    }
}