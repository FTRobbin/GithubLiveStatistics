package Statistics;

import java.util.HashMap;
import java.util.Observable;

/**
 * Created by RobbinNi on 7/9/16.
 */
public class UpdateEvent extends Observable{

    private static final HashMap<String, UpdateEvent> dict = new HashMap<>();

    public final String name;

    private UpdateEvent(String name) {
        this.name = name;
    }

    public static UpdateEvent newUpdateEvent(String name) {
        UpdateEvent ret = new UpdateEvent(name);
        dict.put(name.intern(), ret);
        return ret;
    }

    public static UpdateEvent findEventName(String name) {
        return dict.get(name.intern());
    }

    public void newEvent() {
        setChanged();
    }
}
