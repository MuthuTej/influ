package np.com.bimalkafle.firebaseauthdemoapp.utils

object IndianLocations {
    val statesAndCities: Map<String, List<String>> = linkedMapOf(
        "Andhra Pradesh" to listOf("Visakhapatnam", "Vijayawada", "Guntur", "Tirupati", "Nellore"),
        "Arunachal Pradesh" to listOf("Itanagar", "Naharlagun", "Pasighat"),
        "Assam" to listOf("Guwahati", "Silchar", "Dibrugarh", "Jorhat"),
        "Bihar" to listOf("Patna", "Gaya", "Bhagalpur", "Muzaffarpur"),
        "Chhattisgarh" to listOf("Raipur", "Bhilai", "Bilaspur", "Durg"),
        "Goa" to listOf("Panaji", "Margao", "Vasco da Gama"),
        "Gujarat" to listOf("Ahmedabad", "Surat", "Vadodara", "Rajkot", "Gandhinagar"),
        "Haryana" to listOf("Gurugram", "Faridabad", "Panipat", "Ambala"),
        "Himachal Pradesh" to listOf("Shimla", "Manali", "Dharamshala"),
        "Jharkhand" to listOf("Ranchi", "Jamshedpur", "Dhanbad", "Bokaro"),
        "Karnataka" to listOf("Bengaluru", "Mysuru", "Mangaluru", "Hubballi"),
        "Kerala" to listOf("Kochi", "Thiruvananthapuram", "Kozhikode", "Thrissur"),
        "Madhya Pradesh" to listOf("Bhopal", "Indore", "Gwalior", "Jabalpur"),
        "Maharashtra" to listOf("Mumbai", "Pune", "Nagpur", "Nashik", "Aurangabad"),
        "Manipur" to listOf("Imphal"),
        "Meghalaya" to listOf("Shillong"),
        "Mizoram" to listOf("Aizawl"),
        "Nagaland" to listOf("Kohima", "Dimapur"),
        "Odisha" to listOf("Bhubaneswar", "Cuttack", "Rourkela"),
        "Punjab" to listOf("Chandigarh", "Ludhiana", "Amritsar", "Jalandhar"),
        "Rajasthan" to listOf("Jaipur", "Jodhpur", "Udaipur", "Kota"),
        "Sikkim" to listOf("Gangtok"),
        "Tamil Nadu" to listOf("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem"),
        "Telangana" to listOf("Hyderabad", "Warangal", "Nizamabad"),
        "Tripura" to listOf("Agartala"),
        "Uttar Pradesh" to listOf("Lucknow", "Kanpur", "Noida", "Ghaziabad", "Varanasi", "Agra"),
        "Uttarakhand" to listOf("Dehradun", "Haridwar", "Rishikesh"),
        "West Bengal" to listOf("Kolkata", "Howrah", "Durgapur", "Siliguri"),
        "Andaman and Nicobar Islands" to listOf("Port Blair"),
        "Chandigarh" to listOf("Chandigarh"),
        "Dadra and Nagar Haveli and Daman and Diu" to listOf("Daman", "Silvassa"),
        "Delhi" to listOf("New Delhi", "Dwarka", "Rohini", "Saket"),
        "Jammu and Kashmir" to listOf("Srinagar", "Jammu"),
        "Ladakh" to listOf("Leh", "Kargil"),
        "Lakshadweep" to listOf("Kavaratti"),
        "Puducherry" to listOf("Puducherry", "Karaikal"),
    )

    val states: List<String> = statesAndCities.keys.toList()

    fun citiesFor(state: String): List<String> = statesAndCities[state] ?: emptyList()
}
