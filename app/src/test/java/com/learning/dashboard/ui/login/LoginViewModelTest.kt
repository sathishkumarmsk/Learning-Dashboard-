package com.learning.dashboard.ui.login

import com.learning.dashboard.data.remote.AuthApi
import com.learning.dashboard.data.remote.AuthException
import com.learning.dashboard.data.repository.AuthRepository
import com.learning.dashboard.data.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockAuthApi: FakeAuthApi
    private lateinit var fakeSessionStore: FakeSessionStore
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockAuthApi = FakeAuthApi()
        fakeSessionStore = FakeSessionStore()
        repository = AuthRepository(mockAuthApi, fakeSessionStore)
        viewModel = LoginViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_validationFails_whenEmailIsBlank() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password123")
        viewModel.login()

        val state = viewModel.uiState.value
        assertEquals("Email is required", state.emailError)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun login_validationFails_whenEmailIsInvalid() = runTest {
        viewModel.onEmailChange("invalid-email")
        viewModel.onPasswordChange("password123")
        viewModel.login()

        val state = viewModel.uiState.value
        assertEquals("Enter a valid email", state.emailError)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun login_validationFails_whenPasswordIsTooShort() = runTest {
        viewModel.onEmailChange("student@learn.com")
        viewModel.onPasswordChange("123")
        viewModel.login()

        val state = viewModel.uiState.value
        assertEquals("Password must be at least 6 characters", state.passwordError)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun login_succeeds_withValidCredentials() = runTest {
        viewModel.onEmailChange("student@learn.com")
        viewModel.onPasswordChange("password123")
        viewModel.login()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
        assertTrue(state.isLoggedIn)
        assertEquals("token_123", fakeSessionStore.token)
    }

    @Test
    fun login_fails_whenCredentialsAreIncorrect() = runTest {
        mockAuthApi.shouldFail = true
        viewModel.onEmailChange("student@learn.com")
        viewModel.onPasswordChange("wrongpassword")
        viewModel.login()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertNotNull(state.errorMessage)
        assertEquals("Invalid email or password", state.errorMessage)
    }

    private class FakeAuthApi : AuthApi {
        var shouldFail: Boolean = false
        override suspend fun login(email: String, password: String): String {
            if (shouldFail) {
                throw AuthException("Invalid email or password")
            }
            return "token_123"
        }
    }

    private class FakeSessionStore : SessionStore {
        override var token: String? = null
        override val isLoggedIn: Boolean get() = !token.isNullOrBlank()
        override fun clear() {
            token = null
        }
    }
}
