package com.example.shop.unit.service;

import com.example.shop.dto.ProductDto;
import com.example.shop.model.ProductModel;
import com.example.shop.model.SortModel;
import com.example.shop.repository.ProductRepository;
import com.example.shop.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SpringBootTest(
        classes = {
                ProductRepository.class,
                ProductService.class
        }
)
class ProductServiceTest {
    @Autowired
    ProductService productService;

    @MockitoBean
    ProductRepository postRepository;

    @BeforeEach
    void resetAll() {
        Mockito.reset(postRepository);
    }


    @Test
    void getProduct_shouldReturnProductDto_whenProductExists() {
        final var productId = 1L;

        final var productModel = new ProductModel(
                productId,
                "Test Product",
                "Test Description",
                "image.jpg",
                100
        );

        when(postRepository.getProductModelById(productId)).thenReturn(productModel);

        ProductDto result = productService.getProduct(productId);

        assertNotNull(result);
        assertEquals(productId, result.id());
        assertEquals("Test Product", result.title());
        assertEquals("Test Description", result.description());
        assertEquals("image.jpg", result.imgPath());
        assertEquals(100.0, result.price());
        verify(postRepository).getProductModelById(productId);
    }

    @Test
    void getProducts_shouldReturnPartitionedProducts_whenNoSearchAndNoSort() {
        final var productModels = List.of(
                new ProductModel(1L, "Product 1", "Desc 1", "img1.jpg", 10),
                new ProductModel(2L, "Product 2", "Desc 2", "img2.jpg", 20),
                new ProductModel(3L, "Product 3", "Desc 3", "img3.jpg", 30),
                new ProductModel(4L, "Product 4", "Desc 4", "img4.jpg", 40)
        );

        final var productPage = new PageImpl<>(productModels);

        when(postRepository.findAll(any(PageRequest.class))).thenReturn(productPage);

        final var dto = productService.getProducts(null, SortModel.NO, 10, 1);

        assertNotNull(dto);
        assertEquals(2, dto.items().size());
        assertEquals(3, dto.items().get(0).size());
        assertEquals(1, dto.items().get(1).size());

        verify(postRepository).findAll(any(PageRequest.class));
    }

    @Test
    void getProducts_shouldReturnSortedProductsByTitle_whenSortIsAlpha() {
        final var productModels = List.of(
                new ProductModel(1L, "Alpha Product", "Desc 1", "img1.jpg", 10),
                new ProductModel(2L, "Beta Product", "Desc 2", "img2.jpg", 20)
        );
        final var productPage = new PageImpl<>(productModels);

        when(postRepository.findAll(any(PageRequest.class))).thenReturn(productPage);

        final var dto = productService.getProducts(null, SortModel.ALPHA, 10, 1);

        assertNotNull(dto);
        assertFalse(dto.items().isEmpty());
        verify(postRepository).findAll(PageRequest.of(0, 10, Sort.by("title")));
    }

    @Test
    void getProducts_shouldReturnSortedProductsByPrice_whenSortIsPrice() {
        final var productModels = List.of(
                new ProductModel(1L, "Product 1", "Desc 1", "img1.jpg", 10),
                new ProductModel(2L, "Product 2", "Desc 2", "img2.jpg", 20)
        );
        final var productPage = new PageImpl<>(productModels);

        when(postRepository.findAll(any(PageRequest.class))).thenReturn(productPage);

        final var dto = productService.getProducts(null, SortModel.PRICE, 10, 1);

        assertNotNull(dto.items());
        assertFalse(dto.items().isEmpty());

        verify(postRepository).findAll(PageRequest.of(0, 10, Sort.by("price")));
    }

    @Test
    void getProducts_shouldReturnSearchResults_whenSearchIsProvided() {
        final var search = "test";
        final var productModels = List.of(
                new ProductModel(
                        1L,
                        "Test Product",
                        "Test Description",
                        "img1.jpg",
                        10
                )
        );

        final var productPage = new PageImpl<>(productModels);

        when(postRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                anyString(), anyString(), any(PageRequest.class))).thenReturn(productPage);

        final var dto = productService.getProducts(search, SortModel.NO, 10, 1);

        assertNotNull(dto.items());
        assertFalse(dto.items().isEmpty());

        verify(postRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                search,
                search,
                PageRequest.of(0, 10, Sort.unsorted())
        );
    }
}
