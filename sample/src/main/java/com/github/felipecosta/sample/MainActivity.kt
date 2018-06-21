package com.github.felipecosta.sample

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.view.*
import com.github.felipehjcosta.slidingtablayout.SlidingTabLayout
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.main_fragment.view.*

class MainActivity : AppCompatActivity() {

    private var sectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val colors = intArrayOf(
                Color.parseColor("#7ED321"),
                Color.parseColor("#E5615C"),
                Color.parseColor("#40AAB9")
        )
        sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        container.adapter = sectionsPagerAdapter
        slidingTabLayout.setDistributeEvenly(false)
        slidingTabLayout.setCustomTabView(R.layout.indicator_view, R.id.indicator_title)
        slidingTabLayout.setTabColorizer(SlidingTabLayout.SimpleTabColorizer(colors))
        slidingTabLayout.setViewPager(container)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when {
            item.itemId == R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return PlaceholderFragment.newInstance(position + 1)
        }

        override fun getCount(): Int = 3

        override fun getPageTitle(position: Int): CharSequence? {
            val pageTitle = "$position"
            val spannableStringBuilder = SpannableStringBuilder()
            spannableStringBuilder.append(pageTitle)
            return spannableStringBuilder
        }
    }

    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.main_fragment, container, false)
            rootView.label.text = getString(R.string.section_format, arguments?.getInt(ARG_SECTION_NUMBER))
            return rootView
        }

        companion object {
            private const val ARG_SECTION_NUMBER = "section_number"

            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                return PlaceholderFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_SECTION_NUMBER, sectionNumber)
                    }
                }
            }
        }
    }
}
