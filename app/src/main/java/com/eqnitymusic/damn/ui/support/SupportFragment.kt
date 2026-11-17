package com.eqnitymusic.damn.ui.support

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
        binding.rvSkills.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSkills.adapter = skillAdapter

        binding.btnRetrySupport.setOnClickListener { viewModel.loadInfo() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.supportLoadingContainer.visibility =
                            if (loading) View.VISIBLE else View.GONE
                        binding.supportContent.visibility = if (loading) View.GONE else View.VISIBLE
                        binding.supportErrorContainer.visibility =
                            if (!loading && viewModel.error.value != null && viewModel.info.value == null) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }
                }
                launch {
                    viewModel.info.collect { info ->
                        info ?: return@collect
                        populateInfo(info)
                    }
                }
                launch {
                    viewModel.error.collect { error ->
                        binding.supportErrorContainer.visibility =
                            if (viewModel.info.value == null && !viewModel.isLoading.value && error != null) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }
                }
            }
        }
    }

    private fun populateInfo(info: com.eqnitymusic.damn.data.network.EqnityInfoResponse) {
        Glide.with(this)
            .load("https://stormmc.top/eqnity.png")
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .apply(RequestOptions().transform(RoundedCorners(24)))
            .into(binding.supportAppLogo)

        binding.supportAppName.text = info.name
        binding.supportAppTagline.text = info.tagline
        binding.supportAppVersion.text = getString(R.string.support_version, "6.7")
        binding.supportAboutContent.text = info.about
        skillAdapter.submitList(info.skills)

        binding.contactGithubCard.setOnClickListener {
            if (info.links.github.isNotBlank()) openUrl(info.links.github)
        }
        binding.contactDiscordCard.setOnClickListener {
            if (info.links.discord.isNotBlank()) openUrl(info.links.discord)
        }
        binding.tvContactGithub.text = info.contact.github
        binding.tvContactDiscord.text = info.contact.discord
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}