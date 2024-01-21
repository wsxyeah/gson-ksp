package io.github.wsxyeah.gsonksp.example;

import io.github.wsxyeah.gsonksp.annotation.GenerateGsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

@GenerateGsonAdapter
public class User {
    @SerializedName("some_string")
    String someString;

    @SerializedName("some_int")
    int someInt;

    @SerializedName("some_long")
    long someLong;

    @SerializedName("some_short")
    long someShort;

    @SerializedName("some_byte")
    long someByte;

    @SerializedName("some_float")
    float someFloat;

    @SerializedName("some_double")
    double someDouble;

    @SerializedName("some_boolean")
    boolean someBoolean;

    @SerializedName("some_integer")
    Integer someInteger;

    @SerializedName("some_integer_list")
    List<Integer> someIntegerList;

    @SerializedName("some_map")
    Map<String, String> someMap;

    @SerializedName("nested_map")
    Map<String, Map<String, String>> nestedMap;

}
