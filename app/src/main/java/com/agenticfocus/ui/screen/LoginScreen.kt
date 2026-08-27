package com.agenticfocus.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.viewmodel.AuthState
import com.agenticfocus.viewmodel.AuthViewModel

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val isLoading = authState is AuthState.Loading
    val focusManager = LocalFocusManager.current

    // Clear inline validation errors when user types
    LaunchedEffect(email) { emailError = null }
    LaunchedEffect(password) { passwordError = null }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AgenticFocus",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isRegisterMode) "Créez votre compte AgenticFocus"
                           else "Connectez-vous pour synchroniser vos données",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Adresse email") },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSubmit(email, password,
                                isRegister = isRegisterMode,
                                setEmailError = { emailError = it },
                                setPasswordError = { passwordError = it },
                                authViewModel = authViewModel
                            )
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                              else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Masquer" else "Afficher"
                            )
                        }
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Auth error from ViewModel
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Submit button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSubmit(email, password,
                            isRegister = isRegisterMode,
                            setEmailError = { emailError = it },
                            setPasswordError = { passwordError = it },
                            authViewModel = authViewModel
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isRegisterMode) "Créer mon compte" else "Se connecter")
                    }
                }

                // Toggle login / register
                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        emailError = null
                        passwordError = null
                    },
                    enabled = !isLoading
                ) {
                    Text(
                        text = if (isRegisterMode) "Déjà un compte ? Se connecter"
                               else "Pas encore de compte ? Créer un compte",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Échappatoire hors ligne — indispensable quand Supabase est injoignable
                // (quota, panne, avion). Les données sont en Room, seule l'auth exige le
                // réseau : sans ce bouton l'utilisateur est enfermé dehors de ses données.
                if (!isRegisterMode) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            authViewModel.enterStandalone()
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Travailler en local (hors ligne)")
                    }
                    Text(
                        text = "Ouvre vos données déjà présentes sur cet appareil. Vos " +
                               "modifications sont conservées et synchronisées à la " +
                               "prochaine connexion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun onSubmit(
    email: String,
    password: String,
    isRegister: Boolean,
    setEmailError: (String?) -> Unit,
    setPasswordError: (String?) -> Unit,
    authViewModel: AuthViewModel
) {
    var hasError = false
    if (email.isBlank()) {
        setEmailError("Email requis")
        hasError = true
    }
    if (password.isBlank()) {
        setPasswordError("Mot de passe requis")
        hasError = true
    } else if (isRegister && password.length < 6) {
        setPasswordError("6 caractères minimum")
        hasError = true
    }
    if (!hasError) {
        if (isRegister) authViewModel.signUp(email.trim(), password)
        else authViewModel.signIn(email.trim(), password)
    }
}
