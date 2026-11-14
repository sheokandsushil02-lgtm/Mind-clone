package com.example.mad

import android.content.Context
import android.content.Intent

/**
 * MAD khulne ka darwaza — HomeScreen ke button se call hota hai.
 * MadActivity alag Activity hai, original app ke screens/ViewModel ko touch nahi karti.
 */
object MadLauncher {

    fun open(context: Context) {
        val intent = Intent(context, MadActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
