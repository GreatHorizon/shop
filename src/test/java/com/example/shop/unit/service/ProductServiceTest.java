package com.example.shop.unit.service;

import com.example.shop.model.ProductModel;
import com.example.shop.model.ProductsInCartModel;
import com.example.shop.model.SortModel;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ProductService.class)
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CartRepository cartRepository;

    @BeforeEach
    void resetAll() {
        Mockito.reset(productRepository, cartRepository);
    }

    @Test
    void getProduct_shouldReturnProductDto_whenProductExists() {
        long productId = 1L;

        ProductModel productModel = new ProductModel(
                productId,
                "Test Product",
                "Test Description",
                "image.jpg",
                100
        );

        ProductsInCartModel cartItem = new ProductsInCartModel();
        cartItem.setId(10L);
        cartItem.setProductId(productId);
        cartItem.setCount(3);

        when(productRepository.getProductModelById(productId))
                .thenReturn(Mono.just(productModel));
        when(cartRepository.findByProductId(productId))
                .thenReturn(Mono.just(cartItem));

        StepVerifier.create(productService.getProduct(productId))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(productId, result.id());
                    assertEquals("Test Product", result.title());
                    assertEquals("Test Description", result.description());
                    assertEquals("image.jpg", result.imgPath());
                    assertEquals(100, result.price());
                    assertEquals(3, result.count());
                })
                .verifyComplete();

        verify(productRepository).getProductModelById(productId);
        verify(cartRepository).findByProductId(productId);
    }

    @Test
    void getProduct_shouldReturnProductDtoWithZeroCount_whenProductNotInCart() {
        long productId = 1L;

        ProductModel productModel = new ProductModel(
                productId,
                "Test Product",
                "Test Description",
                "image.jpg",
                100
        );

        when(productRepository.getProductModelById(productId))
                .thenReturn(Mono.just(productModel));
        when(cartRepository.findByProductId(productId))
                .thenReturn(Mono.empty());

        StepVerifier.create(productService.getProduct(productId))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(productId, result.id());
                    assertEquals("Test Product", result.title());
                    assertEquals(0, result.count());
                })
                .verifyComplete();

        verify(productRepository).getProductModelById(productId);
        verify(cartRepository).findByProductId(productId);
    }

    @Test
    void getProducts_shouldReturnPartitionedProducts_whenNoSearchAndNoSort() {
        ProductModel product1 = new ProductModel(1L, "Product 1", "Desc 1", "img1.jpg", 10);
        ProductModel product2 = new ProductModel(2L, "Product 2", "Desc 2", "img2.jpg", 20);
        ProductModel product3 = new ProductModel(3L, "Product 3", "Desc 3", "img3.jpg", 30);
        ProductModel product4 = new ProductModel(4L, "Product 4", "Desc 4", "img4.jpg", 40);

        when(productRepository.findAll(Sort.unsorted()))
                .thenReturn(Flux.just(product1, product2, product3, product4));
        when(productRepository.count())
                .thenReturn(Mono.just(4L));

        when(cartRepository.findByProductId(1L)).thenReturn(Mono.empty());
        when(cartRepository.findByProductId(2L)).thenReturn(Mono.empty());
        when(cartRepository.findByProductId(3L)).thenReturn(Mono.empty());
        when(cartRepository.findByProductId(4L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.getProducts(null, SortModel.NO, 10, 1))
                .assertNext(dto -> {
                    assertNotNull(dto);
                    assertEquals(2, dto.items().size());
                    assertEquals(3, dto.items().get(0).size());
                    assertEquals(1, dto.items().get(1).size());

                    assertEquals("Product 1", dto.items().get(0).getFirst().title());
                    assertEquals("Product 4", dto.items().get(1).getFirst().title());

                    assertEquals(10, dto.pagingDto().pageSize());
                    assertEquals(1, dto.pagingDto().pageNumber());
                    assertFalse(dto.pagingDto().hasPrevious());
                    assertFalse(dto.pagingDto().hasNext());
                })
                .verifyComplete();

        verify(productRepository).findAll(Sort.unsorted());
        verify(productRepository).count();
        verify(cartRepository).findByProductId(1L);
        verify(cartRepository).findByProductId(2L);
        verify(cartRepository).findByProductId(3L);
        verify(cartRepository).findByProductId(4L);
    }

    @Test
    void getProducts_shouldReturnSortedProductsByTitle_whenSortIsAlpha() {
        ProductModel product1 = new ProductModel(1L, "Alpha Product", "Desc 1", "img1.jpg", 10);
        ProductModel product2 = new ProductModel(2L, "Beta Product", "Desc 2", "img2.jpg", 20);

        when(productRepository.findAll(Sort.by("title")))
                .thenReturn(Flux.just(product1, product2));
        when(productRepository.count())
                .thenReturn(Mono.just(2L));

        when(cartRepository.findByProductId(1L)).thenReturn(Mono.empty());
        when(cartRepository.findByProductId(2L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.getProducts(null, SortModel.ALPHA, 10, 1))
                .assertNext(dto -> {
                    assertNotNull(dto);
                    assertFalse(dto.items().isEmpty());
                    assertEquals(1, dto.items().size());
                    assertEquals(2, dto.items().getFirst().size());
                    assertEquals("Alpha Product", dto.items().getFirst().get(0).title());
                    assertEquals("Beta Product", dto.items().getFirst().get(1).title());
                })
                .verifyComplete();

        verify(productRepository).findAll(Sort.by("title"));
        verify(productRepository).count();
        verify(cartRepository).findByProductId(1L);
        verify(cartRepository).findByProductId(2L);
    }

    @Test
    void getProducts_shouldReturnSortedProductsByPrice_whenSortIsPrice() {
        ProductModel product1 = new ProductModel(1L, "Product 1", "Desc 1", "img1.jpg", 10);
        ProductModel product2 = new ProductModel(2L, "Product 2", "Desc 2", "img2.jpg", 20);

        when(productRepository.findAll(Sort.by("price")))
                .thenReturn(Flux.just(product1, product2));
        when(productRepository.count())
                .thenReturn(Mono.just(2L));

        when(cartRepository.findByProductId(1L)).thenReturn(Mono.empty());
        when(cartRepository.findByProductId(2L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.getProducts(null, SortModel.PRICE, 10, 1))
                .assertNext(dto -> {
                    assertNotNull(dto);
                    assertFalse(dto.items().isEmpty());
                    assertEquals(1, dto.items().size());
                    assertEquals(2, dto.items().get(0).size());
                })
                .verifyComplete();

        verify(productRepository).findAll(Sort.by("price"));
        verify(productRepository).count();
        verify(cartRepository).findByProductId(1L);
        verify(cartRepository).findByProductId(2L);
    }

    @Test
    void getProducts_shouldReturnSearchResults_whenSearchIsProvided() {
        String search = "test";

        ProductModel product = new ProductModel(
                1L,
                "Test Product",
                "Test Description",
                "img1.jpg",
                10
        );

        when(productRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                search,
                search,
                Sort.unsorted()
        )).thenReturn(Flux.just(product));

        when(productRepository.count())
                .thenReturn(Mono.just(1L));

        when(cartRepository.findByProductId(1L))
                .thenReturn(Mono.empty());

        StepVerifier.create(productService.getProducts(search, SortModel.NO, 10, 1))
                .assertNext(dto -> {
                    assertNotNull(dto);
                    assertFalse(dto.items().isEmpty());
                    assertEquals(1, dto.items().size());
                    assertEquals(1, dto.items().get(0).size());
                    assertEquals("Test Product", dto.items().get(0).get(0).title());
                })
                .verifyComplete();

        verify(productRepository)
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        search,
                        search,
                        Sort.unsorted()
                );
        verify(productRepository).count();
        verify(cartRepository).findByProductId(1L);
    }

    @Test
    void getProducts_shouldReturnOnlyRequestedPage() {
        ProductModel product1 = new ProductModel(1L, "Product 1", "Desc 1", "img1.jpg", 10);
        ProductModel product2 = new ProductModel(2L, "Product 2", "Desc 2", "img2.jpg", 20);
        ProductModel product3 = new ProductModel(3L, "Product 3", "Desc 3", "img3.jpg", 30);
        ProductModel product4 = new ProductModel(4L, "Product 4", "Desc 4", "img4.jpg", 40);
        ProductModel product5 = new ProductModel(5L, "Product 5", "Desc 5", "img5.jpg", 50);

        when(productRepository.findAll(Sort.unsorted()))
                .thenReturn(Flux.just(product1, product2, product3, product4, product5));
        when(productRepository.count())
                .thenReturn(Mono.just(5L));

        when(cartRepository.findByProductId(3L)).thenReturn(Mono.empty());
        when(cartRepository.findByProductId(4L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.getProducts(null, SortModel.NO, 2, 2))
                .assertNext(dto -> {
                    assertNotNull(dto);
                    assertEquals(1, dto.items().size());
                    assertEquals(2, dto.items().get(0).size());

                    assertEquals("Product 3", dto.items().get(0).get(0).title());
                    assertEquals("Product 4", dto.items().get(0).get(1).title());

                    assertTrue(dto.pagingDto().hasPrevious());
                    assertTrue(dto.pagingDto().hasNext());
                    assertEquals(2, dto.pagingDto().pageSize());
                    assertEquals(2, dto.pagingDto().pageNumber());
                })
                .verifyComplete();

        verify(productRepository).findAll(Sort.unsorted());
        verify(productRepository).count();
        verify(cartRepository).findByProductId(3L);
        verify(cartRepository).findByProductId(4L);
    }
}