package it.polito.timebanking.ui.messages

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import it.polito.timebanking.R
import it.polito.timebanking.model.chat.JobData
import it.polito.timebanking.model.chat.toJobData
import it.polito.timebanking.model.profile.toUserProfileData
import it.polito.timebanking.model.timeslot.toTimeslotData
import java.text.SimpleDateFormat
import java.util.*

class ConsumingJobsAdapter : RecyclerView.Adapter<ConsumingJobsAdapter.ChatListViewHolder>() {
    private var allJobs = mutableListOf<Pair<String, JobData>>()

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ChatListViewHolder {
        return ChatListViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.widget_jobs_list, parent, false))
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        holder.bind(allJobs[position].first, allJobs[position].second,holder.itemView.context)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setChats(chats: MutableList<Pair<String, JobData>>) {
        allJobs = chats.sortedByDescending { it.second.lastUpdate }.toMutableList()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filterBy(jobs: MutableList<Pair<String, JobData>>, status: JobStatus): Int {
        allJobs = jobs.filter { it.second.jobStatus == status }.toMutableList()
        notifyDataSetChanged()
        return allJobs.size
    }

    override fun getItemCount() = allJobs.size

    class ChatListViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val userName = v.findViewById<TextView>(R.id.chatMember)
        private val timeslotTitle = v.findViewById<TextView>(R.id.timeslotTitle)
        private val jobStatus = v.findViewById<TextView>(R.id.jobStatus)
        private val time = v.findViewById<TextView>(R.id.time)
        private val date = v.findViewById<TextView>(R.id.date)
        private val image = v.findViewById<ImageView>(R.id.userImageOnChat)
        private val rootView = v
        fun bind(jobID: String, job: JobData,context: Context) {
            val firebaseUserID = FirebaseAuth.getInstance().uid!!
            val otherUserID = if (job.userProducerID == firebaseUserID) job.userConsumerID
            else job.userProducerID

            FirebaseFirestore.getInstance().collection("users").document(otherUserID).get().addOnSuccessListener { otherUser ->
                    userName.text = otherUser.toUserProfileData().fullName
                    if (otherUser != null) {
                        Firebase.storage.getReferenceFromUrl(String.format(context.getString(R.string.firebaseUserPic,otherUserID))).getBytes(1024 * 1024).addOnSuccessListener { pic ->
                                image.setImageBitmap(BitmapFactory.decodeByteArray(pic, 0, pic.size))
                            }
                    }
                }

            FirebaseFirestore.getInstance().collection("timeslots").document(job.timeslotID).get().addOnSuccessListener {
                    timeslotTitle.text = it.toTimeslotData().title
                }
            FirebaseFirestore.getInstance().collection("jobs").document(jobID).get().addOnSuccessListener {
                val jData = it.toJobData()
                time.text = timeFormatter(jData.lastUpdate)
                date.text = dateFormatter(jData.lastUpdate)
                jobStatus.text = jobStatusFormatter(jData.jobStatus)
            }

            rootView.setOnClickListener {
                rootView.findNavController().navigate(R.id.consuming_to_job, bundleOf(
                        "otherUserName" to userName.text,
                        "jobID" to jobID,
                    ))
            }
        }

        private fun timeFormatter(time: Long): String {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = time
            return SimpleDateFormat("hh:mm a", Locale.ITALIAN).format(calendar.time)
        }
        private fun dateFormatter(time: Long): String {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = time
            return SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN).format(calendar.time)
        }
    }
}
