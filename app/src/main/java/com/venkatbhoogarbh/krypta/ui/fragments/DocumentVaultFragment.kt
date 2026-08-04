package com.venkatbhoogarbh.krypta.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.venkatbhoogarbh.krypta.R
import com.venkatbhoogarbh.krypta.ui.adapter.DocumentAdapter
import com.venkatbhoogarbh.krypta.ui.viewmodel.DocumentViewModel
import com.venkatbhoogarbh.krypta.util.showDocumentDeleteConfirmation

class DocumentVaultFragment : Fragment() {

    private lateinit var viewModel: DocumentViewModel
    private lateinit var adapter: DocumentAdapter

    private val getFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.importDocument(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_document_vault, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[DocumentViewModel::class.java]

        setupRecyclerView(view)
        setupSearchView(view)

        val importFab = view.findViewById<ExtendedFloatingActionButton>(R.id.import_document_fab)
        importFab.setOnClickListener {
            getFileLauncher.launch("*/*")
        }

        viewModel.allDocuments.observe(viewLifecycleOwner) { documents ->
            val emptyState = view.findViewById<LinearLayout>(R.id.document_empty_state)
            val recyclerView = view.findViewById<RecyclerView>(R.id.document_recycler_view)
            
            if (documents.isEmpty()) {
                val isSearching = !viewModel.searchQuery.value.isNullOrEmpty()
                updateEmptyState(isSearching, view)
                emptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.submitList(documents)
            }
        }

        viewModel.importStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is DocumentViewModel.ImportStatus.Success -> {
                    Toast.makeText(context, "Document imported successfully", Toast.LENGTH_SHORT).show()
                    viewModel.resetImportStatus()
                }
                is DocumentViewModel.ImportStatus.Error -> {
                    Toast.makeText(context, "Error: ${status.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetImportStatus()
                }
                else -> {}
            }
        }

        viewModel.deleteStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is DocumentViewModel.DeleteStatus.Success -> {
                    Toast.makeText(context, R.string.document_deleted, Toast.LENGTH_SHORT).show()
                    viewModel.resetDeleteStatus()
                }
                is DocumentViewModel.DeleteStatus.Error -> {
                    Toast.makeText(context, "Error: ${status.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetDeleteStatus()
                }
                else -> {}
            }
        }
    }

    private fun setupSearchView(view: View) {
        val searchView = view.findViewById<com.google.android.material.search.SearchView>(R.id.document_search_view)
        searchView.editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun updateEmptyState(isSearching: Boolean, view: View) {
        val icon = view.findViewById<android.widget.ImageView>(R.id.document_empty_state_icon)
        val title = view.findViewById<TextView>(R.id.document_empty_state_title)
        val description = view.findViewById<TextView>(R.id.document_empty_state_description)

        if (isSearching) {
            icon.setImageResource(android.R.drawable.ic_menu_search)
            title.text = getString(R.string.no_matches_title)
            description.text = getString(R.string.no_matches_description)
        } else {
            icon.setImageResource(android.R.drawable.ic_menu_agenda)
            title.text = getString(R.string.empty_documents_title)
            description.text = getString(R.string.empty_documents_description)
        }
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.document_recycler_view)
        adapter = DocumentAdapter(
            onItemClick = { entry ->
                val bundle = Bundle().apply { putLong("documentId", entry.id) }
                findNavController().navigate(R.id.action_document_vault_to_details, bundle)
            },
            onDeleteClick = { entry ->
                showDocumentDeleteConfirmation {
                    viewModel.deleteDocument(entry)
                }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
}
