package com.example.skindex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.skindex.databinding.DetailFragmentBinding
import com.example.skindex.ui.detail.DetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailFragment : Fragment() {

    private var _binding: DetailFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DetailFragmentBinding.inflate(inflater, container, false)
        val postId = arguments?.getInt("postId") ?: 1
        viewModel.fetchPost(postId)
        observePost()
        return binding.root
    }

    private fun observePost() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.post.collectLatest { post ->
                post?.let {
                    binding.detailText.text = "${it.title}\n\n${it.body}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}