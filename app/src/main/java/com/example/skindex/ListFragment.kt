package com.example.skindex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skindex.data.PreferencesManager
import com.example.skindex.databinding.ListFragmentBinding
import com.example.skindex.ui.list.ListViewModel
import com.example.skindex.ui.list.PostAdapter
import com.example.skindex.ui.list.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ListFragment : Fragment() {

    private var _binding: ListFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ListViewModel by viewModels()
    private lateinit var adapter: PostAdapter

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ListFragmentBinding.inflate(inflater, container, false)
        setupRecyclerView()
        setupLoadButton()
        observeUiState()
        return binding.root
    }

    private fun setupLoadButton() {
        binding.loadButton.setOnClickListener {
            viewModel.fetchPosts()
        }
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter { postId ->
            findNavController().navigate(
                ListFragmentDirections.actionListToDetail(postId)
            )
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@ListFragment.adapter
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        adapter.submitList(state.posts)
                        if (state.posts.isEmpty()) {
                            Toast.makeText(context, "Список постів порожній", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.recyclerView.visibility = View.GONE
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}