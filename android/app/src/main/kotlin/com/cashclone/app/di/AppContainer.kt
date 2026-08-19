package com.cashclone.app.di

import android.content.Context
import com.cashclone.app.data.remote.RetrofitClient
import com.cashclone.app.data.remote.TokenStore
import com.cashclone.app.data.repository.CashCloneRepository

class AppContainer(context: Context) {
    private val tokenStore = TokenStore(context.applicationContext)
    private val api = RetrofitClient.create(tokenStore)
    val repository = CashCloneRepository(api, tokenStore)
}
