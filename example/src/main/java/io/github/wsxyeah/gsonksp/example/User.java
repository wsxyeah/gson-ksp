package io.github.wsxyeah.gsonksp.example;

import io.github.wsxyeah.gsonksp.annotation.GenerateGsonAdapter;

@GenerateGsonAdapter
public class User {
    private String name;
    private String email;
    private int repoCount;
}
