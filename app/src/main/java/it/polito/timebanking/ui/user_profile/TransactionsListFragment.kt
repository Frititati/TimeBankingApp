package it.polito.timebanking.ui.user_profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import it.polito.timebanking.R
import it.polito.timebanking.databinding.FragmentTransactionsListBinding
import it.polito.timebanking.model.transaction.TransactionViewModel

class TransactionsListFragment : Fragment() {
    private var _binding: FragmentTransactionsListBinding? = null
    private val binding get() = _binding!!
    private var transactionListAdapter = TransactionListAdapter()
    private val firebaseUserID = FirebaseAuth.getInstance().uid!!
    private val transactionVM by viewModels<TransactionViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout).setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        binding.transactionListRecycler.layoutManager = LinearLayoutManager(activity)
        binding.transactionListRecycler.adapter = transactionListAdapter

        transactionVM.getTransactions(firebaseUserID).observe(viewLifecycleOwner) {
            transactionListAdapter.setTransactions(it.toMutableList())
            if (it.isNotEmpty()) {
                binding.nothingToShow.visibility = View.GONE
            }
        }
    }
}