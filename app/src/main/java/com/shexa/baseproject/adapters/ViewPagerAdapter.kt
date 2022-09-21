package com.shexa.baseproject.adapters

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.shexa.baseproject.fragments.ReadDataHistoryFragment
import com.shexa.baseproject.fragments.WriteDataHistoryFragment

class ViewPagerAdapter(var context:Context,var tabCount:Int,var fm:FragmentManager) : FragmentStatePagerAdapter(fm)
{
    override fun getCount(): Int
    {
        return tabCount;
    }

    override fun getItem(position: Int): Fragment
    {
        when(position)
        {
            0->{
                return WriteDataHistoryFragment()
            }
            1->{
                return ReadDataHistoryFragment()
            }
            2-> {
                return WriteDataHistoryFragment()
            }
            else->
            {
                return WriteDataHistoryFragment()
            }
        }
    }

}