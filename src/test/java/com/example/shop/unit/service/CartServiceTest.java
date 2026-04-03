package com.example.shop.unit.service;

import com.example.shop.model.CartActionModel;
import com.example.shop.model.ProductModel;
import com.example.shop.model.ProductsInCartModel;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.service.CartService;
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

        ProductsInCartModel cartItem2 = new ProductsInCartModel();
        cartItem2.setId(2L);
        cartItem2.setProductId(2L);
        cartItem2.setCount(1);

        when(cartRepository.findAllByOrderById())
                .thenReturn(Flux.just(cartItem1, cartItem2));

        when(productRepository.getProductModelById(1L))
                .thenReturn(Mono.just(product1));

        when(productRepository.getProductModelById(2L))
                .thenReturn(Mono.just(product2));

        StepVerifier.create(cartService.getCartItems())
                .assertNext(result -> {
                    assertEquals(2, result.items().size());
                    assertEquals(400, result.total());
                })
                .verifyComplete();

        verify(cartRepository).findAllByOrderById();
        verify(productRepository).getProductModelById(1L);
        verify(productRepository).getProductModelById(2L);
    }

    @Test
    void getCartItems_shouldReturnEmptyCartWhenNoItems() {
        when(cartRepository.findAllByOrderById())
                .thenReturn(Flux.empty());

        StepVerifier.create(cartService.getCartItems())
                .assertNext(result -> {
                    assertEquals(0, result.items().size());
                    assertEquals(0, result.total());
                })
                .verifyComplete();

        verify(cartRepository).findAllByOrderById();
        verify(productRepository, never()).findAllById(anyIterable());
    }

    @Test
    void updateCartStateForProduct_shouldAddProductWhenActionIsPlus() {
        Long productId = 1L;

        ProductModel product = new ProductModel();
        product.setId(productId);
        product.setTitle("Product 1");
        product.setPrice(100);

        when(cartRepository.findByProductId(productId))
                .thenReturn(Mono.empty());

        when(productRepository.findProductModelById(productId))
                .thenReturn(Mono.just(product));

        when(cartRepository.save(any(ProductsInCartModel.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(cartService.updateCartStateForProduct(productId, CartActionModel.PLUS))
                .verifyComplete();

        verify(cartRepository).findByProductId(productId);
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

        when(productRepository.findProductModelById(productId))
                .thenReturn(Mono.just(product));

        when(cartRepository.findByProductId(productId))
                .thenReturn(Mono.just(cartItem));

        when(cartRepository.save(cartItem))
                .thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.updateCartStateForProduct(productId, CartActionModel.PLUS))
                .verifyComplete();

        assertEquals(3, cartItem.getCount());
        verify(productRepository).findProductModelById(productId);
        verify(cartRepository).findByProductId(productId);
        verify(cartRepository).save(cartItem);
    }

    @Test
    void updateCartStateForProduct_shouldDecreaseCountWhenActionIsMinusAndCountMoreThanOne() {
        Long productId = 1L;

        ProductsInCartModel cartItem = new ProductsInCartModel();
        cartItem.setId(10L);
        cartItem.setProductId(productId);
        cartItem.setCount(3);

        when(cartRepository.findByProductId(productId))
                .thenReturn(Mono.just(cartItem));

        when(cartRepository.save(cartItem))
                .thenReturn(Mono.just(cartItem));

        StepVerifier.create(cartService.updateCartStateForProduct(productId, CartActionModel.MINUS))
                .verifyComplete();

        assertEquals(2, cartItem.getCount());
        verify(cartRepository).findByProductId(productId);
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

        when(cartRepository.findByProductId(productId))
                .thenReturn(Mono.just(cartItem));

        when(cartRepository.delete(cartItem))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.updateCartStateForProduct(productId, CartActionModel.MINUS))
                .verifyComplete();

        verify(cartRepository).findByProductId(productId);
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

        when(cartRepository.findByProductId(productId))
                .thenReturn(Mono.just(cartItem));

        when(cartRepository.delete(cartItem))
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.updateCartStateForProduct(productId, CartActionModel.DELETE))
                .verifyComplete();

        verify(cartRepository).findByProductId(productId);
        verify(cartRepository).delete(cartItem);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void cleanCart_shouldDeleteAllItems() {
        when(cartRepository.deleteAll())
                .thenReturn(Mono.empty());

        StepVerifier.create(cartService.cleanCart())
                .verifyComplete();

        verify(cartRepository).deleteAll();
    }
}