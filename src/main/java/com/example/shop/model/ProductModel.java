package com.example.shop.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class ProductModel {

    @OneToOne(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private ProductsInCartModel productsInCartModel;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public ProductModel() {

    }

    public int getCountInCart() {
        if (productsInCartModel == null) return 0;

        return productsInCartModel.getCount();
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

    public void setProductInCartReference(ProductsInCartModel ref) {
        this.productsInCartModel = ref;
    }

    public ProductsInCartModel getProductsInCartModel() {
        return productsInCartModel;
    }
}
