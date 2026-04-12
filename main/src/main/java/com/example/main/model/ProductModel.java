package com.example.main.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "products")
public class ProductModel {

    @Id
    private long id;
    private String title;
    private String description;
    private String mainImagePath;
    private int price;

    public ProductModel(long id, String title, String description, String mainImagePath, int price) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.mainImagePath = mainImagePath;
        this.price = price;
    }

    public ProductModel(String title, String description, String mainImagePath, int price) {
        this.title = title;
        this.description = description;
        this.mainImagePath = mainImagePath;
        this.price = price;
    }

    public ProductModel() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMainImagePath() {
        return mainImagePath;
    }

    public void setMainImagePath(String mainImagePath) {
        this.mainImagePath = mainImagePath;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
