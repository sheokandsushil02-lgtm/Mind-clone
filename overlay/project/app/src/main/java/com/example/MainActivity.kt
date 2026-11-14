package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.RemsFloatingNavigation
import com.example.ui.screens.BillGenerateScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ItemListScreen
import com.example.ui.screens.KhataBookScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RemsGold
import com.example.ui.theme.RemsNavyCard
import com.example.ui.theme.RemsNavyDark
import com.example.viewmodel.AppHub
import com.example.viewmodel.RemsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: RemsViewModel = viewModel()
                val currentHub by viewModel.currentHub.collectAsState()
                val selectedCustomerId by viewModel.selectedCustomerId.collectAsState()

                // Handle Back press
                BackHandler(enabled = currentHub != AppHub.HOME || selectedCustomerId != null) {
                    if (selectedCustomerId != null) {
                        viewModel.selectCustomer(null)
                    } else if (currentHub != AppHub.HOME) {
                        viewModel.navigateToHub(AppHub.HOME)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = RemsNavyDark,
                    topBar = {
                        if (currentHub != AppHub.HOME && selectedCustomerId == null) {
                            RemsTopBar(
                                hub = currentHub,
                                onBack = { viewModel.navigateToHub(AppHub.HOME) }
                            )
                        }
                    },
                    bottomBar = {
                        RemsFloatingNavigation(
                            currentHub = currentHub,
                            onHubSelected = { hub ->
                                if (selectedCustomerId != null) {
                                    viewModel.selectCustomer(null)
                                }
                                viewModel.navigateToHub(hub)
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentHub) {
                            AppHub.HOME -> HomeScreen(viewModel = viewModel)
                            AppHub.ITEM_LIST -> ItemListScreen(viewModel = viewModel)
                            AppHub.KHATA_BOOK -> KhataBookScreen(viewModel = viewModel)
                            AppHub.BILL_GENERATE -> BillGenerateScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemsTopBar(
    hub: AppHub,
    onBack: () -> Unit
) {
    val title = when (hub) {
        AppHub.HOME -> "REMS Business"
        AppHub.ITEM_LIST -> "📋 Item List"
        AppHub.KHATA_BOOK -> "📒 Khata Book Pro"
        AppHub.BILL_GENERATE -> "🧾 Bill & Warranty"
    }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 17.sp
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = RemsGold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = RemsNavyCard)
    )
}
