package com.example.shop.service;


import com.example.shop.dto.CartProductsDto;
import com.example.shop.entity.ProductInCartEntity;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.model.CartActionModel;
import com.example.shop.model.ProductsInCartModel;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository, CartRepository repository) {
        this.cartRepository = repository;
        this.productRepository = productRepository;
    }

    public Mono<CartProductsDto> getCartItems() {
        return cartRepository.findAllByOrderById()
                .flatMap((productsInCartModel ->
                                productRepository.getProductModelById(productsInCartModel.getProductId())
                                        .map((productModel ->
                                                        new ProductInCartEntity(
                                                                productsInCartModel.getId(),
                                                                productsInCartModel.getCount(),
                                                                productModel
                                                        )
                                                )
                                        )
                        )
                ).collectList().map((list) -> {
                    final var total = list.stream()
                            .map(ProductInCartEntity::getTotalPrice)
                            .reduce(Integer::sum)
                            .orElse(0);

                    final var products = list.stream()
                            .map((item) -> ProductMapper.toDto(item.getProduct(), item.getCount()))
                            .toList();

                    return new CartProductsDto(products, total);
                });
    }


    @Transactional
    public Mono<Void> updateCartStateForProduct(Long productId, CartActionModel action) {
        if (action.isPlus()) {
            return addProductToCart(productId);
        } else if (action.isMinus()) {
            return removeProductFromCart(productId);
        } else if (action.isDelete()) {
            return deleteProductFromCart(productId);
        }

        return Mono.error(new RuntimeException("Unknown action"));
    }

    @Transactional
    public Mono<Void> cleanCart() {
        return cartRepository.deleteAll();
    }


    private Mono<Void> addProductToCart(Long productId) {
        return cartRepository.findByProductId(productId)
                .flatMap(model -> {
                    model.setCount(model.getCount() + 1);

                    return cartRepository.save(model);
                })
                .switchIfEmpty(
                        productRepository.findProductModelById(productId)
                                .map(product -> new ProductsInCartModel(1, product.id()))
                                .flatMap(cartRepository::save)
                )
                .then();
    }


    private Mono<Void> removeProductFromCart(Long productId) {
        return cartRepository
                .findByProductId(productId)
                .switchIfEmpty(Mono.error(new RuntimeException("Product not in cart")))
                .flatMap((model) -> {
                    if (model.getCount() == 1) {
                        return cartRepository.delete(model);
                    } else {
                        model.setCount(model.getCount() - 1);

                        return cartRepository.save(model);
                    }
                })
                .then();
    }

    private Mono<Void> deleteProductFromCart(Long productId) {
        return cartRepository
                .findByProductId(productId)
                .switchIfEmpty(Mono.error(new RuntimeException("Product not in cart")))
                .flatMap(cartRepository::delete)
                .then();
    }
}
