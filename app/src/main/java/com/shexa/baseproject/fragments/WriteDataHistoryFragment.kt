package com.shexa.baseproject.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shexa.baseproject.R
import com.shexa.baseproject.adapters.RvWriteDataHistoryAdapter
import com.shexa.baseproject.entities.WriteHistoryModel
import com.shexa.baseproject.helpers.AppDataBase
import java.lang.Exception
import kotlin.concurrent.thread


class WriteDataHistoryFragment : Fragment()
{
    private var tvWrite : AppCompatTextView?=null
    private var rvWriteDataHistory : RecyclerView?=null
    private  var lstWriteData: List<WriteHistoryModel> = mutableListOf()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_write_data_history, container, false)

        tvWrite = view.findViewById(R.id.tv_write_history)
        rvWriteDataHistory = view.findViewById(R.id.rv_write_history_Data)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpWriteHistoryDataToRV()

    }

    private fun setUpWriteHistoryDataToRV()
    {
//        val lstData : List<WriteHistoryModel> = getAllWriteHistoryData()
        getAllWriteHistoryData()
    }

    private fun getAllWriteHistoryData() //: List<WriteHistoryModel>
    {
        val dbInstance = AppDataBase.getInstance(view?.context!!)
        try
        {
            val t = Thread{
                lstWriteData = dbInstance.WriteHistoryDao().getAllWriteRecord()
                Log.e("LST_HISTORY", "getAllWriteHistoryData: ${lstWriteData}" )
                activity?.runOnUiThread {
                    if(lstWriteData.isNotEmpty())
                    {
                        tvWrite?.text = "Write History";
                        rvWriteDataHistory?.visibility = View.VISIBLE
                        rvWriteDataHistory?.layoutManager = LinearLayoutManager(context,LinearLayoutManager.VERTICAL,true)
                        rvWriteDataHistory?.adapter = RvWriteDataHistoryAdapter(requireContext(),lstWriteData)
                    }
                    else
                    {
                        tvWrite?.text = "nothing to show here";
                        rvWriteDataHistory?.visibility = View.GONE
                    }
                }
            }
            t.start()

//            thread(start = true)
//            {
//
//            }
        }catch(e:Exception)
        {
            e.printStackTrace()
        }

//        return  lstWriteData
    }

}