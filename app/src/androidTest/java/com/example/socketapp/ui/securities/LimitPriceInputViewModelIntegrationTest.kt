package com.example.socketapp.ui.securities

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.example.socketapp.Security
import com.example.socketapp.TradeValidator
import com.example.socketapp.TradeViewModel
import com.example.socketapp.data.SecuritiesRepository
import org.junit.Rule
import org.junit.Test

class LimitPriceInputViewModelIntegrationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typingLimitPriceUsesViewModelFormatterAndKeepsCursorAtEnd() {
        composeRule.setContent {
            val viewModel = remember {
                TradeViewModel(
                    repository = EmptySecuritiesRepository(),
                    validator = TradeValidator(),
                )
            }

            BasicTextField(
                value = viewModel.uiState.limitPriceInput,
                onValueChange = viewModel::onLimitPriceChange,
                modifier = Modifier.testTag(LimitPriceInputTag),
            )
        }

        composeRule.onNodeWithTag(LimitPriceInputTag).performClick()

        composeRule.onNodeWithTag(LimitPriceInputTag).performTextInput("1")
        composeRule.onNodeWithTag(LimitPriceInputTag)
            .assertEditableText("1")
            .assertSelection(TextRange(1))

        composeRule.onNodeWithTag(LimitPriceInputTag).performTextInput("2")
        composeRule.onNodeWithTag(LimitPriceInputTag)
            .assertEditableText("12")
            .assertSelection(TextRange(2))

        composeRule.onNodeWithTag(LimitPriceInputTag).performTextInput("3")
        composeRule.onNodeWithTag(LimitPriceInputTag)
            .assertEditableText("123")
            .assertSelection(TextRange(3))

        composeRule.onNodeWithTag(LimitPriceInputTag).performTextInput("4")
        composeRule.onNodeWithTag(LimitPriceInputTag)
            .assertEditableText("1.234")
            .assertSelection(TextRange(5))
    }

    @Test
    fun typingLimitPriceKeepsOnlyThreeDecimalDigitsThroughViewModelFormatter() {
        composeRule.setContent {
            val viewModel = remember {
                TradeViewModel(
                    repository = EmptySecuritiesRepository(),
                    validator = TradeValidator(),
                )
            }

            BasicTextField(
                value = viewModel.uiState.limitPriceInput,
                onValueChange = viewModel::onLimitPriceChange,
                modifier = Modifier.testTag(LimitPriceInputTag),
            )
        }

        composeRule.onNodeWithTag(LimitPriceInputTag).performClick()
        composeRule.onNodeWithTag(LimitPriceInputTag).performTextInput("1234,5678")

        composeRule.onNodeWithTag(LimitPriceInputTag)
            .assertEditableText("1.234,567")
            .assertSelection(TextRange(9))
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertEditableText(
        text: String,
    ): androidx.compose.ui.test.SemanticsNodeInteraction =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text)))

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertSelection(
        selection: TextRange,
    ): androidx.compose.ui.test.SemanticsNodeInteraction =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, selection))

    private class EmptySecuritiesRepository : SecuritiesRepository {
        override fun getCachedSecurities(): List<Security>? = null

        override suspend fun refreshSecurities(): List<Security> = emptyList()

        override suspend fun getBuyableInstruments(): List<Security> = emptyList()

        override suspend fun getBuyableInstrument(id: String): Security? = null
    }

    private companion object {
        const val LimitPriceInputTag = "limit-price-input"
    }
}
