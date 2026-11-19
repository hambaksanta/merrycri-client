// MainActivity.kt
package com.hambaksanta.merrycri

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.hambaksanta.merrycri.databinding.ActivityMainBinding
import com.hambaksanta.merrycri.ui.friends.FriendsFragment
import com.hambaksanta.merrycri.ui.tree.TreeFragment

/** v2의 단일 Activity. 하단 탭에 맞춰 Fragment만 교체한다. 담당: B */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val app get() = application as MerryCriApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_tree -> showFragment(TreeFragment(), TAG_TREE)
                R.id.navigation_friends -> showFragment(FriendsFragment(), TAG_FRIENDS)
                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.navigation_tree
        }
    }

    private fun showFragment(fragment: Fragment, tag: String): Boolean {
        if (supportFragmentManager.findFragmentByTag(tag) != null) return true
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment, tag)
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        app.audio.resume()
    }

    override fun onStop() {
        app.audio.pause()
        super.onStop()
    }

    private companion object {
        const val TAG_TREE = "tree"
        const val TAG_FRIENDS = "friends"
    }
}
