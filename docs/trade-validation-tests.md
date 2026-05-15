# Trade Validation Test Matrix

This document maps the trading business rules to the unit tests that document them.

## Core Flow

- `buy by amount computes nominals rounded down to lot size`
  - Documents amount mode: amount input is converted to nominals and rounded down to `lotInstrumentSize`.
- `buy by quantity computes trade amount`
  - Documents quantity mode: nominal input is multiplied by trade price.
- `buy limit by amount computes nominals using limit price`
  - Documents that limit orders use `limitPrice` as `tradePrice`.
- `buy limit by quantity computes trade amount using limit price`
  - Documents quantity mode with limit price.

## Limit Price

- `buy limit rejects only prices above ask plus movement`
  - BUY band: `limitPrice <= askPrice * (1 + percentageMovement)`.
- `sell limit rejects only prices below bid minus movement`
  - SELL band: `limitPrice >= bidPrice * (1 - percentageMovement)`.
- `limit price rejects invalid multiple for stock range`
  - Tick size validation for stock prices.
- `limit price accepts valid multiple for letras`
  - Tick size validation for Letras.
- `limit price validates bond multiple by price range`
  - Tick size validation for Bonos/ON.
- `limit price validates cedear and etf multiple by price range`
  - Tick size validation for CEDEARs/ETFs.
- `limit price validates non leader stock multiple by price range`
  - Tick size validation for non-leader stocks.
- `limit price accepts valid high range multiple for non leader stock`
  - Documents high range tick size.

## Nominals

- `quantity mode rejects nominals below minimum`
  - Validates `tradeNominals >= minInstrumentNominals`.
- `quantity mode rejects nominals that are not lot multiple`
  - Validates `tradeNominals % lotInstrumentSize == 0`.
- `quantity mode rejects nominals above market maximum`
  - Validates `maxInstrumentNominals`.
- `sell by quantity rejects nominals over available holdings`
  - SELL holdings validation: `tradeNominals <= holdingQuantity`.

## Sell By Amount

- `sell market by amount validates max sellable amount using bid price`
  - MARKET max sellable: `holdingQuantity * bidPrice`.
- `sell limit by amount rejects input above sellable amount using limit price`
  - LIMIT max sellable: `holdingQuantity * limitPrice`.
- `sell limit by amount accepts exact sellable amount using limit price`
  - Boundary case for exact max sellable.

## Min And Max Total

- `buy derives minimum amount from instrument minimum and lot size`
  - Amount mode below `roundUp(minInstrumentNominals, lotInstrumentSize) * tradePrice`.
- `buy ars amount below channel minimum rejects with amount minimum`
  - BUY + ARS + Amount must be at least `$100`.
- `buy ars quantity below channel minimum rejects with total minimum`
  - BUY + ARS + Quantity must produce a total of at least `$100`.
- `quantity mode below monetary minimum is not a separate rule`
  - Quantity mode is validated by nominal minimum and lot multiple.
- `buy rejects total above maximum amount`
  - Total amount above the selected account balance.
- `buy quantity limit rejects total above maximum amount`
  - Quantity + limit above the selected account balance.
- `buy quantity limit accepts exact maximum amount`
  - Boundary case for exact selected account balance.
- `buy amount rejects total one cent above maximum amount without fee`
  - Input validation uses `totalTradeAmount = tradeAmount`; fee is validated in the confirmation step.

## Balances And Fees

- `buy ars rejects insufficient ars balance by trade amount`
  - BUY ARS balance validation.
- `buy usd validates usd amount balance`
  - BUY USD uses the selected USD balance for trade amount.

## Account Balance Selection

- `21 market today uses market now balance`
  - Uses `balanceMarketNow`.
- `22 market 24 hours uses market 24 balance`
  - Uses `balanceMarket24`.
- `23 limit today uses limit now balance`
  - Uses `balanceLimitNow`.
- `24 limit 24 hours uses limit 24 balance`
  - Uses `balanceLimit24`.

## UI State Guards

- `security defaults allow missing server values but ui state blocks missing market rules`
  - Missing server values do not pollute validator errors; `uiState.canContinue` blocks the action.
- `settlement term is stored in view model state`
  - Settlement term is part of order state, not local UI state.
