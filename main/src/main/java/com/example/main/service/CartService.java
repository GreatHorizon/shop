package com.example.main.service;


import com.example.main.dto.CartProductsDto;
import com.example.main.entity.ProductInCartEntity;
import com.example.main.mapper.ProductMapper;
import com.example.main.model.CartActionModel;
import com.example.main.model.ProductsInCartModel;
import com.example.main.repository.CartRepository;
import com.example.main.repository.ProductRepository;
import com.example.main.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class CartService {

    final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository, CartRepository repository, UserRepository userRepository) {
        this.cartRepository = repository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Cacheable(value = "cart-items", key = "#username")
    public Mono<CartProductsDto> getCartItems(String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> cartRepository.findAllByUserId(user.getId())
                        .flatMap((productsInCartModel -> {
                                    return productRepository.getProductModelById(productsInCartModel.getProductId())
                                            .map((productModel ->
                                                            new ProductInCartEntity(
                                                                    productsInCartModel.getId(),
                                                                    productsInCartModel.getCount(),
                                                                    productModel
                                                            )
                                                    )
                                            );

                                })
                        ).collectList().map((list) -> {
                            final var total = list.stream()
                                    .map(ProductInCartEntity::getTotalPrice)
                                    .reduce(Integer::sum)
                                    .orElse(0);

                            final var products = list.stream()
                                    .map((item) -> ProductMapper.toDto(item.getProduct(), item.getCount()))
                                    .toList();

                            return new CartProductsDto(products, total);
                        }));
    }

    public Mono<Integer> getCartPrice(String username) {
        return userRepository.findByUsername(username).flatMap(user -> cartRepository.findAllByUserId(user.getId())
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
                ).collectList().map((list) -> list.stream()
                        .map(ProductInCartEntity::getTotalPrice)
                        .reduce(Integer::sum)
                        .orElse(0)
                ));
    }

    @CacheEvict(cacheNames = "cart-items", key = "#username")
    @Transactional
    public Mono<Void> updateCartStateForProduct(Long productId, CartActionModel action, String username) {
        if (action.isPlus()) {
            return addProductToCart(productId, username);
        } else if (action.isMinus()) {
            return removeProductFromCart(productId, username);
        } else if (action.isDelete()) {
            return deleteProductFromCart(productId, username);
        }

        return Mono.error(new RuntimeException("Unknown action"));
    }

    @Transactional
    @CacheEvict(cacheNames = "cart-items", key = "#username")
    public Mono<Void> cleanCart(String username) {
        return cartRepository.deleteAll();
    }


    private Mono<Void> addProductToCart(Long productId, String username) {
        return userRepository.findByUsername(username)
                .flatMap(user ->
                        cartRepository.findByProductIdAndUserId(productId, user.getId())
                                .flatMap(model -> {
                                    model.setCount(model.getCount() + 1);
                                    model.setUserId(user.getId());

                                    return cartRepository.save(model);
                                })
                                .switchIfEmpty(
                                        productRepository.findProductModelById(productId)
                                                .map(product -> new ProductsInCartModel(1, product.getId(), user.getId()))
                                                .flatMap(cartRepository::save)
                                )
                                .then()
                );
    }


    private Mono<Void> removeProductFromCart(Long productId, String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> cartRepository
                        .findByProductIdAndUserId(productId, user.getId())
                        .switchIfEmpty(Mono.error(new RuntimeException("Product not in cart")))
                        .flatMap((model) -> {
                            if (model.getCount() == 1) {
                                return cartRepository.delete(model);
                            } else {
                                model.setCount(model.getCount() - 1);
                                model.setUserId(user.getId());

                                return cartRepository.save(model);
                            }
                        })
                        .then());
    }

    private Mono<Void> deleteProductFromCart(Long productId, String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> cartRepository
                        .findByProductIdAndUserId(productId, user.getId())
                        .switchIfEmpty(Mono.error(new RuntimeException("Product not in cart")))
                        .flatMap(cartRepository::delete)
                        .then());
    }
}
