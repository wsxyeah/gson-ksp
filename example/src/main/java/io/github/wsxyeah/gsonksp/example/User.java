package io.github.wsxyeah.gsonksp.example;

import io.github.wsxyeah.gsonksp.annotation.GenerateGsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.wsxyeah.gsonksp.annotation.GsonSetter;

import java.util.List;
import java.util.Map;

@GenerateGsonAdapter
public class User {
    @SerializedName("some_string")
    private String someString;

    @SerializedName("some_int")
    private int someInt;

    @SerializedName("some_long")
    private long someLong;

    @SerializedName("some_short")
    private long someShort;

    @SerializedName("some_byte")
    private long someByte;

    @SerializedName("some_float")
    private float someFloat;

    @SerializedName("some_double")
    private double someDouble;

    @SerializedName("some_boolean")
    private boolean someBoolean;

    @SerializedName("some_integer")
    private Integer someInteger;

    @SerializedName("some_integer_list")
    private List<Integer> someIntegerList;

    @SerializedName("some_map")
    private Map<String, String> someMap;

    @SerializedName("nested_map")
    private Map<String, Map<String, String>> nestedMap;

    @GsonSetter(name = "some_string")
    public void setSomeString(String someString) {
        this.someString = someString;
    }

    @GsonSetter(name = "some_int")
    public void setSomeInt(int someInt) {
        this.someInt = someInt;
    }

    @GsonSetter(name = "some_long")
    public void setSomeLong(long someLong) {
        this.someLong = someLong;
    }

    @GsonSetter(name = "some_short")
    public void setSomeShort(long someShort) {
        this.someShort = someShort;
    }

    @GsonSetter(name = "some_byte")
    public void setSomeByte(long someByte) {
        this.someByte = someByte;
    }

    @GsonSetter(name = "some_float")
    public void setSomeFloat(float someFloat) {
        this.someFloat = someFloat;
    }

    @GsonSetter(name = "some_double")
    public void setSomeDouble(double someDouble) {
        this.someDouble = someDouble;
    }

    @GsonSetter(name = "some_boolean")
    public void setSomeBoolean(boolean someBoolean) {
        this.someBoolean = someBoolean;
    }

    @GsonSetter(name = "some_integer")
    public void setSomeInteger(Integer someInteger) {
        this.someInteger = someInteger;
    }

    @GsonSetter(name = "some_integer_list")
    public void setSomeIntegerList(List<Integer> someIntegerList) {
        this.someIntegerList = someIntegerList;
    }

    @GsonSetter(name = "some_map")
    public void setSomeMap(Map<String, String> someMap) {
        this.someMap = someMap;
    }

    @GsonSetter(name = "nested_map")
    public void setNestedMap(Map<String, Map<String, String>> nestedMap) {
        this.nestedMap = nestedMap;
    }
}
