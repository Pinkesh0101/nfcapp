package com.shexa.baseproject.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.shexa.baseproject.R
import com.shexa.baseproject.adapters.ViewPagerAdapter

class TagHistoryActivity : AppCompatActivity()
{

    lateinit var tabLayout : TabLayout
    var viewPager : ViewPager?=null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_history)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        tabLayout.addTab(tabLayout.newTab().setText("Write"))
        tabLayout.addTab(tabLayout.newTab().setText("Read"))
        tabLayout.addTab(tabLayout.newTab().setText("QR"))

        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val pagerAdapter = ViewPagerAdapter(this,tabLayout.tabCount,supportFragmentManager);
        viewPager?.adapter = pagerAdapter

        viewPager?.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        tabLayout.addOnTabSelectedListener(object :TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?)
            {
                viewPager?.currentItem = tab?.position!!
            }

            override fun onTabUnselected(tab: TabLayout.Tab?)
            {

            }

            override fun onTabReselected(tab: TabLayout.Tab?)
            {

            }
        })
    }
}