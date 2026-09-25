package gruvexp.tribes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.UUID;

public class JsonData {

    private final int kromerPool;
    private final boolean friendlyFire;
    private final HashMap<UUID, Tribe> tribes;

    @JsonCreator
    public JsonData(@JsonProperty("kromerPool") int kromerPool, @JsonProperty("friendlyFire") boolean friendlyFire, @JsonProperty("tribes") HashMap<UUID, Tribe> tribes) {
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

    public HashMap<UUID, Tribe> getTribes() {
        return tribes;
    }

}
