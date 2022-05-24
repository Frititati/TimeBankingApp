package it.polito.timebanking.ui.offers

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import it.polito.timebanking.R
import it.polito.timebanking.databinding.FragmentOfferDetailBinding
import it.polito.timebanking.model.chat.ChatData
import it.polito.timebanking.model.profile.ProfileViewModel
import it.polito.timebanking.model.profile.ageFormatter
import it.polito.timebanking.model.profile.fullNameFormatter
import it.polito.timebanking.model.timeslot.*

class OfferDetailFragment : Fragment() {
    private val timeslotVM by viewModels<TimeslotViewModel>()
    private var _binding: FragmentOfferDetailBinding? = null
    private var favList = mutableListOf<String>()
    private val binding get() = _binding!!
    private var fav = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfferDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val idTimeslot = requireArguments().getString("id_timeslot")!!
        val idUser = requireArguments().getString("id_user")
        FirebaseFirestore.getInstance().collection("users")
            .document(idUser!!).get().addOnSuccessListener { user ->
                binding.UserFullName.text = fullNameFormatter(user.get("fullName").toString())
                binding.UserAge.text = ageFormatter(user.get("age").toString())
                binding.UserDescription.text =
                    descriptionFormatter(user.get("description").toString())

                timeslotVM.get(idTimeslot).observe(viewLifecycleOwner) {
                    binding.Title.text = titleFormatter(it.title)
                    binding.Description.text = descriptionFormatter(it.description)
                    binding.Date.text = dateFormatter(it.date)
                    binding.Duration.text = durationMinuteFormatter(resources, it.duration)
                    binding.Location.text = locationFormatter(it.location)
                }
            }
        FirebaseFirestore.getInstance().collection("users")
            .document(FirebaseAuth.getInstance().currentUser!!.uid).get().addOnSuccessListener {
                val myList = it.get("favorites") as MutableList<*>
                favList = myList.map { f -> f.toString() }.toMutableList()
                if (favList.contains(idTimeslot)) {
                    fav = true
                    requireActivity().invalidateOptionsMenu()
                }
            }

        binding.chatStartButton.isVisible = FirebaseAuth.getInstance().currentUser!!.uid != idUser

        binding.chatStartButton.setOnClickListener {
            val data = ChatData(
                listOf(
                    FirebaseAuth.getInstance().currentUser!!.uid,
                    requireArguments().getString("id_user")!!
                ),
                0L,
                emptyList<String>(), idTimeslot
            )
            FirebaseFirestore.getInstance().collection("chats").add(data).addOnSuccessListener {
                findNavController().navigate(
                    R.id.ad_to_chat,
                    bundleOf(
                        "user" to binding.UserFullName.text,
                        "chatID" to it.id,
                        "timeslotID" to idTimeslot
                    )
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (fav) {
            menu.findItem(R.id.action_fav_on).isVisible = true
            menu.findItem(R.id.action_fav_off).isVisible = false
        } else {
            menu.findItem(R.id.action_fav_on).isVisible = false
            menu.findItem(R.id.action_fav_off).isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_fav_off -> {
                fav = true
                requireActivity().invalidateOptionsMenu()
                favList.add(requireArguments().getString("id_timeslot").toString())
                updateUserData(favList)
                Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
            }
            R.id.action_fav_on -> {
                fav = false
                requireActivity().invalidateOptionsMenu()
                favList.remove(requireArguments().getString("id_timeslot").toString())
                updateUserData(favList)
                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUserData(newFavList: List<String>) {
        val vm by viewModels<ProfileViewModel>()
        vm.get(FirebaseAuth.getInstance().currentUser!!.uid).observe(viewLifecycleOwner) {
            vm.update(
                FirebaseAuth.getInstance().currentUser!!.uid,
                it.fullName,
                it.nickName,
                it.age.toString(),
                it.email,
                it.location,
                it.description,
                newFavList
            )
        }
    }
}