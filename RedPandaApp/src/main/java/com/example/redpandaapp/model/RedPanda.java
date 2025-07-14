package com.example.redpandaapp.model;

import lombok.Data;

/*
 * レッサーパンダ個体一覧モデルクラス
 */

@Data
public class RedPanda {
    private String name;
    private String gender;
    private String birthDate;
    private String deathDate;
    private String age;
    private String movedOutDate;
    private String movedOutZoo;
    private String arrivalDate;
    private String originZoo;
    private String father;
    private String mother;
    private String pair1;
    private String pair2;
    private String pair3;
    private String personality;
    private String feature;
}
