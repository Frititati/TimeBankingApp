package it.polito.timebanking.ui.offers

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import it.polito.timebanking.R
import it.polito.timebanking.databinding.FragmentOfferDetailBinding
import it.polito.timebanking.model.chat.JobData
import it.polito.timebanking.model.profile.ageFormatter
import it.polito.timebanking.model.profile.descriptionFormatterProfile
import it.polito.timebanking.model.profile.fullNameFormatter
import it.polito.timebanking.model.timeslot.*
import it.polito.timebanking.ui.messages.JobStatus
import java.text.DecimalFormat

class OfferDetailFragment : Fragment() {
    private val vmTimeslot by viewModels<TimeslotViewModel>()
    private var _binding: FragmentOfferDetailBinding? = null
    private val binding get() = _binding!!
    private val firebaseUserID = FirebaseAuth.getInstance().uid!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfferDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val idTimeslot = requireArguments().getString("id_timeslot")!!
        val otherUserID = requireArguments().getString("id_user")!!

        if (otherUserID == firebaseUserID) Toast.makeText(context, "Your own offer!", Toast.LENGTH_SHORT).show()
        FirebaseFirestore.getInstance().collection("users").document(otherUserID).get().addOnSuccessListener { user ->
            binding.UserFullName.text = fullNameFormatter(user.getString("fullName"), false)
            binding.UserAge.text = ageFormatter(user.getLong("age"), false)
            binding.UserDescription.text = descriptionFormatterProfile(user.getString("description"), false)
            val score = user.getDouble("scoreAsProducer") ?: .0
            val jobsRated = user.getLong("jobsRatedAsProducer") ?: 0
            if (jobsRated != 0L) {
                binding.userRating.text = DecimalFormat("#.0").format(score / jobsRated.toDouble()).toString()
            }
            vmTimeslot.get(idTimeslot).observe(viewLifecycleOwner) {
                binding.Title.text = titleFormatter(it.title)
                binding.Description.text = descriptionFormatter(it.description)
                binding.Date.text = dateFormatter(it.date)
                binding.Duration.text = durationMinuteFormatter(it.duration)
                binding.Location.text = locationFormatter(it.location)
            }
        }.addOnFailureListener { e -> Log.w("warn", "Error with users $e") }

        binding.chatStartButton.isVisible = firebaseUserID != otherUserID

        binding.chatStartButton.setOnClickListener {
            var jobExists = false
            FirebaseFirestore.getInstance().collection("jobs").whereEqualTo("timeslotID", idTimeslot).whereArrayContains("users", firebaseUserID).get().addOnSuccessListener { ext ->
                ext.forEach {
                    if (it.getString("jobStatus") != JobStatus.COMPLETED.toString() && it.getString("jobStatus") != JobStatus.REJECTED.toString()) {
                        jobExists = true
                    }
                }
                if (!jobExists) {
                    val jobData = JobData(idTimeslot, emptyList<String>(), System.currentTimeMillis(), otherUserID, firebaseUserID, listOf(otherUserID, firebaseUserID), JobStatus.INIT, "", "",false,false)
                    FirebaseFirestore.getInstance().collection("jobs").add(jobData).addOnSuccessListener {
                        FirebaseFirestore.getInstance().collection("users").document(otherUserID).update("activeProducingJobs",FieldValue.increment(1))
                        FirebaseFirestore.getInstance().collection("users").document(firebaseUserID).update("activeConsumingJobs",FieldValue.increment(1))
                        findNavController().navigate(R.id.offer_to_job, bundleOf("otherUserName" to binding.UserFullName.text, "jobID" to it.id))
                    }.addOnFailureListener { e -> Log.w("warn", "Error with jobs $e") }
                }
                else {
                    val chat = ext.first()
                    findNavController().navigate(R.id.offer_to_job, bundleOf("otherUserName" to binding.UserFullName.text, "jobID" to chat.id))
                }
            }.addOnFailureListener { e -> Log.w("warn", "Error with users $e") }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}