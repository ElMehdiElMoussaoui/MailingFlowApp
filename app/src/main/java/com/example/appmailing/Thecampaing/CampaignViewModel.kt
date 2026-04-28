package com.example.appmailing.Thecampaing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appmailing.statistique.AppDatabase
import kotlinx.coroutines.launch

class CampaignViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).campaignDao()

    // Basic campaigns list
    val allCampaigns: LiveData<List<Campaign>> = dao.getAllCampaigns()

    // Campaigns with their associated Product data
    val allCampaignsWithProduct: LiveData<List<CampaignWithProduct>> = dao.getAllCampaignsWithProduct()

    // Campaigns with both Template and Product data
    val allCampaignsFullDetails: LiveData<List<CampaignFullDetails>> = dao.getAllCampaignsFullDetails()

    fun addCampaign(campaign: Campaign) = viewModelScope.launch {
        dao.insertCampaign(campaign)
    }

    fun updateCampaign(campaign: Campaign) = viewModelScope.launch {
        dao.insertCampaign(campaign)
    }

    fun updateCampaignStatus(id: Int, newStatus: String) = viewModelScope.launch {
        dao.updateStatus(id, newStatus)
    }

    fun getCampaignById(id: Int): LiveData<Campaign> = dao.getCampaignById(id)

    fun deleteCampaign(id: Int) = viewModelScope.launch {
        dao.deleteById(id)
    }
}