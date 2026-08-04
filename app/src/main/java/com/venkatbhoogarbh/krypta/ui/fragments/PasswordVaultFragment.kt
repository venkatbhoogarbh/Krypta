package com.venkatbhoogarbh.krypta.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.venkatbhoogarbh.krypta.R
import com.venkatbhoogarbh.krypta.ui.adapter.PasswordAdapter
import com.venkatbhoogarbh.krypta.ui.viewmodel.PasswordViewModel
import com.venkatbhoogarbh.krypta.util.ClipboardHelper
import com.venkatbhoogarbh.krypta.util.SessionManager
import com.venkatbhoogarbh.krypta.util.showDeleteConfirmation

class PasswordVaultFragment : Fragment() {

    private lateinit var viewModel: PasswordViewModel
    private lateinit var adapter: PasswordAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_password_vault, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]
        
        setupRecyclerView(view)
        setupSearchView(view)

        val fab = view.findViewById<ExtendedFloatingActionButton>(R.id.add_password_fab)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_vault_to_add)
        }

        val lockButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.manual_lock_button)
        lockButton.setOnClickListener {
            SessionManager.lock()
        }

        viewModel.allPasswords.observe(viewLifecycleOwner) { entries ->
            if (entries.isEmpty()) {
                val isSearching = !viewModel.searchQuery.value.isNullOrEmpty()
                updateEmptyState(isSearching, view)
                showState(VaultState.EMPTY, view)
            } else {
                showState(VaultState.SUCCESS, view)
                adapter.submitList(entries)
            }
        }
    }

    private fun updateEmptyState(isSearching: Boolean, view: View) {
        val icon = view.findViewById<TextView>(R.id.empty_state_icon)
        val title = view.findViewById<TextView>(R.id.empty_state_title)
        val description = view.findViewById<TextView>(R.id.empty_state_description)

        if (isSearching) {
            icon.text = "🔍"
            title.text = getString(R.string.no_matches_title)
            description.text = getString(R.string.no_matches_description)
        } else {
            icon.text = "🔐"
            title.text = getString(R.string.empty_vault_title)
            description.text = getString(R.string.empty_vault_description)
        }
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.password_recycler_view)
        adapter = PasswordAdapter(
            onItemClick = { entry ->
                val bundle = Bundle().apply { putLong("entryId", entry.id) }
                findNavController().navigate(R.id.action_vault_to_details, bundle)
            },
            onEditClick = { entry ->
                val bundle = Bundle().apply { putLong("entryId", entry.id) }
                findNavController().navigate(R.id.action_vault_to_add, bundle)
            },
            onDeleteClick = { entry ->
                showDeleteConfirmation {
                    viewModel.delete(entry)
                }
            },
            onCopyPasswordClick = { entry ->
                ClipboardHelper.copyToClipboard(requireContext(), entry.password, R.string.password_copied)
            },
            onCopyUsernameClick = { entry ->
                ClipboardHelper.copyToClipboard(requireContext(), entry.username, R.string.username_copied)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupSearchView(view: View) {
        val searchView = view.findViewById<com.google.android.material.search.SearchView>(R.id.search_view)
        searchView.editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun showState(state: VaultState, view: View) {
        val loading = view.findViewById<CircularProgressIndicator>(R.id.loading_indicator)
        val empty = view.findViewById<LinearLayout>(R.id.empty_state_container)
        val recycler = view.findViewById<RecyclerView>(R.id.password_recycler_view)

        loading.visibility = if (state == VaultState.LOADING) View.VISIBLE else View.GONE
        empty.visibility = if (state == VaultState.EMPTY) View.VISIBLE else View.GONE
        recycler.visibility = if (state == VaultState.SUCCESS) View.VISIBLE else View.GONE
    }

    enum class VaultState {
        LOADING, EMPTY, SUCCESS
    }
}
