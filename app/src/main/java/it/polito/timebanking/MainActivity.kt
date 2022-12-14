package it.polito.timebanking

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import it.polito.timebanking.databinding.ActivityMainBinding
import it.polito.timebanking.model.profile.ProfileData
import it.polito.timebanking.model.profile.ProfileViewModel
import it.polito.timebanking.model.profile.fullNameFormatter

class MainActivity : AppCompatActivity(), NavBarUpdater {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val defaultAge = 0L
    private val startingTime = 60L
    private val profileVM by viewModels<ProfileViewModel>()
    private val firebaseUserID = FirebaseAuth.getInstance().uid!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.personalTimeslotListFragment, R.id.allSkillFragment, R.id.consumingJobsFragment, R.id.producingJobsFragment, R.id.couponFragment, R.id.transactionsListFragment
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        binding.navView.getHeaderView(0).findViewById<Button>(R.id.buttonLogout).setOnClickListener {
            findViewById<DrawerLayout>(R.id.drawer_layout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Are you sure you want to log out?")
            dialog.setView(layoutInflater.inflate(R.layout.dialog_generic, findViewById(android.R.id.content), false))
            dialog.setNegativeButton("No") { _, _ ->
                findViewById<DrawerLayout>(R.id.drawer_layout).setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
            dialog.setPositiveButton("Yes") { _, _ ->
                Firebase.auth.signOut()
                GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()).signOut()
                Toast.makeText(this, "See you soon!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, EntryPointActivity::class.java))
                finish()
            }
            dialog.create().show()
        }

        FirebaseFirestore.getInstance().collection("users").document(firebaseUserID).get().addOnSuccessListener { res ->
            if (!res.exists()) {
                Firebase.storage.getReferenceFromUrl(String.format(resources.getString(R.string.firebaseDefaultPic))).getBytes(1024 * 1024).addOnSuccessListener {
                    Firebase.storage.getReferenceFromUrl(String.format(resources.getString(R.string.firebaseUserPic, firebaseUserID))).putBytes(it)
                }.addOnFailureListener { e -> Log.w("warn", "Error with login $e") }
                FirebaseFirestore.getInstance().collection("users").document(firebaseUserID)
                    .set(ProfileData("", "", getSharedPreferences("group21.lab5.PREFERENCES", MODE_PRIVATE).getString("email", "unknown email")!!, defaultAge, "", listOf<String>(), listOf<String>(), "", startingTime,0,0, .0, 0, .0, 0, listOf<String>()))
            } else {
                updateIMG(String.format(resources.getString(R.string.firebaseUserPic, firebaseUserID)))
            }
        }.addOnFailureListener { e -> Log.w("warn", "Error with login $e") }
        profileVM.get(firebaseUserID).observe(this) {
            updateTime(it.time)
            updateFName(fullNameFormatter(it.fullName, false))
            updateActiveJobs(it.activeConsumingJobs,it.activeProducingJobs)
        }

        /*
          when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
              Configuration.UI_MODE_NIGHT_YES -> {
                  window.statusBarColor = ContextCompat.getColor(this, R.color.myGreenDark)
              }
              Configuration.UI_MODE_NIGHT_NO -> {
                  window.statusBarColor = ContextCompat.getColor(this, R.color.myGreenLight)
              }
          }
         */
        window.statusBarColor = ContextCompat.getColor(this, R.color.MenuColor)

        binding.navView.getHeaderView(0).findViewById<ShapeableImageView>(R.id.userImageOnDrawer).setOnClickListener {
            navHostFragment.findNavController().navigate(R.id.showProfileFragment)
        }
    }

    override fun setNavBarTitle(title: String?) {
        supportActionBar!!.title = title
    }

    override fun updateActiveJobs(consuming: Long, producing: Long) {
        binding.navView.menu.findItem(R.id.producingJobsFragment).title = String.format(resources.getString(R.string.jobRequests,producing))
        binding.navView.menu.findItem(R.id.consumingJobsFragment).title = String.format(resources.getString(R.string.requestedJobs,consuming))
    }

    override fun updateTime(time: Long) {
        if (time == 0L) {
            binding.navView.getHeaderView(0).findViewById<TextView>(R.id.userTimeOnDrawer).setTextColor(ContextCompat.getColor(this, R.color.Beer))
            binding.navView.getHeaderView(0).findViewById<TextView>(R.id.userTimeOnDrawer).text = String.format(resources.getString(R.string.no_time))
        } else {
            if (time < 60)
                binding.navView.getHeaderView(0).findViewById<TextView>(R.id.userTimeOnDrawer).setTextColor(ContextCompat.getColor(this, R.color.Yellow))
            else
                binding.navView.getHeaderView(0).findViewById<TextView>(R.id.userTimeOnDrawer).setTextColor(ContextCompat.getColor(this, R.color.Azure))
            binding.navView.getHeaderView(0).findViewById<TextView>(R.id.userTimeOnDrawer).text = timeFormatter(time)
        }
    }

    private fun timeFormatter(time: Long): String {
        val h = if (time / 60L == 1L) "1 hour"
        else "${time / 60L} hours"
        val m = if (time % 60L == 1L) "1 minute"
        else "${time % 60L} minutes"
        return if (h == "0 hours") m
        else "$h, $m"
    }

    override fun updateFName(name: String) {
        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.userNameOnDrawer).text = name
    }

    override fun updateIMG(url: String) {
        Firebase.storage.getReferenceFromUrl(url).getBytes(1024 * 1024).addOnSuccessListener {
            binding.navView.getHeaderView(0).findViewById<ShapeableImageView>(R.id.userImageOnDrawer).setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size))
        }.addOnFailureListener { e -> Log.w("warn", "Error with login $e") }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)/*if (this.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            val item = menu.getItem(0)
            val s = SpannableString("Edit")
            s.setSpan(ForegroundColorSpan(Color.WHITE), 0, s.length, 0)
            item.title = s
        }*/
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.fragment_content_main).navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}