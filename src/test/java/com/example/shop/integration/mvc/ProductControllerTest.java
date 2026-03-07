package com.example.shop.integration.mvc;

import com.example.shop.BaseTestContainerTest;
import com.example.shop.dto.PagingDto;
import com.example.shop.dto.ProductDto;
import com.example.shop.integration.utils.TestDataManager;
import com.example.shop.model.ProductModel;
import com.example.shop.model.SortModel;
import com.example.shop.utils.ViewNames;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest()
@Transactional
@AutoConfigureMockMvc
public class ProductControllerTest extends BaseTestContainerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataManager testEntityManager;

    @Autowired
    private EntityManager entityManager;


    @Test
    void givenNoItems_whenGetProducts_thenEmpty() throws Exception {
        mockMvc.perform(get("/items")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.PRODUCTS))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attribute("items", hasSize(0)));
    }

    @Test
    void givenOneItem_whenGetProducts_thenOneItem() throws Exception {
        testEntityManager.insertProduct("Product 2", null, null);
        testEntityManager.insertProduct("Product 1", null, null);

        mockMvc.perform(get("/items")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.PRODUCTS))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attribute("items", hasSize(1)))
                .andExpect(model().attribute("items", everyItem(not(empty()))))
                .andDo(result -> {
                    // Cast model attribute
                    final var items =
                            (List<List<ProductDto>>) result.getModelAndView().getModel().get("items");

                    assertEquals(1, items.size());

                    final var inner = items.get(0);
                    assertEquals(2, inner.size());

                    assertEquals("Product 2", inner.get(0).title());
                    assertEquals("Product 1", inner.get(1).title());
                })
                .andExpect(model().attribute("search", nullValue()))
                .andExpect(model().attribute("sort", equalTo(SortModel.NO)))
                .andDo(result -> {
                    PagingDto paging = (PagingDto) result.getModelAndView().getModel().get("paging");

                    assertEquals(5, paging.pageSize());
                    assertEquals(1, paging.pageNumber());
                    assertFalse(paging.hasPrevious());
                    assertFalse(paging.hasNext());
                });
    }

    @Test
    void givenAlphaSort_whenGetProducts_thenOrderByTitle() throws Exception {
        testEntityManager.insertProduct("Product 2", null, null);
        testEntityManager.insertProduct("Product 1", null, null);

        mockMvc.perform(get("/items")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("sort", "ALPHA"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.PRODUCTS))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attribute("items", hasSize(1)))
                .andDo(result -> {
                    // Cast model attribute
                    final var items =
                            (List<List<ProductDto>>) result.getModelAndView().getModel().get("items");

                    assertEquals(1, items.size());

                    final var inner = items.get(0);
                    assertEquals(2, inner.size());

                    assertEquals("Product 1", inner.get(0).title());
                    assertEquals("Product 2", inner.get(1).title());
                });
    }

    @Test
    void givenPriceSort_whenGetProducts_thenOrderByPrice() throws Exception {
        testEntityManager.insertProduct("Product 2", null, 1000);
        testEntityManager.insertProduct("Product 1", null, 200);

        mockMvc.perform(get("/items")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("sort", "PRICE"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.PRODUCTS))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attribute("items", hasSize(1)))
                .andDo(result -> {
                    // Cast model attribute
                    final var items =
                            (List<List<ProductDto>>) result.getModelAndView().getModel().get("items");

                    assertEquals(1, items.size());

                    final var inner = items.get(0);
                    assertEquals(2, inner.size());

                    assertEquals("Product 1", inner.get(0).title());
                    assertEquals("Product 2", inner.get(1).title());
                });
    }

    @Test
    void givenSearch_whenGetProducts_thenFilterByTitleOrDesc() throws Exception {
        testEntityManager.insertProduct("Product 2", null, 1000);
        testEntityManager.insertProduct("Product 1", null, 200);
        testEntityManager.insertProduct("Product 3", "2", 200);

        mockMvc.perform(get("/items")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("search", "2")
                        .param("sort", "PRICE"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.PRODUCTS))
                .andExpect(model().attributeExists("items"))
                .andDo(result -> {
                    final var items =
                            (List<List<ProductDto>>) result.getModelAndView().getModel().get("items");

                    assertEquals(1, items.size());

                    final var inner = items.get(0);
                    assertEquals(2, inner.size());

                    assertEquals("Product 3", inner.get(0).title());
                    assertEquals("Product 2", inner.get(1).title());
                });
    }

    @Test
    void givenExistingId_whenGetProduct_thenReturnProductDto() throws Exception {
        final var model = testEntityManager.insertProduct("Product 2", "desc test", 1000);


        mockMvc.perform(get("/items/" + model.id()))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andDo(result -> {
                    final var item = (ProductDto) result.getModelAndView().getModel().get("item");

                    assertEquals("Product 2", item.title());
                    assertEquals("desc test", item.description());
                    assertEquals(1000, item.price());
                    assertEquals("path", item.imgPath());
                });
    }

    @Test
    void givenUnknownId_whenGetProduct_thenReturn404() throws Exception {
        mockMvc.perform(get("/items/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenIdAndPlus_whenChangeCartStateFromItems_thenRedirectIncrement() throws Exception {
        final var model = testEntityManager.insertProduct("Product 2", "desc test", 1000);

        mockMvc.perform(
                        post("/items")
                                .param("id", String.valueOf(model.id()))
                                .param("action", "PLUS")
                                .param("search", "test")
                                .param("sort", "PRICE")
                                .param("pageNumber", "1")
                                .param("pageSize", "5")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("items?search=test&sort=PRICE&pageSize=5&pageNumber=1"));

        entityManager.flush();
        entityManager.clear();

        ProductModel product = entityManager.find(ProductModel.class, model.id());
        assertEquals(1, product.getProductsInCartModel().getCount());
    }

    @Test
    void givenIdAndRemove_whenChangeCartStateFromItems_thenRedirectWithDecrement() throws Exception {
        final var model = testEntityManager.insertProduct("Product 2", "desc test", 1000);

        mockMvc.perform(post("/items")
                .param("id", String.valueOf(model.id()))
                .param("action", "MINUS")
        );

        mockMvc.perform(post("/items")
                        .param("id", String.valueOf(model.id()))
                        .param("action", "MINUS")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("items?sort=NO&pageSize=5&pageNumber=1"));

        entityManager.flush();
        entityManager.clear();

        ProductModel product = entityManager.find(ProductModel.class, model.id());
        assertEquals(null, product.getProductsInCartModel());
    }

    @Test
    void givenIdAndPlus_whenChangeCartStateFromItem_thenRedirectIncrement() throws Exception {
        final var model = testEntityManager.insertProduct("Product 2", "desc test", 1000);

        mockMvc.perform(
                        post("/items/" + model.id())
                                .param("action", "PLUS"))
                .andExpect(status().isOk())
                .andExpect(view().name(ViewNames.PRODUCT));

        entityManager.flush();
        entityManager.clear();

        ProductModel product = entityManager.find(ProductModel.class, model.id());
        assertEquals(1, product.getProductsInCartModel().getCount());
    }
}
