package com.example.fibo.di

import android.content.Context
import com.example.fibo.datastore.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.apollographql.apollo3.ApolloClient
import com.example.fibo.apollo.ApolloClient as AppApolloClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApolloClient(@ApplicationContext context: Context): ApolloClient {
        return AppApolloClient.getApolloClient(context)
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
}