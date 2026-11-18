package lt.ktu.donendusted.domain;

public enum FriendshipStatus {
    /** Kvietimas issiustas ir laukia gavejo atsakymo. */
    PENDING,
    /** Draugai: vienas kito viesa veikejo kortele matoma. */
    ACCEPTED,
    /** Viena puse uzblokavo kita - nauju kvietimu tarp ju siusti negalima. */
    BLOCKED
}
