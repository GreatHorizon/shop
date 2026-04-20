package com.example.main.unit.service;

import com.example.main.model.CartActionModel;
import com.example.main.model.ProductModel;
import com.example.main.model.ProductsInCartModel;
import com.example.main.model.UserModel;
import com.example.main.repository.CartRepository;
import com.example.main.repository.ProductRepository;
import com.example.main.repository.UserRepository;
import com.example.main.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    @Mock
    private UserRepository userRepository;

    private final UserModel user = new UserModel(1L, "username");


    @Test
    void getCartItems_shouldReturnCartProductsDtoWithProductsAndTotal() {
        ProductModel product1 = new ProductModel();
        product1.setId(1L);
        product1.setTitle("Product 1");
        product1.setPrice(100);

        ProductModel product2 = new ProductModel();
        product2.setId(2L);
        product2.setTitle("Product 2");
        product2.setPrice(200);

        ProductsInCartModel cartItem1 = new ProductsInCartModel();
        cartItem1.setId(1L);
        cartItem1.setProductId(1L);
        cartItem1.setCount(2);
        cartItem1.setUserId(user.getId());

        ProductsInCartModel cartItem2 = new ProductsInCartModel();
        cartItem2.setId(2L);
        cartItem2.setProductId(2L);
        cartItem2.setCount(1);
        cartItem2.setUserId(user.getId());

        when(cartRepository.findAllByUserId(user.getId()))
                .thenReturn(Flux.just(cartItem1, cartItem2));

        when(productRepository.getProductModelById(1L))
                .thenReturn(Mono.just(product1));

        when(productRepository.getProductModelById(2L))
                .thenReturn(Mono.just(product2));

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Mono.just(user));

        StepVerifier.create(cartService.getCartItems(user.getUsername()))
                .assertNext(result -> {
                    assertEquals(2, result.items().size());
                    assertEquals(400, result.total());
                })
                .verifyComplete();

        verify(cartRepository).findAllByUserId(user.getId());
        verify(productRepository).getProductModelById(1L);
        verify(productRepository).getProductModelById(2L);
    }

    @Test
    void getCartItems_shouldReturnEmptyCartWhenNoItems() {
        when(cartRepository.findAllByUserId(user.getId()))
                .thenReturn(Flux.empty());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Mono.just(user));


        StepVerifier.create(cartService.getCartItems(user.getUsername()))
                .assertNext(result -> {
                    assertEquals(0, result.items().size());
                    assertEquals(0, result.total());
                })
                .verifyComplete();

        verify(cartRepository).findAllByUserId(user.getId());
        verify(productRepository, never()).findAllById(anyIterable());
    }

    @Test
    void updateCartStateForProduct_shouldAddProductWhenActionIsPlus() {
        Long productId = 1L;

        ProductModel product = new ProductModel();
        product.setId(productId);
        product.setTitle("Product 1");
        product.setPrice(100);

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Mono.just(user));

        when(cartRepository.findByProductIdAndUserId(productId, user.getId()))
                .thenReturn(Mono.empty());

        when(productRepository.findProductModelById(productId))
                .thenReturn(Mono.just(product));

        when(cartRepository.save(any(ProductsInCartModel.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(cartService.updateCartStateForProduct(productId, CartActionModel.PLUS, user.getUsername()))
                .verifyComplete();

        verify(cartRepository).findByProductIdAndUserId(productId, user.getId());
        verify(productRepository).findProductModelById(productId);
        verify(cartRepository).save(any(ProductsInCartModel.class));
    }

    @Test
    void updateCartStateForProduct_shouldIncreaseCountWhenActionIsPlusAndItemExists() {
        Long productId = 1L;

        ProductModel product = new ProductModel();
        product.setId(productId);
        product.setTitle("Product 1");
        product.setPrice(100);

        ProductsInCartModel cartItem = new ProductsInCartModel();
        cartItem.setId(10L);
        cartItem.setProductId(productId);
        cartItem.setCount(2);

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Mono.just(user));

        when(productRepository.findProductModelById(productId))
                .thenReturn(Mono.just(product));

        when(cartRepository.findByProductIdAndUserId(productId, user.getId()))
                .thenReturn(Mono.just(cartItem));

        when(cartRepository.save(cartItem))
                .thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.updateCartStateForProduct(productId, CartActionModel.PLUS, user.getUsername()))
                .verifyComplete();

        assertEquals(3, cartItem.getCount());
        verify(productRepository).findProductModelById(productId);
        verify(cartRepository).findByProductIdAndUserId(productId, user.getId());
        verify(cartRepository).save(cartItem);
    }

    @Test
    void updateCartStateForProduct_shouldDecreaseCountWhenActionIsMinusAndCountMoreThanOne() {
        Long productId = 1L;

        ProductsInCartModel cartItem = new ProductsInCartModel();
        cartItem.setId(10L);
        cartItem.setProductId(productId);
        cartItem.setCount(3);

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Mono.just(user));

        when(cartRepository.findByProductIdAndUserId(productId, user.getId()))
                .thenReturn(Mono.just(cartItem));

        when(cartRepository.save(cartItem))
                .thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.updateCartStateForProduct(productId, CartActionModel.MINUS, user.getUsername()))
                .verifyComplete();

        assertEquals(2, cartItem.getCount());
        verify(cartRepository).findByProductIdAndUserId(productId, user.getId());
        verify(cartRepository).save(cartItem);
        verify(cartRepository, never()).delete(any());
    }

    @Test
    void updateCartStateForProduct_shouldDeleteItemWhenActionIsMinusAndCountIsOne() {
        Long productId = 1L;

        ProductsInCartModel cartItem = new ProductsInCartModel();
        cartItem.setId(10L);
        cartItem.setProductId(productId);
        cartItem.setCount(1);

        when(cartRepository.findByProductIdAndUserId(productId, user.getId()))
                .thenReturn(Mono.just(cartItem));

        when(cartRepository.delete(cartItem))
                .thenReturn(Mono.empty());

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Mono.just(user));

        StepVerifier.create(cartService.updateCartStateForProduct(productId, CartActionModel.MINUS, user.getUsername()))
                .verifyComplete();

        verify(cartRepository).findByProductIdAndUserId(productId, user.getId());
        verify(cartRepository).delete(cartItem);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartStateForProduct_shouldDeleteItemWhenActionIsDelete() {
        Long productId = 1L;

        ProductsInCartModel cartItem = new ProductsInCartModel();
        cartItem.setId(10L);
        cartItem.setProductId(productId);
        cartItem.setCount(5);

        when(cartRepository.findByProductIdAndUserId(productId, user.getId()))
                .thenReturn(Mono.just(cartItem));

        when(cartRepository.delete(cartItem))
                .thenReturn(Mono.empty());

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Mono.just(user));


        StepVerifier.create(cartService.updateCartStateForProduct(productId, CartActionModel.DELETE, user.getUsername()))
                .verifyComplete();

        verify(cartRepository).findByProductIdAndUserId(productId, user.getId());
        verify(cartRepository).delete(cartItem);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void cleanCart_shouldDeleteAllItems() {
        when(cartRepository.deleteAll())
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.cleanCart(user.getUsername()))
                .verifyComplete();

        verify(cartRepository).deleteAll();
    }
}