package com.example.lab2

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lab2.ui.components.ContactRow
import com.example.lab2.ui.components.ProfileCard
import com.example.lab2.ui.theme.Lab2Theme

data class Contact(val name: String, val phone: String)

private val sampleContacts = listOf(
    Contact("Ana Popescu", "+40 721 000 001"),
    Contact("Bogdan Ionescu", "+40 721 000 002"),
    Contact("Carmen Dumitru", "+40 721 000 003"),
    Contact("Dan Marinescu", "+40 721 000 004"),
    Contact("Elena Stoica", "+40 721 000 005"),
    Contact("Florin Georgescu", "+40 721 000 006"),
    Contact("Georgiana Radu", "+40 721 000 007"),
    Contact("Horia Constantin", "+40 721 000 008"),
    Contact("Irina Vasile", "+40 721 000 009"),
    Contact("Mihai Toma", "+40 721 000 010"),
    Contact("Nicoleta Ene", "+40 721 000 011"),
    Contact("Ovidiu Pavel", "+40 721 000 012")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ContactsScreen(
                        contacts = sampleContacts,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Ecran în două secțiuni: header (ProfileCard) + listă (LazyColumn).
 * Primește doar date (state) și nu are efecte secundare: aceleași
 * intrări => aceeași interfață.
 */
@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ProfileCard(
            name = "Nika Tirsina",
            role = "Student Android",
            bio = "Învăț Jetpack Compose.",
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Contacte",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(contacts, key = { it.phone }) { contact ->
                ContactRow(name = contact.name, phone = contact.phone)
            }
        }
    }
}

// ---------- Previews ----------

@Preview(name = "Light", showBackground = true)
@Composable
fun ContactsLightPreview() {
    Lab2Theme(darkTheme = false, dynamicColor = false) {
        Surface { ContactsScreen(sampleContacts) }
    }
}

@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun ContactsDarkPreview() {
    Lab2Theme(darkTheme = true, dynamicColor = false) {
        Surface { ContactsScreen(sampleContacts) }
    }
}

@Preview(name = "Font 2x", showBackground = true, fontScale = 2f)
@Composable
fun ContactsLargeFontPreview() {
    Lab2Theme(darkTheme = false, dynamicColor = false) {
        Surface { ContactsScreen(sampleContacts) }
    }
}

@Preview(
    name = "Dark + Font 2x",
    showBackground = true,
    fontScale = 2f,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun ContactsDarkLargeFontPreview() {
    Lab2Theme(darkTheme = true, dynamicColor = false) {
        Surface { ContactsScreen(sampleContacts) }
    }
}