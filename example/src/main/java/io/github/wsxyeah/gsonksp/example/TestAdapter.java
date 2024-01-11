package io.github.wsxyeah.gsonksp.example;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class TestAdapter extends TypeAdapter<User> {
    @Override
    public void write(JsonWriter out, User user) throws IOException {

    }

    @Override
    public User read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        User out = new User();
        in.beginObject();
        in.endObject();
        return out;
    }
}
