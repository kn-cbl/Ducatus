package com.ducatus

import com.ducatus.data.Category

class AppResources {
    private val categorySize = 11

    private val colors = listOf(
        "lighter_red",
        "bright_pink",
        "pink",
        "dark_pink",
        "darker_pink",
        "rose_red",
        "material_bright_red_a400",
        "material_bright_red_a700",
        "material_bright_red_500",
        "material_bright_red_700",
        "material_red_a400",
        "material_red_a700",
        "material_red_500",
        "material_red_700",
        "darker_red",
        "material_purple_a400",
        "material_purple_a700",
        "material_purple_500",
        "material_purple_700",
        "material_dark_purple_a400",
        "material_dark_purple_a700",
        "material_dark_purple_500",
        "material_dark_purple_700",
        "material_dark_blue_a400",
        "material_dark_blue_a700",
        "material_dark_blue_500",
        "material_dark_blue_700",
        "blue",
        "dark_blue",
        "light_blue",
        "material_blue_a400",
        "material_blue_a700",
        "material_blue_500",
        "material_blue_700",
        "material_light_blue_a400",
        "material_light_blue_a700",
        "material_light_blue_500",
        "material_light_blue_700",
        "very_dark_blue",
        "material_cyan_a400",
        "material_cyan_a700",
        "material_cyan_500",
        "material_cyan_700",
        "material_teal_a400",
        "material_teal_a700",
        "material_teal_500",
        "material_teal_700",
        "dark_green",
        "material_green_a400",
        "material_green_a700",
        "material_green_500",
        "material_green_700",
        "material_green_yellow_a400",
        "material_green_yellow_a700",
        "material_green_yellow_500",
        "material_green_yellow_700",
        "material_yellow_green_a400",
        "material_yellow_green_a700",
        "material_yellow_green_500",
        "material_yellow_green_700",
        "material_yellow_a400",
        "material_yellow_a700",
        "material_yellow_500",
        "material_yellow_700",
        "material_dark_yellow_a400",
        "material_dark_yellow_a700",
        "material_dark_yellow_500",
        "material_dark_yellow_700",
        "material_orange_a400",
        "material_orange_a700",
        "material_orange_500",
        "material_orange_700",
        "material_dark_orange_a400",
        "material_dark_orange_a700",
        "material_dark_orange_500",
        "material_dark_orange_700",
        "dark_brown",
        "light_gray",
        "dark_gray",
        "almost_black",
    )

    private val icons = listOf(
        "ic_baseline_account_balance_24",
        "ic_baseline_auto_fix_high_24",
        "ic_baseline_brush_24",
        "ic_baseline_build_24",
        "ic_baseline_business_24",
        "ic_baseline_cable_24",
        "ic_baseline_cake_24",
        "ic_baseline_calendar_month_24",
        "ic_baseline_camera_24",
        "ic_baseline_campaign_24",
        "ic_baseline_carpenter_24",
        "ic_baseline_celebration_24",
        "ic_baseline_child_24",
        "ic_baseline_child_friendly_24",
        "ic_baseline_church_24",
        "ic_baseline_cleaning_24",
        "ic_baseline_cloud_24",
        "ic_baseline_coffee_24",
        "ic_baseline_collections_bookmark_24",
        "ic_baseline_color_lens_24",
        "ic_baseline_computer_24",
        "ic_baseline_connecting_airports_24",
        "ic_baseline_construction_24",
        "ic_baseline_date_24",
        "ic_baseline_design_services_24",
        "ic_baseline_devices_24",
        "ic_baseline_dining_24",
        "ic_baseline_directions_bus_24",
        "ic_baseline_directions_car_24",
        "ic_baseline_download_24",
        "ic_baseline_draw_24",
        "ic_baseline_eco_24",
        "ic_baseline_edit_24",
        "ic_baseline_electric_bolt_24",
        "ic_baseline_electronics_24",
        "ic_baseline_email_24",
        "ic_baseline_event_24",
        "ic_baseline_fastfood_24",
        "ic_local_first_aid_24",
        "ic_baseline_fitness_24",
        "ic_baseline_flag_24",
        "ic_baseline_game_24",
        "ic_baseline_gift_24",
        "ic_baseline_globe_24",
        "ic_baseline_handshake_24",
        "ic_baseline_health_24",
        "ic_baseline_health_and_safety_24",
        "ic_baseline_hike_24",
        "ic_baseline_home_24",
        "ic_baseline_image_24",
        "ic_baseline_info_24",
        "ic_local_investment_24",
        "ic_baseline_jewelry_24",
        "ic_baseline_keyboard_24",
        "ic_baseline_leisure_24",
        "ic_baseline_lock_24",
        "ic_baseline_medication_24",
        "ic_baseline_menu_book_24",
        "ic_baseline_more_horiz_24",
        "ic_baseline_more_vert_24",
        "ic_baseline_motor_24",
        "ic_baseline_music_24",
        "ic_baseline_night_24",
        "ic_baseline_notifications_24",
        "ic_baseline_outdoor_24",
        "ic_baseline_passion_24",
        "ic_baseline_payment_24",
        "ic_baseline_pedal_bike_24",
        "ic_baseline_people_24",
        "ic_baseline_pets_24",
        "ic_baseline_phone_24",
        "ic_baseline_photo_camera_24",
        "ic_baseline_plane_24",
        "ic_baseline_planting_24",
        "ic_baseline_rotate_left_24",
        "ic_baseline_rotate_right_24",
        "ic_baseline_sanitize_24",
        "ic_baseline_savings_24",
        "ic_baseline_savings_2_24",
        "ic_baseline_school_24",
        "ic_baseline_search_24",
        "ic_baseline_shopping_bag_24",
        "ic_baseline_shopping_basket_24",
        "ic_baseline_shopping_cart_24",
        "ic_baseline_social_24",
        "ic_baseline_sports_24",
        "ic_baseline_star_24",
        "ic_baseline_store_24",
        "ic_baseline_tool_24",
        "ic_baseline_toys_24",
        "ic_baseline_transportation_24",
        "ic_baseline_travel_24",
        "ic_baseline_trophy_24",
        "ic_baseline_umbrella_24",
        "ic_baseline_videogame_asset_24",
        "ic_baseline_volunteer_24",
        "ic_baseline_wallet_24",
        "ic_baseline_warning_24",
    )

    fun getCategories(dbKeys: MutableList<String>): Map<String, Category> {
        return mapOf(
            dbKeys[0] to Category(
                dbKeys[0], "Electronics", "electronics", 1,
                "material_dark_blue_700", "ic_baseline_devices_24"
            ),

            dbKeys[1] to Category(
                dbKeys[1], "Emergency Funds", "emergency funds", 0,
                "material_bright_red_700", "ic_local_first_aid_24"
            ),

            dbKeys[2] to Category(
                dbKeys[2], "Financial Expenses", "financial expenses", 0,
                "material_dark_yellow_a400", "ic_baseline_wallet_24"
            ),

            dbKeys[3] to Category(
                dbKeys[3], "Food and Groceries", "food and groceries", 0,
                "material_teal_700", "ic_baseline_fastfood_24"
            ),

            dbKeys[4] to Category(
                dbKeys[4], "Housing", "housing", 0,
                "light_blue", "ic_baseline_home_24"
            ),

            dbKeys[5] to Category(
                dbKeys[5], "Investments", "investments", 0,
                "dark_green", "ic_local_investment_24"
            ),

            dbKeys[6] to Category(
                dbKeys[6], "Healthcare", "healthcare", 0,
                "darker_pink", "ic_baseline_health_and_safety_24"
            ),

            dbKeys[7] to Category(
                dbKeys[7], "Savings", "savings", 2,
                "pink", "ic_baseline_savings_24"
            ),

            dbKeys[8] to Category(
                dbKeys[8], "Shopping and Entertainment", "shopping and entertainment", 1,
                "material_dark_purple_700", "ic_baseline_shopping_bag_24"
            ),

            dbKeys[9] to Category(
                dbKeys[9], "Transportation", "transportation", 0,
                "dark_brown", "ic_baseline_directions_bus_24"
            ),

            dbKeys[10] to Category(
                dbKeys[10], "Utilities", "utilities", 0,
                "material_orange_700", "ic_baseline_electric_bolt_24"
            ),
        )
    }

    fun getCategoryItemCount(): Int {
        return categorySize
    }

    fun getColors(): List<String> {
        return colors
    }

    fun getIcons(): List<String> {
        return icons
    }
}