package com.eqnitymusic.damn.ui.support

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.eqnitymusic.damn.R
import com.eqnitymusic.damn.databinding.FragmentSupportBinding
import com.eqnitymusic.damn.ui.adapter.SkillAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SupportFragment : Fragment() {

    private var _binding: FragmentSupportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SupportViewModel by viewModels()
    private val skillAdapter = SkillAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.rvSkills.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = skillAdapter
        }
    }

    private fun observeData() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.supportLoadingContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.supportErrorContainer.visibility = View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null && !viewModel.isLoading.value) {
                binding.supportLoadingContainer.visibility = View.GONE
                binding.supportErrorContainer.visibility = View.VISIBLE
            }
        }

        viewModel.info.observe(viewLifecycleOwner) { info ->
            if (info != null) {
                populateInfo(info)
            }
        }
    }

    private fun populateInfo(info: com.eqnitymusic.damn.data.network.EqnityInfoResponse) {
        // Load app logo from stormmc.top
        Glide.with(this)
            .load("https://stormmc.top/eqnity.png")
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .apply(RequestOptions().transform(RoundedCorners(24)))
            .into(binding.supportAppLogo)

        binding.supportAppName.text = info.name
        binding.supportAppVersion.text = getString(R.string.support_version, "6.7")
        binding.supportAboutContent.text = info.about

        skillAdapter.submitList(info.skills)

        // Contact cards
        binding.contactGithubCard.setOnClickListener {
            val url = info.links.github
            if (url.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        binding.contactDiscordCard.setOnClickListener {
            val url = info.links.discord
            if (url.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnRetrySupport.setOnClickListener {
            viewModel.loadInfo()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}