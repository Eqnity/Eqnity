package com.eqnitymusic.damn.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.databinding.ActivityMainBinding
import com.eqnitymusic.damn.service.MediaPlaybackService
import com.eqnitymusic.damn.ui.player.PlayerViewModel
import com.eqnitymusic.damn.util.showSnackbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private var playbackService: MediaPlaybackService? = null
    private var isServiceBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupToolbar()
        setupBottomNavigation()
        setupViewPager()
        setupMiniPlayer()
        setupPermissions()
        setupBackPress()
        bindPlaybackService()
        playerViewModel.playbackService = playbackService
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            
            binding.appBar.setPadding(0, statusBarHeight, 0, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, navBarHeight)
            
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupBottomNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.view_pager) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun setupViewPager() {
        // ViewPager2 is handled by Navigation
    }

    private fun setupMiniPlayer() {
        binding.miniPlayer.setOnClickListener {
            expandPlayer()
        }

        binding.miniPlayerDragHandle.setOnClickListener {
            expandPlayer()
        }
    }

    private fun setupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 100)
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.fullPlayerContainer.visibility == View.VISIBLE) {
                    collapsePlayer()
                } else if (binding.miniPlayer.visibility == View.VISIBLE) {
                    // Don't close app, just minimize
                    moveTaskToBack(true)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun bindPlaybackService() {
        val intent = Intent(this, MediaPlaybackService::class.java)
        val connection = object : android.content.ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {
                val binder = service as MediaPlaybackService.MediaPlaybackBinder
                playbackService = binder.getService()
                isServiceBound = true
                playerViewModel.playbackService = playbackService
            }

            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                isServiceBound = false
                playbackService = null
            }
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        if (isServiceBound) {
            unbindService(object : android.content.ServiceConnection {
                override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {}
                override fun onServiceDisconnected(name: android.content.ComponentName?) {}
            })
        }
        super.onDestroy()
    }

    fun playTracks(tracks: List<com.eqnitymusic.damn.data.model.MusicTrack>, startIndex: Int) {
        playbackService?.setPlaylist(tracks, startIndex)
        showMiniPlayer()
    }

    private fun showMiniPlayer() {
        playbackService?.let { service ->
            val track = service.getCurrentTrack()
            track?.let {
                binding.miniPlayer.visibility = View.VISIBLE
                binding.miniPlayerTitle.text = it.getDisplayTitle()
                binding.miniPlayerArtist.text = it.getDisplayArtist()
                
                // Load album art
                com.bumptech.glide.Glide.with(this)
                    .load(it.artworkPath)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(binding.miniPlayerArt)

                updatePlayPauseButton(service.isPlaying())
            }
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.miniPlayerPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun expandPlayer() {
        binding.fullPlayerContainer.visibility = View.VISIBLE
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.view_pager) as NavHostFragment
        val playerFragment = PlayerFragment()
        navHostFragment.childFragmentManager.beginTransaction()
            .replace(R.id.full_player_container, playerFragment)
            .commit()
        
        // Animate
        binding.fullPlayerContainer.alpha = 0f
        binding.fullPlayerContainer.animate().alpha(1f).setDuration(300).start()
    }

    fun collapsePlayer() {
        binding.fullPlayerContainer.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction { binding.fullPlayerContainer.visibility = View.GONE }
            .start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Navigate to settings
                true
            }
            R.id.action_about -> {
                // Show about dialog
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}