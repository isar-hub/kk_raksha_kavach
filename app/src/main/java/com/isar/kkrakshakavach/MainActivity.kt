    package com.isar.kkrakshakavach

    import android.os.Bundle
    import android.util.Log
    import androidx.activity.enableEdgeToEdge
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.ViewCompat
    import androidx.core.view.WindowInsetsCompat
    import androidx.navigation.findNavController
    import androidx.navigation.fragment.NavHostFragment
    import androidx.navigation.ui.setupWithNavController
    import com.isar.kkrakshakavach.databinding.ActivityMainBinding

    private lateinit var binding : ActivityMainBinding
    class MainActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setUpToolbar()


            navGrphImpl()
        }
        private fun setUpToolbar() {
            setSupportActionBar(binding.toolbar)

//            supportActionBar?.setDisplayHomeAsUpEnabled(true)


        }

        private fun navGrphImpl() {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            binding.bottomNavigationView.setupWithNavController(navController)
        }
    }
