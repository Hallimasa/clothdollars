# Create × CobbleDollars

Install `clothdollars-0.1.1.jar` in the client and server mods folders alongside Minecraft 1.21.1, NeoForge 21.1.265, Create 6.0.10, and CobbleDollars 2.0.0+Beta-6.1+1.21.1 with their required dependencies.

As the seller, right-click your configured Table Cloth's price tag with an empty main hand. Choose **Use CobbleDollars**, enter a whole-number amount or adjust it with the buttons, and confirm. Clicking with a currency item selects Create's normal item payment.

As the buyer, click the table with an empty hand, then add purchases to the same list. The quoted total updates on each click. Hand the list to the seated shopkeeper. Create's packager fulfills the order. As the seller, right-click the Stock Ticker empty-handed to collect.

Earnings belong to the seller UUID on the Create logistics network. A replacement ticker linked to that network can collect the same balance. Collect earnings before retiring a network. Item and CobbleDollars purchases use separate lists.

## Development

Use a Java 21 JDK and run `./gradlew build` (Windows: `gradlew.bat build`). The release is in `build/libs/`. Gradle resolves the pinned publisher files. To use matching local files, pass `-PmodDir="/path/to/mods"`. The development runs also use Cobblemon 1.8.4 and Kotlin for Forge 5.12.0. Use `runClient` or `runServer` for the development game. The optional potato-sale fixture uses `./gradlew -PshopTests runGameTestServer`.

On a dependency update, review Create's Table Cloth filtering and slot transform, Shopping List component, packager request allocation and Stock Ticker checkout. The bridge's shop logic is in Shop, its persisted network balances in Earnings, and its CobbleDollars balance access in Prices. Validate the complete sale and collection with separate players after updating either mod.
