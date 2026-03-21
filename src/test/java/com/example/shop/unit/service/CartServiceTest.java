package com.example.shop.unit.service;

import com.example.shop.dto.CartProductsDto;
import com.example.shop.model.CartActionModel;
import com.example.shop.model.ProductModel;
import com.example.shop.model.ProductsInCartModel;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@SpringBootTest(classes = {CartService.class})
class CartServiceTest {

    @MockitoBean
    private CartRepository cartRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @Autowired
    private CartService cartService;

    @BeforeEach
    void setUp() {
        Mockito.reset(productRepository);
        Mockito.reset(cartRepository);    }

    @Test
    void getCartItems_shouldReturnCartProductsDtoWithProductsAndTotal() {
        // Given
        ProductModel product1 = new ProductModel();
        product1.setId(1L);
        product1.setTitle("Product 1");
        product1.setPrice(100);

        ProductModel product2 = new ProductModel();
        product2.setId(2L);
        product2.setTitle("Product 2");
        product2.setPrice(200);

        ProductsInCartModel cartItem1 = new ProductsInCartModel(2, product1);
        ProductsInCartModel cartItem2 = new ProductsInCartModel(1, product2);

        List<ProductsInCartModel> cartItems = List.of(cartItem1, cartItem2);

        when(cartRepository.findAllByOrderById()).thenReturn(cartItems);

        // When
        CartProductsDto result = cartService.getCartItems();

        // Then
        assertNotNull(result);
        assertEquals(2, result.items().size());
        assertEquals(400, result.total());

        verify(cartRepository, times(1)).findAllByOrderById();
    }

    @Test
    void getCartItems_shouldReturnEmptyCartWhenNoItems() {
        // Given
        when(cartRepository.findAllByOrderById()).thenReturn(List.of());

        // When
        CartProductsDto result = cartService.getCartItems();

        // Then
        assertNotNull(result);
        assertTrue(result.items().isEmpty());
        assertEquals(0, result.total());

        verify(cartRepository, times(1)).findAllByOrderById();
    }

    @Test
    void updateCartStateForProduct_shouldAddProductWhenActionIsPlus() {
        // Given
        Long productId = 1L;
        CartActionModel action = CartActionModel.PLUS;

        when(cartRepository.findByProduct_Id(productId)).thenReturn(Optional.empty());

        ProductModel product = new ProductModel();
        product.setId(productId);
        when(productRepository.findProductModelById(productId)).thenReturn(product);

        // When
        cartService.updateCartStateForProduct(productId, action);

        // Then
        verify(cartRepository, times(1)).save(any(ProductsInCartModel.class));
    }

    @Test
    void updateCartStateForProduct_shouldRemoveProductWhenActionIsMinusAndCountIsOne() {
        // Given
        Long productId = 1L;
        CartActionModel action = CartActionModel.MINUS;

        ProductModel product = new ProductModel();
        product.setId(productId);

        ProductsInCartModel cartItem = new ProductsInCartModel(1, product);

        when(cartRepository.findByProduct_Id(productId)).thenReturn(Optional.of(cartItem));

        // When
        cartService.updateCartStateForProduct(productId, action);

        // Then
        verify(cartRepository, times(1)).delete(cartItem);
        assertNull(product.getProductsInCartModel());
    }

    @Test
    void updateCartStateForProduct_shouldDecreaseCountWhenActionIsMinusAndCountMoreThanOne() {
        // Given
        Long productId = 1L;
        CartActionModel action = CartActionModel.MINUS;

        ProductModel product = new ProductModel();
        product.setId(productId);

        ProductsInCartModel cartItem = new ProductsInCartModel(3, product);

        when(cartRepository.findByProduct_Id(productId)).thenReturn(Optional.of(cartItem));

        // When
        cartService.updateCartStateForProduct(productId, action);

        // Then
        assertEquals(2, cartItem.getCount());
        verify(cartRepository, times(1)).save(cartItem);
    }

    @Test
    void updateCartStateForProduct_shouldDeleteProductWhenActionIsDelete() {
        // Given
        Long productId = 1L;
        CartActionModel action = CartActionModel.DELETE;

        ProductModel product = new ProductModel();
        product.setId(productId);

        ProductsInCartModel cartItem = new ProductsInCartModel(2, product);

        when(cartRepository.findByProduct_Id(productId)).thenReturn(Optional.of(cartItem));

        // When
        cartService.updateCartStateForProduct(productId, action);

        // Then
        verify(cartRepository, times(1)).delete(cartItem);
        assertNull(product.getProductsInCartModel());
    }

    @Test
    void cleanCart_shouldDeleteAllItemsInBatch() {
        // When
        cartService.cleanCart();

        // Then
        verify(cartRepository, times(1)).deleteAllInBatch();
    }
}
