package com.example.shop.service;


import com.example.shop.dto.CartProductsDto;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.model.CartActionModel;
import com.example.shop.model.ProductModel;
import com.example.shop.model.ProductsInCartModel;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository repository, ProductRepository productRepository) {
        this.cartRepository = repository;
        this.productRepository = productRepository;
    }

    public CartProductsDto getCartItems() {
        final var productsInCart = cartRepository.findAllByOrderById();

        final var total = productsInCart.stream()
                .map(ProductsInCartModel::getTotalPrice)
                .reduce(Integer::sum)
                .orElse(0);

        final var products = productsInCart.stream()
                .map((item) -> ProductMapper.toDto(item.getProduct()))
                .toList();

        return new CartProductsDto(products, total);
    }


    @Transactional
    public void updateCartStateForProduct(Long productId, CartActionModel action) {
        if (action.isPlus()) {
            addProductToCart(productId);
        } else if (action.isMinus()) {
            removeProductFromCart(productId);
        } else if (action.isDelete()) {
            deleteProductFromCart(productId);
        }
    }

    @Transactional
    public void cleanCart() {

        cartRepository.deleteAllInBatch();
    }


    private void addProductToCart(Long productId) {
        final var productInCartModel = cartRepository.findByProduct_Id(productId);


        if (productInCartModel.isPresent()) {
            final var updatedModel = productInCartModel.get();

            updatedModel.setCount(updatedModel.getCount() + 1);
        } else {
            ProductModel product = productRepository.findProductModelById(productId);

            final var newModel = new ProductsInCartModel(1, product);

            cartRepository.save(newModel);
        }
    }


    private void removeProductFromCart(Long productId) {
        final var productsInCartModel = cartRepository.findByProduct_Id(productId);

        if (productsInCartModel.isEmpty()) {
            return;
        }

        final var model = productsInCartModel.get();

        if (model.getCount() == 1) {
            ProductModel product = model.getProduct();

            product.setProductInCartReference(null);

            cartRepository.delete(model);
        } else {
            model.setCount(model.getCount() - 1);

            cartRepository.save(model);
        }
    }

    private void deleteProductFromCart(Long productId) {
        final var productsInCartModel = cartRepository.findByProduct_Id(productId);

        if (productsInCartModel.isEmpty()) {
            return;
        }

        final var model = productsInCartModel.get();

        ProductModel product = model.getProduct();

        product.setProductInCartReference(null);

        cartRepository.delete(model);
    }

}
