package com.example.shop.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "products")
public class ProductModel {
    public long getId() {
        return id;
    }

    @Id
    private long id;
    private String title;
    private String description;
    private String mainImagePath;
    private int price;

    public String getTitle() {
        return title;
    }

    public int getPrice() {
        return price;
    }

    public ProductModel(long id, String title, String description, String mainImagePath, int price) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.mainImagePath = mainImagePath;
        this.price = price;
    }

    public ProductModel() {

    }


    public long id() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String mainImagePath() {
        return mainImagePath;
    }

    public void setMainImagePath(String mainImagePath) {
        this.mainImagePath = mainImagePath;
    }

    public int price() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
