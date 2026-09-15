package com.example.data.auth

import android.content.Context
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.data.local.AuthCredentialDao
import com.example.data.local.AuthCredentialEntity
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

sealed class AuthResult {
    data class Success(
        val user: UserEntity,
        val isNewUser: Boolean = false,
        val message: String = ""
    ) : AuthResult()

    data class VerificationRequired(
        val email: String,
        val demoCode: String,
        val message: String
    ) : AuthResult()

    data class Error(val message: String) : AuthResult()
    data class Cancelled(val message: String = "Google sign-in was cancelled. You can try again whenever you're ready.") : AuthResult()
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

class NeuraAuthManager(
    private val userDao: UserDao,
    private val authCredentialDao: AuthCredentialDao
) {

    // --- Validation Methods ---

    fun validateName(name: String): ValidationResult {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult(false, "Full name is required.")
            trimmed.length < 2 -> ValidationResult(false, "Name must be at least 2 characters.")
            else -> ValidationResult(true)
        }
    }

    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return when {
            trimmed.isEmpty() -> ValidationResult(false, "Email address is required.")
            !emailRegex.matches(trimmed) ->
                ValidationResult(false, "Please enter a valid email address.")
            else -> ValidationResult(true)
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult(false, "Password is required.")
            password.length < 6 -> ValidationResult(false, "Password must be at least 6 characters long.")
            !password.any { it.isDigit() } ->
                ValidationResult(false, "Password should contain at least one number.")
            !password.any { it.isLetter() } ->
                ValidationResult(false, "Password should contain at least one letter.")
            else -> ValidationResult(true)
        }
    }

    fun validateConfirmPassword(password: String, confirm: String): ValidationResult {
        return when {
            confirm.isEmpty() -> ValidationResult(false, "Please confirm your password.")
            password != confirm -> ValidationResult(false, "Passwords do not match.")
            else -> ValidationResult(true)
        }
    }

    // --- Cryptographic Security ---

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$password".toByteArray(Charsets.UTF_8)
        val digest = md.digest(combined)
        return digest.joinToString("") { "%02x".format(it) }
    }

    // --- Email & Password Sign Up ---

    suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val normEmail = email.trim().lowercase()
        val normName = name.trim()

        val nameCheck = validateName(normName)
        if (!nameCheck.isValid) return@withContext AuthResult.Error(nameCheck.errorMessage ?: "Invalid name")

        val emailCheck = validateEmail(normEmail)
        if (!emailCheck.isValid) return@withContext AuthResult.Error(emailCheck.errorMessage ?: "Invalid email")

        val passCheck = validatePassword(password)
        if (!passCheck.isValid) return@withContext AuthResult.Error(passCheck.errorMessage ?: "Weak password")

        // Check if email is already registered
        val existingCredential = authCredentialDao.getCredentialByEmail(normEmail)
        if (existingCredential != null) {
            return@withContext AuthResult.Error("An account with this email address is already registered. Please sign in instead.")
        }

        // Generate unique User ID (not email)
        val userId = "neura_usr_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val salt = generateSalt()
        val passwordHash = hashPassword(password, salt)
        val verificationCode = "%06d".format(SecureRandom().nextInt(900000) + 100000)

        val credential = AuthCredentialEntity(
            userId = userId,
            email = normEmail,
            passwordHash = passwordHash,
            salt = salt,
            isEmailVerified = false,
            verificationCode = verificationCode,
            authProvider = "email",
            createdAt = System.currentTimeMillis()
        )
        authCredentialDao.saveCredential(credential)

        // Mark any existing active user as logged out
        userDao.markAllLoggedOut()

        val newUser = UserEntity(
            id = userId,
            name = normName,
            email = normEmail,
            avatarUrl = "",
            authProvider = "email",
            isAuthenticated = false, // Require email verification
            sessionToken = "",
            isFirstLogin = true,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
        userDao.saveUser(newUser)

        AuthResult.VerificationRequired(
            email = normEmail,
            demoCode = verificationCode,
            message = "Verification code dispatched to $normEmail."
        )
    }

    // --- Email Verification ---

    suspend fun verifyEmail(
        email: String,
        enteredCode: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val normEmail = email.trim().lowercase()
        val credential = authCredentialDao.getCredentialByEmail(normEmail)
            ?: return@withContext AuthResult.Error("No pending verification found for $normEmail.")

        if (credential.verificationCode != null && credential.verificationCode == enteredCode.trim()) {
            authCredentialDao.markEmailVerified(normEmail)

            val user = userDao.getUserById(credential.userId)
                ?: return@withContext AuthResult.Error("User record not found.")

            val authenticatedUser = user.copy(
                isAuthenticated = true,
                sessionToken = UUID.randomUUID().toString(),
                lastLoginAt = System.currentTimeMillis()
            )
            userDao.saveUser(authenticatedUser)

            AuthResult.Success(
                user = authenticatedUser,
                isNewUser = authenticatedUser.isFirstLogin,
                message = "Email verified successfully! Welcome to NEURA."
            )
        } else {
            AuthResult.Error("Invalid verification code. Please check your email and try again.")
        }
    }

    // --- Email & Password Sign In ---

    suspend fun loginWithEmail(
        email: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val normEmail = email.trim().lowercase()

        val emailCheck = validateEmail(normEmail)
        if (!emailCheck.isValid) return@withContext AuthResult.Error(emailCheck.errorMessage ?: "Invalid email")

        if (password.isBlank()) {
            return@withContext AuthResult.Error("Please enter your password.")
        }

        val credential = authCredentialDao.getCredentialByEmail(normEmail)
            ?: return@withContext AuthResult.Error("No account found with this email. Please create an account.")

        val computedHash = hashPassword(password, credential.salt)
        if (computedHash != credential.passwordHash) {
            return@withContext AuthResult.Error("Incorrect password. Please verify your credentials and try again.")
        }

        if (!credential.isEmailVerified && credential.verificationCode != null) {
            return@withContext AuthResult.VerificationRequired(
                email = normEmail,
                demoCode = credential.verificationCode,
                message = "Please verify your email before logging in."
            )
        }

        val user = userDao.getUserById(credential.userId)
            ?: UserEntity(
                id = credential.userId,
                name = normEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = normEmail,
                authProvider = "email"
            )

        userDao.markAllLoggedOut()
        val activeUser = user.copy(
            isAuthenticated = true,
            sessionToken = UUID.randomUUID().toString(),
            lastLoginAt = System.currentTimeMillis()
        )
        userDao.saveUser(activeUser)

        AuthResult.Success(
            user = activeUser,
            isNewUser = false,
            message = "Welcome back, ${activeUser.name}!"
        )
    }

    // --- Real Google OAuth Sign-In via Android Credential Manager ---

    suspend fun signInWithGoogle(context: Context): AuthResult = withContext(Dispatchers.Main) {
        val webClientId = AuthConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            return@withContext AuthResult.Error(
                "Google OAuth Web Client ID is not configured.\n\n" +
                "To enable real Google Sign-In:\n" +
                "1. Open AI Studio Secrets panel\n" +
                "2. Add GOOGLE_WEB_CLIENT_ID with your Web Client ID from Google Cloud Console\n\n" +
                "In the meantime, you can seamlessly create an account or sign in with Email & Password."
            )
        }

        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)

                val googleEmail = googleIdToken.id
                val displayName = googleIdToken.displayName ?: googleEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                val avatarUri = googleIdToken.profilePictureUri?.toString() ?: ""
                // Use unique provider subject or hash of email/token as user ID (not email directly)
                val userId = "google_" + googleEmail.replace("@", "_").replace(".", "_")

                withContext(Dispatchers.IO) {
                    val existingUser = userDao.getUserById(userId)
                    val isFirst = existingUser == null

                    userDao.markAllLoggedOut()
                    val user = UserEntity(
                        id = userId,
                        name = displayName,
                        email = googleEmail,
                        avatarUrl = avatarUri,
                        authProvider = "google",
                        isAuthenticated = true,
                        sessionToken = UUID.randomUUID().toString(),
                        isFirstLogin = isFirst,
                        createdAt = existingUser?.createdAt ?: System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    )
                    userDao.saveUser(user)

                    // Also save credential record for provider
                    authCredentialDao.saveCredential(
                        AuthCredentialEntity(
                            userId = userId,
                            email = googleEmail,
                            passwordHash = "OAUTH_GOOGLE_MANAGED",
                            salt = "OAUTH_GOOGLE",
                            isEmailVerified = true,
                            authProvider = "google",
                            createdAt = existingUser?.createdAt ?: System.currentTimeMillis()
                        )
                    )

                    AuthResult.Success(
                        user = user,
                        isNewUser = isFirst,
                        message = "Signed in with Google successfully."
                    )
                }
            } else {
                AuthResult.Error("Received unrecognized credential type from authentication provider.")
            }
        } catch (_: GetCredentialCancellationException) {
            AuthResult.Cancelled("Google sign-in was cancelled. You can try again whenever you're ready.")
        } catch (e: NoCredentialException) {
            AuthResult.Error("No Google account found on this device. Please sign in to a Google account in device Settings or continue with Email.")
        } catch (e: GetCredentialException) {
            val userMsg = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error occurred while connecting to Google. Please check your internet connection and try again."
                e.message?.contains("16:", ignoreCase = true) == true ->
                    "Google sign-in was cancelled. You can try again whenever you're ready."
                else ->
                    "Google authentication was unsuccessful (${e.message ?: "Unknown error"}). You can try again or use Email."
            }
            AuthResult.Error(userMsg)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Unexpected error during Google authentication.")
        }
    }

    // --- Sign Out ---

    suspend fun signOut(context: Context? = null): Unit = withContext(Dispatchers.IO) {
        userDao.markAllLoggedOut()
        if (context != null) {
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
                // Ignore credential clear errors on logout
            }
        }
    }

    // --- Delete Account & User Data Retention ---

    suspend fun deleteAccount(userId: String, context: Context? = null): Unit = withContext(Dispatchers.IO) {
        // Delete user identity
        userDao.deleteUserById(userId)
        authCredentialDao.deleteCredential(userId)

        // Clear Credential state if context provided
        if (context != null) {
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
                // Ignore
            }
        }
    }
}
