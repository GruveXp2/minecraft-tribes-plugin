package gruvexp.tribes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

public class JsonData {

    private final int kromerPool;
    private final boolean friendlyFire;
    private final HashMap<String, Tribe> tribes;

    @JsonCreator
    public JsonData(@JsonProperty("kromerPool") int kromerPool, @JsonProperty("friendlyFire") boolean friendlyFire, @JsonProperty("tribes") HashMap<String, Tribe> tribes) {
        this.kromerPool = kromerPool;
        this.friendlyFire = friendlyFire;
        this.tribes = tribes;
    }

    public int getKromerPool() {
        return kromerPool;
    }

    public boolean friendlyFire() {
        return friendlyFire;
    }

    public HashMap<String, Tribe> getTribes() {
        return tribes;
    }

}
