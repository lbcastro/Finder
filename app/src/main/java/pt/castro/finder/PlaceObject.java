package pt.castro.finder;

import java.util.HashMap;

/**
 * Created by lourenco on 11/10/15.
 */
class PlaceObject {
    public PlaceObject(String type, String name, int drawableId) {
        this.type = type;
        this.name = name;
        this.drawableId = drawableId;
    }

    String type;
    String name;
    int drawableId;
    HashMap<String, String> data;

    public HashMap<String, String> getData() {
        return data;
    }

    public void setData(HashMap<String, String> data) {
        this.data = data;
    }
}