package com.ducatus.common

import com.ducatus.data.Category
import com.ducatus.data.Challenge
import com.ducatus.data.Tip

class AppResources {
    private val categorySize = 11

    private val colors = listOf(
        "lighter_red",
        "bright_pink",
        "material_bright_red_500",
        "material_bright_red_a400",
        "material_bright_red_700",
        "material_bright_red_a700",
        "darker_red",
        "material_red_a400",
        "material_red_500",
        "material_red_700",
        "material_red_a700",
        "pink",
        "dark_pink",
        "darker_pink",
        "rose_red",
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
        "material_blue_a700",
        "material_blue_a400",
        "blue",
        "light_blue",
        "dark_blue",
        "material_dark_blue_500",
        "material_dark_blue_700",
        "very_dark_blue",
        "material_light_blue_a400",
        "material_light_blue_500",
        "material_blue_500",
        "material_blue_700",
        "material_light_blue_a700",
        "material_light_blue_700",
        "material_cyan_a400",
        "material_cyan_500",
        "material_cyan_a700",
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
        "material_yellow_500",
        "material_yellow_a400",
        "material_yellow_a700",
        "material_yellow_700",
        "material_dark_yellow_500",
        "material_dark_yellow_a400",
        "material_dark_yellow_a700",
        "material_dark_yellow_700",
        "material_orange_500",
        "material_orange_a400",
        "material_orange_700",
        "material_orange_a700",
        "material_dark_orange_500",
        "material_dark_orange_a400",
        "material_dark_orange_a700",
        "material_dark_orange_700",
        "dark_brown",
        "light_gray",
        "dark_gray",
        "almost_black",
    )

    private val icons = listOf(
        "ic_baseline_account_balance_24",
        "ic_baseline_account_balance_wallet_24",
        "ic_baseline_apartment_24",
        "ic_baseline_architecture_24",
        "ic_baseline_auto_fix_high_24",
        "ic_baseline_bakery_dining_24",
        "ic_baseline_beach_access_24",
        "ic_baseline_bed_24",
        "ic_baseline_bento_24",
        "ic_baseline_bike_scooter_24",
        "ic_baseline_biotech_24",
        "ic_baseline_blender_24",
        "ic_baseline_brunch_dining_24",
        "ic_baseline_brush_24",
        "ic_baseline_bungalow_24",
        "ic_baseline_business_24",
        "ic_baseline_business_center_24",
        "ic_baseline_cable_24",
        "ic_baseline_cake_24",
        "ic_baseline_calendar_month_24",
        "ic_baseline_call_24",
        "ic_baseline_campaign_24",
        "ic_baseline_car_rental_24",
        "ic_baseline_car_repair_24",
        "ic_baseline_card_giftcard_24",
        "ic_baseline_card_membership_24",
        "ic_baseline_card_travel_24",
        "ic_baseline_carpenter_24",
        "ic_baseline_celebration_24",
        "ic_baseline_chair_24",
        "ic_baseline_charging_station_24",
        "ic_baseline_child_care_24",
        "ic_baseline_child_friendly_24",
        "ic_baseline_church_24",
        "ic_baseline_cleaning_services_24",
        "ic_baseline_cloud_24",
        "ic_baseline_coffee_24",
        "ic_baseline_coffee_maker_24",
        "ic_baseline_collections_bookmark_24",
        "ic_baseline_color_lens_24",
        "ic_baseline_commute_24",
        "ic_baseline_computer_24",
        "ic_baseline_connecting_airports_24",
        "ic_baseline_construction_24",
        "ic_baseline_cottage_24",
        "ic_baseline_countertops_24",
        "ic_baseline_credit_card_24",
        "ic_baseline_crib_24",
        "ic_baseline_currency_bitcoin_24",
        "ic_baseline_date_24",
        "ic_baseline_deck_24",
        "ic_baseline_delivery_dining_24",
        "ic_baseline_design_services_24",
        "ic_baseline_desk_24",
        "ic_baseline_devices_24",
        "ic_baseline_devices_other_24",
        "ic_baseline_diamond_24",
        "ic_baseline_directions_bus_24",
        "ic_baseline_directions_car_24",
        "ic_baseline_downhill_skiing_24",
        "ic_baseline_drafts_24",
        "ic_baseline_draw_24",
        "ic_baseline_dry_cleaning_24",
        "ic_baseline_earbuds_24",
        "ic_baseline_eco_24",
        "ic_baseline_edit_24",
        "ic_baseline_electric_bolt_24",
        "ic_baseline_electrical_services_24",
        "ic_baseline_email_24",
        "ic_baseline_ev_station_24",
        "ic_baseline_event_24",
        "ic_baseline_factory_24",
        "ic_baseline_fastfood_24",
        "ic_baseline_festival_24",
        "ic_local_first_aid_24",
        "ic_baseline_fitness_center_24",
        "ic_baseline_flag_24",
        "ic_baseline_flatware_24",
        "ic_baseline_forest_24",
        "ic_baseline_gamepad_24",
        "ic_baseline_golf_course_24",
        "ic_baseline_grass_24",
        "ic_baseline_handshake_24",
        "ic_baseline_handyman_24",
        "ic_baseline_headphones_24",
        "ic_baseline_health_24",
        "ic_baseline_health_and_safety_24",
        "ic_baseline_hiking_24",
        "ic_baseline_home_24",
        "ic_baseline_house_24",
        "ic_baseline_ice_skating_24",
        "ic_local_investment_24",
        "ic_baseline_kayaking_24",
        "ic_baseline_keyboard_24",
        "ic_baseline_kitesurfing_24",
        "ic_baseline_language_24",
        "ic_baseline_leisure_24",
        "ic_baseline_library_music_24",
        "ic_baseline_liquor_24",
        "ic_baseline_local_airport_24",
        "ic_baseline_local_dining_24",
        "ic_baseline_lock_24",
        "ic_baseline_luggage_24",
        "ic_baseline_medication_24",
        "ic_baseline_menu_book_24",
        "ic_baseline_mic_24",
        "ic_baseline_moped_24",
        "ic_baseline_more_horiz_24",
        "ic_baseline_movie_24",
        "ic_baseline_mouse_24",
        "ic_baseline_music_note_24",
        "ic_baseline_nightlife_24",
        "ic_baseline_notifications_24",
        "ic_baseline_outdoor_grill_24",
        "ic_baseline_paragliding_24",
        "ic_baseline_payments_24",
        "ic_baseline_pedal_bike_24",
        "ic_baseline_people_24",
        "ic_baseline_pets_24",
        "ic_baseline_phone_iphone_24",
        "ic_baseline_photo_camera_24",
        "ic_baseline_pool_24",
        "ic_baseline_print_24",
        "ic_baseline_radio_24",
        "ic_baseline_router_24",
        "ic_baseline_rowing_24",
        "ic_baseline_sailing_24",
        "ic_baseline_savings_24",
        "ic_baseline_scanner_24",
        "ic_baseline_school_24",
        "ic_baseline_science_24",
        "ic_baseline_scuba_diving_24",
        "ic_baseline_search_24",
        "ic_baseline_self_improvement_24",
        "ic_baseline_shopping_bag_24",
        "ic_baseline_shopping_basket_24",
        "ic_baseline_shopping_cart_24",
        "ic_baseline_skateboarding_24",
        "ic_baseline_sledding_24",
        "ic_baseline_sports_baseball_24",
        "ic_baseline_sports_cricket_24",
        "ic_baseline_sports_esports_24",
        "ic_baseline_sports_football_24",
        "ic_baseline_sports_golf_24",
        "ic_baseline_sports_gymnastics_24",
        "ic_baseline_sports_handball_24",
        "ic_baseline_sports_hockey_24",
        "ic_baseline_sports_martial_arts_24",
        "ic_baseline_sports_motorsports_24",
        "ic_baseline_sports_soccer_24",
        "ic_baseline_sports_tennis_24",
        "ic_baseline_sports_volleyball_24",
        "ic_baseline_star_24",
        "ic_baseline_store_24",
        "ic_baseline_surfing_24",
        "ic_baseline_theaters_24",
        "ic_baseline_tool_24",
        "ic_baseline_toys_24",
        "ic_baseline_two_wheeler_24",
        "ic_baseline_videogame_asset_24",
        "ic_baseline_volunteer_activism_24",
        "ic_baseline_wallet_24",
        "ic_baseline_warning_24",
        "ic_baseline_water_drop_24",
    )

    private val challenges = mapOf(
        7100 to Challenge(
            7100,
            "₱100 savings in 7 days",
            7,
            100
        ),
        7250 to Challenge(
            7250,
            "₱250 savings in 7 days",
            7,
            250
        ),
        7500 to Challenge(
            7500,
            "₱500 savings in 7 days",
            7,
            500
        ),
        71000 to Challenge(
            71000,
            "₱1000 savings in 7 days",
            7,
            1000
        ),
        14500 to Challenge(
            14500,
            "₱500 savings in 14 days",
            14,
            500
        ),
        141000 to Challenge(
            141000,
            "₱1000 savings in 14 days",
            14,
            1000
        ),
        142000 to Challenge(
            142000,
            "₱2000 savings in 14 days",
            14,
            2000
        ),
        303000 to Challenge(
            303000,
            "₱3000 savings in 30 days",
            30,
            3000
        ),
        305000 to Challenge(
            305000,
            "₱5000 savings in 30 days",
            30,
            5000
        )
    )

    private val challengesAmounts = mapOf(
        7100 to listOf(10, 15, 15, 15, 15, 15, 15),
        7250 to listOf(35, 35, 35, 35, 35, 35, 40),
        7500 to listOf(70, 70, 70, 70, 70, 75, 75),
        71000 to listOf(100, 100, 150, 150, 150, 150, 200),
        14500 to listOf(35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 35, 40, 40),
        141000 to listOf(70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 80, 80),
        142000 to listOf(140, 140, 140, 140, 140, 140, 140, 140, 140, 140, 140, 150, 150, 160),
        303000 to listOf(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
        305000 to listOf(160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 160, 200, 200, 200, 200, 200)
    )

    private val paymentTypes = listOf(
        "Cash",
        "Bank",
        "Credit Card",
        "Virtual Wallet",
        "Others"
    )

    private val subscriptionNotifications = listOf(
        "None",
        "On due date",
        "1 day before",
        "3 days before",
        "1 week before"
    )

    private val tipsArticles = listOf(
        Tip(
            "https://www.realsimple.com/work-life/money/money-planning/tips-for-first-time-budgeting",
            1659974400000, // "Aug 09, 2022",
            "4 Tips For First-time Budgeting",
            "Hiranmayi Srinivasan",
            "https://raw.githubusercontent.com/MakMoinee/makmoinee.github.io/main/tips.png",
        ),
        Tip(
            "https://moneytamer.com/budgeting-tips-for-beginners/",
            1580227200000, // "Jan 29, 2020",
            "Budgeting Tips For Beginners: How To Start A Budget That Works",
            "Steffa Mantilla, CFEI",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips2.png?raw=true",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips2Author.png?raw=true"
        ),
        Tip(
            "https://www.prulifeuk.com.ph/en/explore-pulse/health-financial-wellness/50-30-20-budgeting-hack/",
            0,
            "Is The 50-30-20 Budgeting Hack Right For You?",
            "Prolife UK",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips3.jpg?raw=true"
        ),
        Tip(
            "https://www.mymoneycoach.ca/blog/how-to-save-money-on-low-income",
            0,
            "4 Tips to Save Money on Low Income",
            "Kevin Sun",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips4.png?raw=true"
        ),
        Tip(
            "https://mint.intuit.com/blog/planning/money-101-27-financial-tips-to-live-by/",
            1651593600000, // "May 04, 2022",
            "Financial Advice: 12 Personal Finance Tips",
            "Matthew Amster-Burton",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips5.png?raw=true"
        ),
        Tip(
            "https://www.investopedia.com/articles/younginvestors/08/eight-tips.asp",
            1652457600000, // "May 14, 2022",
            "8 Financial Tips for Young Adults",
            "Amy Fontinelle",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips6.png?raw=true"
        ),
        Tip(
            "https://www.sunlife.com.ph/en/life-goals/grow-your-money/how-to-achieve-financial-stability-in-changing-times/",
            1660406400000, // "Aug 24, 2022",
            "How to achieve financial stability in changing times",
            "Sunlife",
            "https://github.com/MakMoinee/makmoinee.github.io/blob/main/tips7.png?raw=true"
        )
    )

    private val tipsVideos = listOf(
        Tip(
            "https://www.youtube.com/watch?v=CFrhSBwPJwU",
            1633622400000, // "October 08, 2021",
            "Budgeting 101 Guide for Pinoy",
            "Pinay Investor",
            "https://i3.ytimg.com/vi/CFrhSBwPJwU/maxresdefault.jpg"
        ),
        Tip(
            "https://www.youtube.com/watch?v=gct3D8v2cSo",
            1633104000000, // "October 02, 2021",
            "Financial Planning for Beginners",
            "Pinay Investor",
            "https://i3.ytimg.com/vi/gct3D8v2cSo/maxresdefault.jpg"
        ),
        Tip(
            "https://www.youtube.com/watch?v=OYY_FXec1jY",
            1633363200000, // "October 05, 2021",
            "Paano Gumawa ng Financial Plan | Building Your Financial Home | Financial Planning 101",
            "Pinay Investor",
            "https://i3.ytimg.com/vi/OYY_FXec1jY/maxresdefault.jpg"
        ),
        Tip(
            "https://www.youtube.com/watch?v=OyYL4C7nwvU",
            1640707200000, // "December 29, 2021",
            "How to Monitor Your Budget | Budget Tips | Free Budget Planner 2021",
            "Budget Pinay",
            "https://i3.ytimg.com/vi/OyYL4C7nwvU/maxresdefault.jpg"
        ),
        Tip(
            "https://www.youtube.com/watch?v=7_oy3AJI23s",
            1606665600000, // "November 30, 2020",
            "How To Create Monthly Budget Plan | Budgeting Tips",
            "Budget Pinay",
            "https://i3.ytimg.com/vi/7_oy3AJI23s/maxresdefault.jpg"
        ),
        Tip(
            "https://www.youtube.com/watch?v=WBf1Q53tv7I",
            1604937600000, // "November 10, 2020",
            "Top 7 Grocery Budget Tips | Tipid Tips",
            "Budget Pinay",
            "https://i3.ytimg.com/vi/WBf1Q53tv7I/maxresdefault.jpg"
        )
    )

    private val tipsContent = mapOf(
        "4 Tips For First-time Budgeting"
                to "There's a first time for everything, and it's never too late to get started on a budget. Creating a budget that you will stick to will help you get to know your money better and help ease financial anxiety—which is often caused by avoiding money decisions. Invest some time in getting to know your finances, whether that's with a financial coach, a partner, or just you and a trusty spreadsheet.\n\nSetting time aside for your money doesn't have to be boring or scary. Light some candles, grab a snack, take some deep breaths, and think of it as self-care; changing your approach to budgeting is important to make the whole thing more routine. Here are some expert tips that will have you budgeting like a pro in no time.\n\n01 of 04\nKeep it simple.\nWhen you're creating a budget for the first time, try to keep it as simple as possible. This way, it's not overwhelming and will be easier for you to maintain.\n\n\"A lot of people think that you have to have these complicated budgets, these complicated spreadsheets—and you grow to that,\" says Tonya Rapley, millennial money expert and founder of the financial education and lifestyle blog My Fab Finance. Rapley recommends simply writing down your expenses: purchases in the last month (plus annual or quarterly charges) and subtracting your income from your expenses. This will give you a good picture of how much you're saving versus spending, and allow you to identify areas where you can possibly cut back.\n\nA simple structure like the 50-30-20 budget can help you track your spending without having to maintain a bunch of categories, recommends Colleen McCreary, chief people officer and financial advocate at Credit Karma.\n\nSplit your expenses into three categories—50 percent for needs, 30 for wants, and 20 for savings or paying down debt. \"You can always change the percentages based on your lifestyle. Your budget has to work for you,\" says McCreary. You can also download a budget app for an even easier way to keep track of your budget and spending.\n\n02 of 04\nBase your budget off of your income.\nAlong with the overall structure of your budget, consider the type of budget that will be best for you. Rapley recommends basing your budget off of your income. So if you get paid every week, create a weekly budget; a monthly salary would get a monthly budget.\n\nFeel free to try out different formats and ways of organizing your budget until you find one that best fits your lifestyle and money goals.\n\nAlso observe how frequently you're spending money. \"If you're pulling out your credit card on a daily basis, you may want to set a weekly budget,\" says McCreary. \"A weekly budget may help you keep a closer eye on your day-to-day spending.\"\n\nMcCreary recommends opening a separate checking account specifically to keep track of your spending and keep it in check. Put some money on a debit card weekly or monthly based on your budget, and see how much of it you spend—or, ideally, save.\n\n\n\n03 of 04\nAvoid situations where you know you'll spend money.\nNow that you have made your budget, it's time to stick to it. Try not to put yourself in situations where you know you'll spend money, like going out for dinner or pedicures. \"One of the best ways to stick to a budget is to not pay people for things you can do yourself—until you get to a point where you can afford to pay people to do those things,\" says Rapley.\n\nAnother tip is to set aside one day a week or a few days month where you don't spend any money—cook meals at home or find free activities instead. \"You may be surprised how many seemingly small purchases add up over the course of a month, and how much you end up saving if you find certain days to avoid spending,\" says McCreary.\n\nCheck in on your spending every few weeks (or whatever deadline makes sense for you) to see how you're doing and if you need to adjust anything in your budget. Set realistic spending goals for yourself; being too rigid can have the opposite effect and can lead to impulse buying.\n\n04 of 04\nSet money goals and create a budget around them.\nSet some goals for your money, and use your budget to work towards them. Whether it's paying off student loans, buying a home, or saving for retirement, creating the right budget will help you get there. \"A budget can be a north star for those who want to hold themselves accountable as they work toward a specific financial goal,\" says McCreary.\n\nTo stay on track with your budgeting goals, Rapley recommends using a flexible payment tool like Affirm, which allows you to spread out the cost of purchases at retailers like Target and Walmart without any late fees.\n\nBudgeting will help you identify your spending habits—good and bad—so you can figure out any expenses you can do without and have a true picture of your cash flow in and out. Knowing your finances in this way will prepare you for unexpected expenses or financial setbacks.\n\nDon't be afraid to try different types of budgets until you find the right one (or a combination of methods). Your budget doesn't have to be perfect—it just has to be right for you.",

        "Budgeting Tips For Beginners: How To Start A Budget That Works"
                to "Tips For Creating A Budget For Beginners\nLearning how to budget can seem rather overwhelming to beginners at first. But there’s no need to be scared!\n\nOnce you start getting your head around a couple of simple budgeting principles, you’ll be able to really start saving money and see a difference in your finances right away.\n\nHere are the best budgeting tips for beginners, which will take you through everything you need to know about starting (and keeping up!) your first-ever budget from scratch. \n\nThis post may contain affiliate links. That means if you purchase an item through these links, I may earn a commission at no additional cost to you. Please read the full disclosure policy for more info.\n\n\nHow To Set Clear Budget Goals\nIf you’re reading this article, chances are you’re motivated to start budgeting. Maybe you want to finally feel financially secure and stop living paycheck to paycheck.\n\n1. Define Your “Why”\nMaybe you want to pay off debt or save up enough for a big trip. Whatever your motivation is, it’s very important to have a clear reason “why.”\n\nThis is because budgeting, especially as a beginner, requires conscious effort and repetition. Creating a new habit isn’t always fun. You’re already halfway there if you have your “why” for doing so.\n\nTo define your goals, it can help to get visual and put things down on paper. You could draft up a budgeting motivation list, or make a vision board. Whatever you need to do to keep your budgeting goals in mind!\n\nRelated: Use These 7 Reasons Why Budgeting Is Important to Inspire You\n\n2. Set Financial Goals\nNot only do you need to know why you want to start budgeting in the first place, but it’s also very important to think in terms of specific numbers.\n\nHow much debt are you hoping to pay off each month? How much money do you want to put aside in your emergency fund? How much will you be saving to go toward your retirement? \n\nYou cannot budget with the vague idea that you want to “save up some money”. Focus on numbers and focus on facts. \n\n3. Make Sure Your Goals Are Realistic\nMy final tip in defining your budgeting goals is to be realistic. Sure, it’d be great to be able to save \$200 a month into your traveling fund or to pay off all your college debt in the space of 2 years.\n\nThis may not always be possible unless you are willing to switch to an extremely frugal lifestyle. In order to start a budget from scratch, you’ll have to start small. Its small changes done consistently over time that compound into large wins.\n\nRemember, you can always adjust your budget to save more later. Aim to start with realistic goals, and you’ll be well on your way to become a great budgeter!\n\n\ncomputer with paper and pen for budgeting\nHow To Start Budgeting: Best Budgeting Tips\nThe following easy budgeting tips will walk you through the process of creating a monthly budget. You could have never budgeted a day in your life; it doesn’t matter. These steps show you a simple but effective way to budget your money.\n\n1. Keeping Track Of Paychecks\nThere are two basic parts to budgeting: determining your income and determining your expenses. So the first step that you need to take is adding up all your income sources, and figuring out how much money you have to go around each month.\n\nThis will include your day job(s), any additional side jobs you take on, and any passive income source(s). \n\nRelated: 6 Steps toEasily Keep Track of Bills and Payments\n\n2. Analyze Where You Are Spending Money\nNext, you’ll need to figure out where your money is going. A good first step to determine this is to have a look at your bank statement.\n\n\nTake some time to go through it, and try to see which categories are taking most of your income. Is it entertainment? Going out? Groceries?\n\nStart writing down each purchase you make in a notebook and go through it at the end of the month. These are my favorite budgeting notebooks to organize my finances.\n\nNot only will it give you an accurate idea of where your money is going, but it should also make you more mindful of your spending. \n\n3. Divvy Up Your Paycheck\nAfter you know where your money currently is being spent, it’s time to reassess and make decisions on how much money should be allocated to each budgeting category going forward\n\nHere are the recommended budgeting percentages to get you started.\n\nIt’s important to choose realistic amounts for each category. For a budget to be effective, you must stick to it so if the money in a category runs out, you won’t be able to spend more on it until the next month.\n\n4. Incorporate Sinking Funds\nWhen you go through this exercise, you may realize you have upcoming known events that will cost money. This includes Christmas shopping, birthdays, and even bills like life insurance or car maintenance.\n\n\nFor these non-monthly larger expenses, it can be beneficial to plan ahead for them and break up the cost into a monthly amount. Another word for this is a sinking fund.\n\nHere is a list of essential sinking fund categories you can include in your budget.\n\n5. Take Away Temptation To Overspend\nIf you’re using a debit or credit card, it can be easy to overspend. You aren’t tangibly losing cash during the purchase.\n\nOne way to combat this is to use the cash envelope method. You can use this system while starting out or continue with it if you like it. This easy guide walks you through how to use it.\n\nIf you end up liking this method and want an easy way to carry the envelopes around with you, then these are the wallets I recommend.\n\n6. Use A Budgeting App To Track Your Finances\nThis isn’t necessary but is a good option for those who prefer to track their money digitally. There are a ton that you can choose from now, from PocketGuard to Dollarbird.\n\nYour bank may even have an app that you can use that tracks your expenses directly. The great thing about using those is that they can really help you visualize what your expenses are.\n\nIt can be quite shocking to notice that 23% of your money is going toward brunch, or just how much you are spending going out on the weekends every month!\n\nWith their colorful graphics, not only will these apps give you a clear sense of where your money is going, but they can also make budgeting more fun. Well, as fun as it gets!\n\n\nRelated: Get on the same financial page as your spouse with this financial program.\n\nTrack all of your money in one place and set goals with this free financial dashboard.\n\nwood desk with woman writing in monthly budget planner notebook\nPlanning Your Regular Expenses & Savings\nOnce you have established where your money is going, you’ll need to make a distinction between recurring expenses and occasional ones. Expenses typically fall into these three categories:\n\n1. Monthly Expenses\nRecurring expenses are things like your water and electricity bills, your rent and your insurance. It can also cover categories like the cost of monthly medicine for regular treatments or paying your dog walker weekly.\n\n\n2. Yearly Expenses\nRegular expenses are budget items that you know will be occurring in the future. Taxes are one such expense. You know you will have to pay them every year.\n\nWork with an accountant to predict roughly how much you will be taxed in order to save that and about 10% more just in case. You can spread this total amount into a monthly line budget item so it won’t hit you all at once.\n\n3. Occasional Expenses\nAnything else belongs to the category of occasional expenses, meaning they are harder to predict. While you know roughly how much your rent will be 6 months from now, the amount that you spend on groceries can vary widely. \n\nPut your regular expenses down on paper, and make sure that you budget enough to pay them in full each month, with a little wiggle room. \n\n4. Savings\nSavings can fall into monthly, yearly, or occasional expenses. The easiest way to stay consistent would be to set it up as a monthly expense.\n\nI prefer to have it set to auto-draft like I would a recurring bill. This way, I don’t have to think about it and be tempted to spend the money elsewhere.\n\nLearn How To Separate Wants vs Needs In Your Budget\nOnce you have put money aside toward recurring expenses and savings, you can start thinking about clever ways to use what is now your spending money.\n\nAs a beginner trying to learn the secrets of budgeting, this often involves a complete shift in the way that you think about money. More specifically, it involves rethinking the difference between needs and wants. \n\nThere are a couple of things which you need to live:\n\nFood\nShelter\nMedicine\nClothing\nAnything that involves entertainment, going out, or buying clothes you already have are wants rather than needs.\n\nAs a new budgeter, you’ll need to pay attention to your needs first. How much money do you spend on groceries each month? Can that number be cut down? \n\nNext, you’ll need to start thinking about your wants. If your goals are to save more money than you did in the past, you’ll now have less expendable money to spend on your wants.\n\nRelated: Easy steps to set and achieve your money goals.\n\nThis may be hard to adapt to at first, but it will teach you self-control. As much as possible, stay away from reckless expenses.\n\nThis means, don’t impulse shop. It can be a good idea to write down the things which you want to buy and give yourself a couple of weeks to really ponder the reasonableness of that purchase.\n\nDo you really need another rain jacket? Is it reasonable to buy two new sweaters in a month?\n\nRemind yourself that there is a difference between a need and a want.\n\nWanting something is not a good enough reason to get it. Learn to restrict yourself, and you’ll be well on your way to having a working budget. \n\nwood plank desk flat lay with open budgeting notebook and sticky notes.\n\nCommon Budgeting Mistakes\nThere are two things which we tend to spend way more money on than we should. These are subscription services, and eating out. \n\n\n\nReassess Subscription Services\nSubscriptions like Netflix, Spotify, magazines, Headspace, Amazon Prime and all sorts of other services get you to join their program by advertising a low monthly cost.\n\nYou will only be spending a couple of dollars a month for a wealth of benefits, they claim. However, these few dollars spent here and there do add up and can make your monthly expenses go up by dozens of dollars.\n\nSo take some time to reassess everything that you are subscribed to, and see if there are a couple that you could live without. It’s good to limit your recurring expenses as much as you possibly can.\n\nAre you getting the most out of your Amazon Prime subscription? Prime money-saving hacks you’re not doing.\n\nBe Mindful Of How Frequently You Eat Out\nEating out can quickly become a major expense. Between going out to a fast-food restaurant for lunch, meeting a friend for coffee or dessert, and ordering pizza when you don’t feel like cooking, these expenses can add up to a considerable portion of your income.\n\nOne of the best budgeting tips for beginners is simply to learn to cook. A home-cooked meal can be anything from half to a tenth of the price of a restaurant-bought one.\n\nSo do yourself a favor and have a long, hard look at your eating-out expenses. Should you really be spending that much money on things you could learn to cook for yourself?\n\n\nYou will also join the weekly email list and can unsubscribe anytime. Terms & Conditions and Privacy Policy.\n\nSaving Money For Beginners: A Mindset Change\nNow that you understand how to make a basic budget, you can now focus on changing your savings mindset. This is an area that many people struggle with.\n\n\n\nBudgeting and saving money is not just about planning all your income and expenses at the beginning and end of the month. It is an ongoing process that can take a bit of time to master.\n\nAs a general rule, try to find little things you can do each day to save a bit of money. It could be aiming to take shorter showers to lower your water bill (and help the planet!) or buying unbranded pasta as opposed to the brand which you are used to getting.\n\nRelated: Frugal Depression Era money-saving tips you can do today.\n\nThese small everyday gestures can add up to big spending over time. Plus, once they become second nature, you’ll be saving money day to day completely effortlessly! \n\nReassess Your Budget Regularly\nWhen learning how to start a budget from scratch, it’s important to remember that this is a process as opposed to a one-time thing.\n\nYou should reassess your budget regularly, every single month if possible.\n\n\n\nFirst, this is to ensure that your budget is still working for you, that you are saving as much as you can, but not restricting yourself so much that you feel miserable.\n\nSecond, you need to adjust it for any unforeseen changes in either your income or expenses. Maybe you lost your part-time job and now have slightly less to spend, maybe your rent went up, or maybe you switched to cheaper insurance.\n\nWhatever it may be, make sure that analyzing and planning your budget is something that you sit down to do on a regular basis. This will ensure that you have the best budget for you, every single month. \n\nGive Yourself Time To Master Budgeting\nThe final tip in this Budgeting 101 article is to be patient with yourself. You cannot expect to halve your spending in a month.\n\nNeither can you expect budgeting to feel easy in the first few weeks. Learning how to create a budget and stick to it takes time and determination.\n\nSo be kind to yourself. Remind yourself that this is a process, and that change takes time.\n\nThe changes you make today may seem hard and at times frustrating, but they will soon develop into a habit that you don’t even have to think about.\n\nTry to keep your motivation for budgeting up by constantly referring to your goals, and have a little self-compassion. Budgeting from scratch can be hard, but it’ll only get easier the longer you keep to it!",

        "Is The 50-30-20 Budgeting Hack Right For You?"
                to "With the value of the peso shrinking against the US dollar and prices of commodities going up due to inflation, you've surely noticed: you’re buying less with your salary.\n\nMore than ever, effective budgeting and financial planning are urgently needed to survive the negative effects of inflation. Let’s look into one popular budget hack and see if this can really help us cope with rising prices.\n\n50/30/20\n\nOne popular guide to budgeting is the 50/30/20 guide that has been shared widely online.  It recommends dividing your income in this way:\n\n50% - Spend for your needs. These include basic necessities like housing, food, utilities, health care (insurance, treatments), or car payments.\n\n30% - Spend for your wants. These would include hobbies, recreation (movies, eating out, etc.), gadgets, cable TV, etc.\n\n20% - Set aside for savings. You should also set this aside to pay for any outstanding debts.\n\n \n\nThis makes a lot of sense as a general rule of thumb. However, in an economy where inflation and a weak currency are a problem, we would likely need more help than just this budget plan.\n\nA woman working with receipts and journal\n‘Hacking’ your budget may help you manage your money.\nNot for low-income, heavy debts\n\nSimply put, the 50/30/20 budget plan only succeeds with a certain income range. People with lower incomes will find it nearly, if not flat-out impossible, to apply that in their lives.\n\nAccording to the National Economic Development Authority (NEDA), for a typical family of four to have a simple and comfortable life, they should have at least a gross monthly household income of PhP 120,000. For NEDA, a “simple and comfortable life” means a family can afford a car, a medium-sized house, college education, and leisure.\n\nFor an average public school teacher who could only earn a little over PhP 20,000, he/she will have to forego a “simple and comfortable life” if he/she is the sole breadwinner of a family of four.\n\nAfter all the deductions for his/her GSIS or SSS (PhP 581), PhilHealth (PhP 275) and PAGIBIG Fund (PhP 100) contributions he/she would take home only about PhP 19,000.\n\nConsidering the monthly rent for one-bedroom apartment outside Metro Manila, which averages about PhP 7,000 outside Metro Manila or PhP 12,500 within the metro according to Numbeo,1 he/she would have about PhP 12,000 or as little as PhP 6,500 left to budget for his/her family’s daily needs. This includes food and transportation, among many others – and this could be an uncomfortable budget to work on for most people.\n\nTo survive, a household should have at least two breadwinners earning about PhP 21,000 each. This only means that their take home pay would only be enough to cover bare necessities such as food and lodging. With this take home pay, a family can only afford supporting two students studying in a public school and taking public transport.\n\nClearly, the 50-30-20 budget would be tough for those earning less than PhP 120,000 a month, and practically impossible for minimum wage earners. Minimum wage in the Philippines differs according to region and ranges from PhP 255 to PhP 512 per day as of September 2018. The average minimum monthly salary in the Philippines is a little over PhP 10,000.\n\nAnother reason why a 50/30/20 budget plan would not work for many Filipinos is heavy debt. Many Filipinos deal with a lot of debt. According to a Business World report2, “[Filipinos'] average debt across age groups stands at PhP 291,582 for those aged below 35; PhP 207,418 for those aged 35 to 49; and PhP 143,958 for those 50 and older.”\n\nTo pay off such heavy debts, a more detailed and personalized budget plan is needed.\n\nFinancial planning\n\nWhether you are earning minimum wage or more, sound financial planning is still the best option. Instead of trying in vain to stick to a 50/30/20 budget plan, why not get advice from a professional financial planner?\n\nOnly then can you find a tailor-fit financial plan that is designed for your unique situation. Your financial planner will consider your current income, capacity to earn, values, material and non-material assets, daily and monthly expenses, and needs.\n\nOnly when your financial planner has all of these data can he or she craft a financial plan that will work for you. This will help you spend wisely, save more money, pay off debts, plan a comfortable retirement, and even have enough money left to invest.\n\n---\n\nSources:\n\n1 Numbeo. Cost of Living in the Philippines \n2 BusinessWorld. Many Filipinos found to be still dependent on debt ",

        "4 Tips to Save Money on Low Income"
                to "Do you struggle with saving money on low income? When every pay cheque you earn is just barely enough to cover your spending, setting some cash aside each month is likely the last thing on your mind. If you’re in a situation where you’re worried about how to survive on a low income budget, then saving might be one of your last priorities. However, if that’s not quite your situation and you’re instead focused on a goal such as saving for a house or want strategies on effective money management for low income families, then here are 4 tips that might help:\n\nCouple Struggling to Save Money on Low IncomeIs Your Income Low, or Are Your Expenses High?\nOur first tip is to take a look at the big picture of both your income and your expenses, all the money that you spend. No matter how much or how little you earn there is never a guarantee of financial wellbeing. A higher income household that has a lot of debt is in fact further behind than a family who takes home much less and has no debt – this family could build up their savings despite lower income. What you spend is just as important as what you earn when it comes to saving money.\n\nKnowing your income might be as simple as looking at your pay cheques, but knowing your expenses is often much trickier. The best way to get started is to record everything that you spend money on, either on paper or in an easy-to-use expense tracker. After a few weeks, you should have enough info to start seeing if there are any places where you could spend less to save more. You might be surprised at how fast this works to boost your savings each month, even at a low income.\n\n7 Spending Tips that Can Save Thousands\n\nThink of Saving Money as Paying Yourself, and Pay Yourself First\nYou might never have thought about it this way, but most of our income is spent paying other people. Canadians usually pay their rent or mortgage, groceries, household bills, and whatever other costs first before thinking about their savings. But if you worry about everything other than your savings first, then when you finally get around to it, there likely won’t be much money left to save.\n\nSaving money is like paying yourself because the cash stays in your pocket, not someone else’s. Those funds can then be put towards your savings goal, work for you in TFSA and RRSP investments, and help your family in financial emergencies. That’s why you should make paying yourself (i.e. saving money) a top priority. In other words, decide how much money you want to pay yourself each month and do that before, not after, paying your other expenses.\n\nHow to Invest Your Money\n\nSave Money in Different Bank Accounts\nWhen your income is low, you want to make sure that every dollar counts. Organizing what you earn into different savings accounts helps make sure that everything goes where it should. Another benefit of doing this is that it can make managing your money easier, especially if you automate the process. Here are the steps to do that:\n\nHave a main account that your pay cheques get deposited into.\nSet up a savings account, or several if you want. For example, you could have separate accounts for your emergency fund, vehicle or home repairs, clothes for your kids, saving for a house, etc. Depending on your bank or credit union, you might have to pay service charges for extra savings accounts. Shop around and find the most cost-effective way to organize what you need.\nBased on your budget, decide how much money you want to put into each savings account each month.\nSet up recurring automatic transfers from your main account into your savings account(s). You can do this from your online banking platform; call your bank if you need help.\nAfter setting this all up, you can forget about it and let your savings accumulate by itself. Then when you do need to tap into one of those accounts, you can do so without worrying about taking away from any of your other savings goals. If having too many accounts is confusing, just use one account and keep track of how much of the money is for each goal using a notebook or spreadsheet. There’s really no wrong way to do it – find what works best for you.\n\nMake the Most of Your Income with a Budget\nOur final and most important tip for saving money on low income is to put everything together into a budget. A budget is really nothing more than a plan for how you want to spend your money. It can help you keep your expenses down, focus on paying yourself first, and decide how to organize your bank accounts. Whether your income is low, high, or somewhere in-between, a solid budget will always help you save money and reach your financial goals.\n\n7 Steps to a Budget Made Easy\n\nGet Help with Budgeting to Save Money, No Matter Your Income\nThere’s no secret formula for a low income budget that you need to learn to make an effective money management strategy. Any budget planner can help you get started, including this web-based one. However, if you would like to get expert assistance with making a budget that works with your needs and income, a non-profit credit counselling organization would be happy to guide you through the process. Find help by calling a nearby credit counselling organization in your area. The best part: budgeting help is free!",

        "Financial Advice: 12 Personal Finance Tips"
                to "Managing your money isn’t always a fun activity, especially when your friends are begging you to go out on the weekend or the latest tech gadget you’ve had your eyes on hits the market. Spending your money can be very tempting, and can lead to poor financial choices. However, there are plenty of ways you can manage your money wisely, while still enjoying life’s simple pleasures.\n\nIn this post, we’ll provide our best financial advice to help you manage your personal finances better.\n\nBelow, we’ll break down our top personal finance tips into three categories: the basics, budgeting, and saving. You can read through to view our financial tips to help you get your finances in order, or use the links to jump to a category of your choosing.\n\nFinancial Advice Basics\nBuy the Right Insurance\nUse Your Credit Card Wisely\nDon’t Forget Your Taxes\nKeep Track of Interest Rates\nFinancial Tips for Budgeting\nBudget for College Early\nCarefully Plan When Buying a House\nTake Advantage of Budgeting Resources\nTry the 50/30/20 Budgeting Rule\nMoney Tips for Saving\nSave Early\nMake Smart Investments\nFocus on Family Finances\nSave for the Unexpected Emergency\nKey Takeaways\nFinancial Advice Basics\nNo matter who you are, there are certain financial advice basics you should follow. Doing so can help ensure you keep your personal finances in good health. Once you have the basics down, managing your finances can become much easier. Then, you can move onto some of my more comprehensive money management tips.\n\n\n\n1. Buy the Right Insurance\nInsurance can be great in unfortunate events, whether a natural disaster tears off your roof or you get in a car accident. However, too many people are often roped into insurance plans that cost too much.\n\nConsiderations for different types of insurance:\n\nLife Insurance: Is it worth it?\nIf someone depends on your income, buy life insurance. However, if you don’t have dependents, life insurance isn’t always necessary.\n\nWhat types of insurance should you definitely have if you can afford it?\nYou should also consider insuring against financial disasters, not just annoyances. Buy renters or homeowner’s insurance, car insurance, disability insurance, and health insurance.\n\nWhat insurance policies can you likely skip on?\nBy the same token, you might want to steer clear of extended warranties, smartphone insurance, travel insurance, or payment protection plans, as these might not always be necessary and can cost you a lot of money.\n\n2. Use Your Credit Card Wisely\nCredit cards are useful but can be dangerous — kind of like power tools. Using them frequently can make it more likely that you’ll cut your thumb off, so to speak. A lot of sad stories begin, “I always paid off my credit card every month, until…”\n\nHow you can use your credit card wisely:\nUsing your credit card wisely and keeping your credit utilization ratio below 30 percent can help you keep your credit score in check.\n\nWhy it’s important:\nPreserving your credit score is important, as it’s used for a variety of financial matters, such as taking out a mortgage or applying for an auto loan.\n\nHow to keep an eye on your credit score:\nWith that said, make sure to check your free annual credit reports for errors. This should be a regular action you take at least once a year. By periodically pulling a credit report, you can look for any errors or mistakes that might be lowering your credit score.\n\nCredit scores are simpler than you think. If you pay your bills on time, more than likely, you’ll have a good credit score. If you don’t, you won’t. \n\n3. Don’t Forget Your Taxes\n\n\nNobody likes paying and filing taxes, but failing to do so can lead you into serious financial trouble.\n\nWhen are taxes due?\nTaxes either come around once a year or quarterly, depending on your occupation. With that said, make sure you have a financial calendar that reminds you when to pay or file your taxes. \n\nHow can you save on your taxes?\nOne piece of financial advice for saving on taxes is to contribute to your 401(k) or other retirement plans. Clever tax-avoidance schemes are often illegal, so make sure you’re honest and make legal choices that can save you money.\nIf you always hire someone to do your taxes, try doing it yourself (or use tax software) once. If you always do it yourself, try hiring someone. Either way, you might save money or learn something.\n4. Keep Track of Interest Rates\nWith almost any financial move you make, interest rates will follow. Credit cards, student loans, mortgages, savings accounts—these are just some of the types of loans, debts, and financial accounts you’ll have that come with an interest rate.\n\nWhy is it important to keep an eye on interest rates?\nKnowing the interest rates on these various types of accounts is essential because you might be spending more or earning less on your various debts and savings commitments. It’s best to keep an eye out on your interest rates, so you know what accounts to focus on.\n\nFinancial Tips for Budgeting\nBudgeting is one of the most important personal finance tips. Without a budget, you can easily be spending more money than you earn, which can make it challenging to:\n\nPay off debts\nSave for the future\nAfford an emergency expense\nConsider these budgeting tips as you plan for the future.\n\n\n\n5. Budget for College Early\nStudent loans are awful.\n\nWhat do you need to know about college debt?\nTraditional four-year colleges are often unaffordable without taking on massive debt—and don’t necessarily provide a better education.\n\nWhat can you do to prepare?\nIf you’re a middle-class family, it might be worth considering sending your kids to a community college, in-state public university, military academy, or elite private college. This way, they won’t take on as much in student loans that can take decades to pay off.\n\nWhen should you start saving?\nRetirement savings come before college savings. If you can’t afford to save for your kid’s college, don’t make it a priority quite yet. Even if you can’t afford to save now, open a 529 college savings plan for grandparents or other family members to contribute to.\n\n6. Carefully Plan When Buying a House\nAggressively paying down a mortgage is another important personal finance tip worth considering.\n\nThe best measure of your readiness to buy a house is the size of your down payment. Be wary of making a down payment under 20%, even through a government loan program.\n\nStretching to buy more house than you can afford can often lead to painful and avoidable financial misery.\n\n7. Take Advantage of Budgeting Resources\nYou don’t have to go at budgeting alone. Carefully tracking your finances without any help can be overwhelming and stressful. Fortunately, there are plenty of resources out there that can help you track your income and expenses and make smart financial moves.\n\nMymoney.gov has plenty of financial wellness tips that you can take advantage of and learn a thing or two.\nAt Mint, you can use our free budgeting app that can help you manage all of your finances in one place, including your bills, balances, and credit score.\n8. Try the 50/30/20 Budgeting Rule\nSometimes, all you need is a little guidance to help you build a strong and manageable budget.\n\nWhat is the 50/30/20 budgeting rule?\nA great money management tip is following the 50/30/20 budgeting rule, which goes as follows:\n\n50 percent of your income goes toward your essentials, such as housing, food, transportation, and utilities\n30 percent of your income goes toward your wants, such as a nice smartphone, entertainment, and travel\n20 percent of your income goes toward your savings and debt repayments, such as your student loans, medical loans, and auto loans\n\n\nHow can the 50/30/20 budgeting rule improve your finances?\nIncome management is an essential skill needed to budget correctly, and with the 50/30/20 budgeting rule, you can make this happen. With this budgeting rule, you can create a solid plan to meet your financial goals by identifying areas where you can cut or reduce expenses. \n\nMoney Tips for Saving\nManaging your money can be a challenging task, especially when you have important expenses to pay like rent, student loans, utilities, groceries, and so forth. However, there are plenty of ways you can still pay for your necessities while treating yourself to things you love, all while saving.\n\nConsumerfinance.gov has plenty of smart financial tips and tricks that can help you start saving.\n\n\n\n9. Save Early\nThere is no shame in using tricks to get yourself to save money.\n\nWhat are some tips for saving more?\nUse multiple savings accounts\nPut your credit card in the freezer\nSet up automated transfers\nThink of your next raise as an opportunity to save more (not an opportunity to spend more)\nWhatever works for you is fine. The key is to begin saving as early as you can, even if it means setting aside a few dollars in a piggy bank. Getting in the habit of saving early can set you up for financial success in the future.\n\nWhat about cutting expenses?\nIt’s also important to look at your lifestyle and identify areas where you can cut expenses. Often, the best way to make saving a habit isn’t skipping lattes; it’s keeping your housing and transportation expenses low.\n\nShould you be focused on saving for retirement?\nWhen it comes to saving, retirement should always be part of the conversation. The last thing you want is to enter your golden years and realize you can’t retire because you won’t have enough money to make ends meet. The earlier you begin saving, the better.\n\n*Pro-tip: With Mint’s retirement calculator, you can see how much you need to save in order to make your retirement dreams a reality.\n\n10. Make Smart Investments\nInvesting can be a great way to boost your savings and make extra income that can be put toward necessary expenses.\n\nWhat are some ways to start investing?\nWhile it’s possible to beat the market, it’s often so unlikely that it might not be worth trying. Instead, consider investing in inexpensive index funds or target-date funds, as you can reduce your risk of losing large sums of money.\n\nIt’s always important to avoid investing in anything that promises impressive returns with little or no risk.\n\nWhat about retirement savings?\nYou can also invest in your retirement savings. Try and max out your tax-advantaged accounts, such as your 401(k) or IRA, before investing in a taxable account. This way, you can invest in a less risky manner, all while growing your nest egg.\n\n11. Focus on Family Finances\nCouples have assorted ways of merging and managing their finances. No matter your relationship dynamic, it’s important to find common ground when it comes to managing family finances. This way, you’ll be able to make plans for buying a new home, saving for your children’s college, or buying a new car.\n\nRetirement planning:\nHaving said that, couples who intend to spend retirement together should consider looking at their investment portfolio as a single unit. Doing so can allow you to create a retirement plan, so that you can spend your golden years the way you’ve always envisioned. \n\nMoney management and education:\nIn addition to focusing on your and your spouse’s finances, it’s important to teach your children smart financial moves to help set them up for success. For example, forcing kids to save or donate part of their allowance can sometimes deprive them of the opportunity to learn worthwhile lessons.\n\nWith minimal financial obligations, your kids can learn from an early age what makes a financial decision risky, so when they’re older, they’ll be able to reflect on that experience and make the right choice.\n\n12. Save for the Unexpected Emergency\nYou can never predict the unpredictable, which is why you want to have a plan in place should an unexpected emergency arise. Today, many Americans found just how important emergency savings are due to the coronavirus pandemic. With millions of workers now unemployed, savings have never been more important.\n\nWhat constitutes an emergency expense?\nFender benders, medical bills, a leaky roof—these are just some of the surprises life might throw your way, which can leave a serious dent in your finances if you don’t have adequate emergency savings.\n\nWhat can you do to prepare for emergencies?\nTo prepare for future economic downturns, you can review Mint’s recession finance tips that can help you get through any financial disruption.\n\nTo create a rainy day fund, set aside a portion of your income in a savings account that you won’t be tempted to touch. It’s recommended to have anywhere between six months and a year’s worth of savings stored in an emergency fund. This way, if you lose your job, have to buy a new car, or need to pay for an expensive surgery, you won’t face economic hardship. \n\nKey Takeaways\nFinancial advice basics: Make sure you buy the right insurance, use your credit cards wisely, stay on top of your taxes, and be aware of interest rates on any loans and savings accounts.\nFinancial tips for budgeting: Set aside funds early for college, buy a house that you can reasonably afford, take advantage of budgeting resources, and try the 50/30/20 budgeting rule.\nMoney tips for saving: Save as early as possible, especially for retirement, make smart investments that aren’t extremely risky, focus on your family finances, and create an emergency fund.\nWhat would you add? Let’s hear your financial wisdom at its pithiest.",

        "8 Financial Tips for Young Adults"
                to "A class titled “Finance for Young Adults” usually isn’t part of a high school curriculum—an unfortunate oversight that leaves many young people clueless about how to manage their money, apply for credit, and stay out of debt. Although some progress has been made—23 U.S. states required a personal finance course and 25 required an economics course for high school graduation in 2022—there are still large knowledge gaps in this age group.\n1\n\nBasic economic and financial education in high schools should help at least a segment of the next generation, but young adults in the crucial post-high school years also need to master core lessons about money. Learn more about how to start managing your money from the very beginning of your financial life.\n\nKEY TAKEAWAYS\nTaking the time to learn a few basic financial rules can help you build a healthy financial future.\nLearning to prepare your annual tax return yourself could save you money.\nStart an emergency fund and pay into it every month, even if it is a small amount.\nSaving for retirement is an integral part of any financial plan, and starting young gives you the maximum time to grow your nest egg. \n8 Essential Finance Tips for Young Adults\nThe sooner you start learning to manage your money, the better your chance of financial success will be throughout your life. If you are just starting out, there are eight steps you can take now to protect your financial health, start saving, and build wealth throughout your life.\nPay with cash, not credit.\nEducate yourself on personal finance.\nLearn to budget.\nStart an emergency fund.\nStart saving for retirement early.\nStay on top of your taxes.\nGuard your health.\nProtect your wealth.\nPractice Self-Control: Pay With Cash, Not Credit\nIf you’re lucky, your parents taught you self-control when you were a kid. If not, keep in mind that the sooner you learn the essential life skill of delaying gratification, the sooner you’ll keep your personal finances in order as a matter of habit.\n\n\nOne of the most important ways to exercise self-control with your finances is also very simple. If you wait until you’ve saved the money for whatever it is you need, then you can put all everyday purchases on a debit card instead of a credit card. A debit card deducts the money from your checking account immediately (with no additional fees), but a credit card—unless you can afford to pay off the balance in full every month—is actually a high-interest loan.\n\nIf you get into the dangerous habit of putting all your purchases on credit cards, then not only will you be paying interest on a pair of jeans or a box of cereal, but you could also still be paying for those items in 10 years.\n\nCredit cards are certainly useful; some offer great rewards; paying them off on time helps you build a good credit score. However, it is essential to use them to your advantage—not to the advantage of the lender who profits from your bad habit of racking up interest-bearing balances. Keep credit cards for emergencies only—and always pay your balance in full when the bill arrives. Also, don’t apply for every credit offer you receive—and never carry more cards than you can keep track of.\n\nBeware of Bad Advice: Educate Yourself\nIf you don’t learn to manage your money, then other people will find ways to mismanage it for you. Some of these people could have bad intentions, like unscrupulous financial planners. Others may be well-meaning, but not fully informed about your circumstances, like relatives who make blanket recommendations about the importance of owning your own house—even though the only way you could afford to buy right now would be taking on a risky adjustable-rate mortgage.\n\nInstead of relying on random advice from unqualified people, take charge of your own financial future and read a few basic books on personal finance. Once you’re armed with knowledge, don’t let anyone get you off track—whether it’s a significant other who siphons off your bank account or friends who want you to go out and blow tons of money with them every weekend.\n\nKnow Where Your Money Goes: Learn to Budget\nOnce you’ve read a few personal finance books, you will understand the importance of two rules that every personal finance advisor keeps repeating. Never let your expenses exceed your income, and always keep your eye on where your money goes. The best way to do this is by budgeting and creating a personal spending plan to track the money you have coming in and the money you have going out.\n\nOnce you actually start tracking how you spend your money, it can be a valuable wake-up call to realize how the cost of buying coffee from a barista every morning adds up over the course of a month. Unlike a salary increase, which is in the hands of your boss to a large extent, small changes in your everyday expenses, like making coffee at home, are completely under your control—and they can have as big an impact on your financial situation as getting a raise.\n\nKeeping your larger monthly expenses—like rent—as low as possible can save you even more money over time. Even if you can swing an amenity-packed apartment right now, choosing a simpler place—and banking the cash you save—could put you in a position to own a condominium or a house much sooner than your friends who are paying high rent.\n\n Understanding how money works is the first step toward making your money work for you. \nPay Yourself First: Start an Emergency Fund\nOne of the most-repeated mantras in personal finance is “pay yourself first,” which means saving money for emergencies and for your future. This simple practice not only keeps you out of trouble financially, but it can also help you sleep better at night. Even on the tightest budget—no matter how much you owe in student loans or credit card debt, no matter how low your salary is—there are ways to put at least some of your money into an emergency fund every month.\n\nAn added benefit is that, if you get into the habit of socking away money into savings automatically, then you will stop treating savings as optional—and start treating it as a required monthly expense. Before long, you’ll have more than just emergency money saved up—you’ll have retirement money, vacation money, or even money for a down payment on a home.\n\nIf you put your cash into a standard savings account, it will be secure and available whenever you need it. However, that kind of account will earn almost no interest—which means that inflation will erode the value of your savings over time. Instead, you can put your fund in a high-yield savings account, short-term certificate of deposit (CD), or money market account. Just make sure the rules of your savings vehicle permit you to get to your money quickly in an emergency.\n\nStart Saving for Retirement Now\nJust as your parents sent you off to kindergarten to prepare you for success in a world that seemed eons away, you need to plan for your retirement well in advance—that is, right now.\n\nAn excellent way to get started on the right path is to educate yourself about the power (some say magic) of compound interest. Once you do, the wisdom of starting your retirement fund as soon as possible will be undeniable. The simplest way to think of compound interest is as “interest on interest,” which means that you will earn interest not only on the principal (the money you put in), but also on the interest (the money the bank pays you for holding your principal).\n\nBy making your money grow at a much faster rate than simple interest, which is calculated only on the principal, compound interest super-charges your savings—especially over time.\nWhy start saving for your retirement in your 20s? Again, because of the way compound interest works, the sooner you start saving, the less principal you have to invest to end up with the amount that you need to retire.\n\nHere’s an example: You start investing in the market at \$100 a month, averaging a positive return of 1% a month (which is 12% a year), compounded monthly over 40 years. Your friend, who is the same age, doesn’t begin investing until 30 years later and invests \$1,000 a month for 10 years, also averaging 1% a month (12% a year), compounded monthly. After 10 years, your friend will have saved around \$230,000. Your retirement account will be a bit over \$1.17 million.\n\nCompany-sponsored retirement plans are a particularly great choice. Not only do you get to put in pretax dollars (which lowers the income tax you pay), but many companies will also match part of your contribution, which is like getting free money. Contribution limits tend to be higher for 401(k)s than for individual retirement accounts (IRAs), but any employer-sponsored plan that you’re fortunate enough to be offered is a step closer to financial health.\n2\n\nIf you don’t have access to a company plan, don’t despair. Those who are self-employed have a range of options for setting up retirement plans. Others can open their own IRAs, allowing for a set amount of money each month to be withdrawn from their savings account and contributed directly into their IRA. Even if it’s only a small sum, it will eventually add up to something substantial.\n\nStay on Top of Your Taxes\nBefore you even get your first paycheck, it’s important to understand how income tax works. When a company offers you a starting salary, you need to calculate whether that salary will give you enough money after taxes to meet your financial obligations—and, with smart planning, meet your savings and retirement goals as well.\n\nFortunately, there are plenty of online calculators that take the grunt work out of determining what your after-tax salary will be, such as PaycheckCity.com.\n3\n These calculators will chart your gross pay (total earnings), how much goes to taxes, and your net pay (earnings after taxes and other deductions, also known as take-home pay). For example, in 2022, an annual salary of \$35,000 in New York City would leave you with around \$28,270 after federal and state taxes (without exemptions)—about \$2,356 a month. (Then you need to consider city taxes as well.)\n\nIn another scenario, perhaps you’re considering leaving one job for another to get a salary increase. Before you do this, you’ll need to understand how your marginal tax rate—the tax rate you pay on additional income—will affect your raise.\n\nIn the U.S., low-income earners are taxed at a lower rate than higher-income earners—the higher your salary, the higher the tax rate. For example, a salary increase from \$35,000 a year to \$41,000 a year looks like an extra \$6,000 per year (\$500 per month)—but the tax rate will be higher, so it will only give you an extra \$4,227 (around \$352 per month). (The amount will vary depending on taxes in your state of residence.) If you’re considering a move, keep that in mind.\nFinally, take the time to learn to do your own taxes. Unless you have a complicated financial situation, it’s not that hard to do, and you won’t have to pay a tax professional. Tax software has made doing your own taxes much easier than it used to be—and software also ensures that you can file online.\n\nGuard Your Health\nIf paying monthly health insurance premiums seems impossible, what will you do if you have to go to the emergency room—where a single visit for a minor injury like a broken bone can cost thousands of dollars? If you’re uninsured, don’t wait another day to apply for health insurance. It’s easier than you think to wind up in a car accident or trip and fall down a flight of stairs.\n\nIf you’re employed, then your employer may offer health insurance, including high-deductible health plans that save on premiums and qualify you for a Health Savings Account (HSA). If you need to buy insurance on your own, investigate the federal and state plans offered by the Health Insurance Marketplace of the Affordable Care Act (ACA). Look at quotes from different insurance providers to find the lowest rates. Research all your options to see if you qualify for a subsidy based on your income. If you have health issues, know that a more expensive plan could be the most cost-effective in the end.\n\nIf you’re under age 26, then your best choice may be to stay on your parents’ health insurance—an option that has been allowed since the 2010 passage of the ACA. If you can manage it, offer to reimburse your parents for the cost of keeping you on their plan.\n\nIt also makes excellent financial sense to build staying healthy into your daily routine as soon as possible. Common-sense health maintenance is very straightforward, and you've heard it all before. Eat fruits and vegetables, maintain a healthy weight, exercise, don't smoke, avoid excessive alcohol consumption, and drive defensively. Not only will you feel better physically right now, but these behaviors can also save you on medical bills down the road.\n\n Since the 2020 COVID-19 pandemic exposed critical gaps in U.S. health care and health insurance, the U.S government has been leveraging the American Rescue Plan (ARP), a \$1.9-trillion stimulus package signed into law in March 2021, to expand health care coverage and reduce costs. The plan also includes incentives for states that have not participated in the Affordable Care Act (ACA) expansion to do so, potentially extending healthcare coverage to over 3 million uninsured people.\n4\nProtect Your Wealth\nTo ensure that your hard-earned money doesn’t vanish in an emergency, you should take steps right now to protect it. Below are some smart moves to think about, even if you can’t afford them all right away.\n\nIf you rent, get renter's insurance to protect the contents of your home from loss due to burglary or fire. Read the policy carefully to see what’s covered and what isn’t.\n\nDisability insurance protects your greatest financial asset—the ability to earn an income—by providing you with a steady income if you ever become unable to work for an extended period of time due to illness or injury.\n\nIf you want help managing your money, find a fee-only financial planner to provide unbiased advice. Unlike a commission-based financial advisor, who earns money when you sign up with the investments that their company backs, a fee-only planner has no personal incentive to give you financial advice that might not be in your best interest. (Even if a commission-based advisor gives you solid advice, they still always have a divided loyalty—to their company’s bottom line and to you.)\n\nYou should also protect your money from taxes, which is easy to do with a retirement account, and from inflation, which you can do by making sure that your money is earning interest.\n\nAs you decide how to protect your savings, learn everything you can about relevant investment vehicles, because they all bring both different degrees of risk and different potential for growth. For example, high-interest savings accounts, money market funds, and CDs are relatively free of risk; your money is safe, but it will grow slowly. On the other hand, stocks, bonds, and mutual funds are much riskier; the value of your portfolio could fall, but the potential for growth is much greater as well.\n\nFrequently Asked Questions\nHow Do I Choose a Financial Advisor?\nAn excellent choice for a young adult is a fee-only financial planner. Unlike a commission-based advisor, who earns a commission if they sign you up with their company's investment plans, a fee-only planner has no personal incentive beyond your best interest, so they have no reason not to give you unbiased advice.\n\nWhy Is Compound Interest So Powerful?\nCompound interest is one of the most powerful forces in finance because it grows your money exponentially, which means it can super-charge your savings, especially over time. The magic of compound interest for your retirement account is that it is interest on interest—literally. You earn interest not only on the principal (the money you put in), but also on the interest (the money the bank pays you for holding your principal).\n\nWhy Did My Paycheck Shrink After My Raise?\nThe higher your salary, the higher your tax rate. If you just got a raise or took a new job at a higher salary, the change in the marginal tax rate on the additional income will definitely affect your paycheck. For example, if a salary increase of \$6,000 per year bumps you up into a higher tax bracket, the percentage of your income that goes to taxes bumps up as well—which will make your paycheck smaller than expected. If you’re considering a move to a more expensive area to accept a higher salary, keep that in mind.\n\nThe Bottom Line\nRemember, you don’t need an MBA in finance or even specialized training to become an expert at managing your finances. Following these eight basic rules can put you on the path to financial security, which is the foundation that will allow you to build the rest of your dreams.",

        "How to achieve financial stability in changing times"
                to "There are changes that cause and continue to bring difficulties to Filipinos. During these trying times, stability is far-fetched. It would take determination, focus, and the right strategies to thrive and survive.\n\nPeople who deal with difficulties because of the unexpected turn of events recently should not lose hope. Having a stable life and income is still achievable even in changing times.\n\nChanges that may impact life and income stability among Filipinos\nInflation rate\nThe country's inflation rate continues to rise. In May, it reached 5.4%--the highest level since December 2021. When the inflation rate is high, the cost of living also increases. This is due to the high cost of commodities and the decline in the purchasing power of the Philippine peso. In addition to that, gas prices are at record-high levels. Increases in the prices of electricity, food, and other basic products have also been seen in the past months.\n\nPost-pandemic transition\nThe pandemic is not yet over. Although post-pandemic preparations are being done, the threat of the Coronavirus remains. In addition to that are emerging diseases, such as monkeypox and other Omicron subvariants, that also challenge the health sector. As health continues to be a concern, security and stability among Filipinos will still be uncertain.\n\nNatural calamities\nThe Philippines deals with several typhoons every year. With the worsening effects of climate change and the rainy season, extreme weather disturbances can be expected. Likewise, earthquakes and volcanic eruptions happen in various parts of the country which further turns the lives of Filipinos upside down. Properties may vanish in a snap due to flash floods and lives may be lost without warning.\n\nLife and income stability is an uphill battle. However, with consistency and the right amount of effort and perseverance, it can be achieved. Read on to learn short-term and long-term tips on how it can be achieved.\n\nTips to achieve financial stability in changing times\nShort-term strategies\n\nStay on a budget.\nSet a realistic budget and stick to it. Expenses should be based on monthly income. One should spend within means to avoid debts. Identify expenses and prioritize the essentials to ensure that even on a budget, the basic needs and other necessary expenses are not compromised.\n\nReduce expenses.\nAfter the budget and necessities have been identified, check if your income is sufficient for all the essentials. If not, cut the expenses starting from those that will have the least impact even when gone. Modifications to the usual choices should also be done to make ends meet and still have some left for unexpected expenses. For instance, if the Internet subscription is high, shop around. Consider other providers that offer the best prices.\n\nCreate additional sources of income.\nThe usual income earned by an individual may no longer be sufficient now that the inflation rate is high and the purchasing power is low. It may be a struggle to make ends meet and live a quality life. Creating additional sources of income will help a person cope with a higher cost of living.\n\nLong-term strategies\n\nUpgrade your knowledge.\nAn increase in knowledge and skills can help a person earn more money in the long run. Continuing education may require time, effort, and dedication but it may bring lifelong benefits. It may be a factor why a person will be promoted in the future or be given a higher salary. Learning new skills will also open opportunities for new and better employment.\n\nInvest.\nSome may doubt the idea of investing when the inflation rate is high. When in fact, the right investments can help outrun the impact of inflation. Grab opportunities from affordable investment plans to keep funds growing. Diversify and seek other investment opportunities that can bring huge rewards in the long run.\n\nGet insured.\nHealth and life insurance plans should not be treated as unnecessary expenses, especially with the benefits that they guarantee. Life-threatening diseases and critical illnesses require immediate treatment. With the high cost of medicines and hospitalization, it would be a smart move to ensure that funds will be available as soon as the need for them arises. Health and life insurance plans can prepare a person for life’s surprises. It protects not only the insured but his/her family as well.\n\nCheck out the wide range of life insurance and health protection plans, here.\n\nPlan for retirement.\nIt is never too early to think about retirement now. No matter what the age is, retirement planning is a must especially if the goal is to achieve financial security.  Look into the future and consider what it will take to be financially free when retirement day comes. Retirement planning will help build the fund that will be needed to have peace of mind and comfortable life even during the golden years.\n\nSOURCES: csis.org chartercollege.edu rappler.com thebalance.com"
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

    fun getChallenges(): Map<Int, Challenge> {
        return challenges
    }

    fun getChallengesAmounts(): Map<Int, List<Int>> {
        return challengesAmounts
    }

    fun getPaymentTypes(): List<String> {
        return paymentTypes
    }

    fun getSubscriptionNotifications(): List<String> {
        return subscriptionNotifications
    }

    fun getTipsArticles(): List<Tip> {
        return tipsArticles
    }

    fun getTipsVideos(): List<Tip> {
        return tipsVideos
    }

    fun getTipsContent(): Map<String, String> {
        return tipsContent
    }

}