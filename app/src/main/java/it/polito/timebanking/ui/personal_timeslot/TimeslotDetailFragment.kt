package it.polito.timebanking.ui.personal_timeslot

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import it.polito.timebanking.R
import it.polito.timebanking.model.timeslot.TimeslotViewModel
import it.polito.timebanking.databinding.FragmentTimeslotDetailBinding
import it.polito.timebanking.model.timeslot.*

class TimeslotDetailFragment : Fragment() {
    private val timeslotVM by viewModels<TimeslotViewModel>()
    private var _binding: FragmentTimeslotDetailBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        _binding = FragmentTimeslotDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val idTimeslot = requireArguments().getString("id_timeslot")!!
        timeslotVM.get(idTimeslot).observe(viewLifecycleOwner) {
            binding.Title.text = titleFormatter(it.title)
            binding.Description.text = descriptionFormatter(it.description)
            binding.Date.text = dateFormatter(it.date)
            binding.Duration.text = durationMinuteFormatter(it.duration)
            binding.Location.text = locationFormatter(it.location)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_settings).isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val idTimeslot: String? = requireArguments().getString("id_timeslot")
                findNavController().navigate(R.id.details_to_edit, bundleOf("id_timeslot" to idTimeslot))
                Snackbar.make(binding.root, "Remember to bind with your own skills", 1500).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

