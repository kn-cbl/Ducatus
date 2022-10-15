package com.ducatus

class AppResources {
    private val categories = mapOf(
        "0" to Category(
            0, "Electronics", 1,
            "material_dark_blue_700", "ic_baseline_devices_24"),

        "1" to Category(
            1, "Emergency Funds", 0,
            "material_bright_red_700", "ic_local_first_aid_24"),

        "2" to Category(
            2, "Financial Expenses", 0,
            "material_dark_yellow_a400", "ic_baseline_wallet_24"),

        "3" to Category(
            3, "Food and Groceries", 0,
            "material_green_500", "ic_baseline_fastfood_24"),

        "4" to Category(
            4, "Housing", 0,
            "material_orange_a400", "ic_baseline_home_24"),

        "5" to Category(
            5, "Investments", 0,
            "dark_green", "ic_local_investment_24"),

        "6" to Category(
            6, "Healthcare", 0,
            "darker_pink", "ic_baseline_health_and_safety_24"),

        "7" to Category(
            7, "Savings", 1,
            "material_green_700", "ic_baseline_savings_24"),

        "7" to Category(
            7, "Shopping and Entertainment", 1,
            "material_dark_purple_700", "ic_outline_shopping_bag_24"),

        "8" to Category(
            8, "Transportation", 0,
            "material_teal_700", "ic_baseline_directions_bus_24"),

        "9" to Category(
            9, "Utilities", 0,
            "material_orange_700", "ic_baseline_electric_bolt_24"),
    )

    //                    val categories = mapOf(
//                        "0" to Category(
//                            0, "Electronics", 1,
//                            "blue", "ic_baseline_devices_24"),
//
//                        "1" to Category(
//                            1, "Financial Expenses", 0,
//                            "material_dark_yellow_a400", "ic_baseline_wallet_24"),
//
//                        "2" to Category(
//                            2, "Food and Drinks", 0,
//                            "material_bright_red_a400", "ic_baseline_fastfood_24"),
//
//                        "3" to Category(
//                            3, "Housing", 0,
//                            "material_orange_a400", "ic_baseline_home_24"),
//
//                        "4" to Category(
//                            4, "Investments", 0,
//                            "dark_green", "ic_local_investment_24"),
//
//                        "5" to Category(
//                            5, "Life and Entertainment", 1,
//                            "material_cyan_a400", "ic_baseline_videogame_asset_24"),
//
//                        "6" to Category(
//                            6, "Shopping", 1,
//                            "dark_pink", "ic_outline_shopping_bag_24"),
//
//                        "7" to Category(
//                            7, "Transportation", 0,
//                            "dark_brown", "ic_baseline_directions_bus_24"),
//
//                        "8" to Category(
//                            8, "Vehicle", 1,
//                            "material_dark_purple_a400", "ic_baseline_directions_car_24"),
//
//                        "9" to Category(
//                            9, "Others", 1,
//                            "light_gray", "ic_baseline_more_horiz_24"),
//                    )

    private val colors = listOf(
        "lighter_red", "bright_pink", "pink", "dark_pink", "darker_pink", "rose_red",
        "material_bright_red_a400", "material_bright_red_a700", "material_bright_red_500", "material_bright_red_700",
        "material_red_a400", "material_red_a700", "material_red_500", "material_red_700", "darker_red",
        "material_purple_a400", "material_purple_a700", "material_purple_500", "material_purple_700",
        "material_dark_purple_a400", "material_dark_purple_a700", "material_dark_purple_500", "material_dark_purple_700",
        "material_dark_blue_a400", "material_dark_blue_a700", "material_dark_blue_500", "material_dark_blue_700",
        "blue", "dark_blue", "light_blue", "material_blue_a400", "material_blue_a700", "material_blue_500", "material_blue_700",
        "material_light_blue_a400", "material_light_blue_a700", "material_light_blue_500", "material_light_blue_700",
        "very_dark_blue", "material_cyan_a400", "material_cyan_a700", "material_cyan_500", "material_cyan_700",
        "material_teal_a400", "material_teal_a700", "material_teal_500", "material_teal_700",
        "dark_green", "material_green_a400", "material_green_a700", "material_green_500", "material_green_700",
        "material_green_yellow_a400", "material_green_yellow_a700", "material_green_yellow_500", "material_green_yellow_700",
        "material_yellow_green_a400", "material_yellow_green_a700", "material_yellow_green_500", "material_yellow_green_700",
        "material_yellow_a400", "material_yellow_a700", "material_yellow_500", "material_yellow_700",
        "material_dark_yellow_a400", "material_dark_yellow_a700", "material_dark_yellow_500", "material_dark_yellow_700",
        "material_orange_a400", "material_orange_a700", "material_orange_500", "material_orange_700",
        "material_dark_orange_a400", "material_dark_orange_a700", "material_dark_orange_500", "material_dark_orange_700",
        "dark_brown", "light_gray", "dark_gray", "black"
    )

    private val icons = listOf(
        "ic_baseline_devices_24", "ic_baseline_directions_bus_24", "ic_baseline_directions_car_24",
        "ic_baseline_electric_bolt_24", "ic_baseline_fastfood_24", "ic_baseline_health_and_safety_24",
        "ic_baseline_home_24", "ic_baseline_medication_24", "ic_baseline_more_horiz_24",
        "ic_baseline_savings_24", "ic_outline_shopping_bag_24", "ic_baseline_videogame_asset_24",
        "ic_baseline_wallet_24", "ic_local_first_aid_24", "ic_local_investment_24",
    )

    fun getDefaultCategories(): Map<String, Category> {
        return categories
    }

    fun getColors(): List<String> {
        return colors
    }

    fun getIcons(): List<String> {
        return icons
    }
}