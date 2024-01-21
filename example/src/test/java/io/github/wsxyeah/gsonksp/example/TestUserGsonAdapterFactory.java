package io.github.wsxyeah.gsonksp.example;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

class TestUserGsonAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() == User.class) {
            return (TypeAdapter<T>) new UserGsonAdapter(gson);
        }
        return null;
    }
}
