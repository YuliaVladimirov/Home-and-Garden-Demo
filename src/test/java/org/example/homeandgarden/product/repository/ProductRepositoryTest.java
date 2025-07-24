package org.example.homeandgarden.product.repository;

import org.example.homeandgarden.order.entity.enums.OrderStatus;
import org.example.homeandgarden.product.entity.ProductProjection;
import org.example.homeandgarden.product.entity.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findTopProducts_shouldReturnOrderedProductProjections_whenValidStatusesAndPaging() {

        Pageable pageable = PageRequest.of(0, 10);
        List<OrderStatus> statuses = List.of(OrderStatus.PAID);

        Page<ProductProjection> result = productRepository.findTopProducts(statuses, pageable);

        assertThat(result).isNotEmpty();
        assertNotNull(result);
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());

        assertThat(result.getContent().getFirst().getProductId()).isInstanceOf(UUID.class);
        assertThat(result.getContent().getFirst().getProductName()).isInstanceOf(String.class);
        assertThat(result.getContent().getFirst().getListPrice()).isInstanceOf(BigDecimal.class);
        assertThat(result.getContent().getFirst().getCurrentPrice()).isInstanceOf(BigDecimal.class);
        assertThat(result.getContent().getFirst().getProductStatus()).isInstanceOf(ProductStatus.class);
        assertThat(result.getContent().getFirst().getImageUrl()).isInstanceOf(String.class);
        assertThat(result.getContent().getFirst().getAddedAt()).isInstanceOf(Instant.class);
        assertThat(result.getContent().getFirst().getUpdatedAt()).isInstanceOf(Instant.class);
        assertThat(result.getContent().getFirst().getTotalAmount()).isInstanceOf(Long.class);
    }

    @Test
    void findPendingProducts_shouldReturnOrderedProductProjections_whenValidParametersAndPaging() {

        Pageable pageable = PageRequest.of(0, 10);
        OrderStatus status = OrderStatus.ON_THE_WAY;
        Instant cutoff = Instant.now().minus(10, ChronoUnit.DAYS);

        Page<ProductProjection> result = productRepository.findPendingProducts(status, cutoff, pageable);

        assertThat(result).isNotEmpty();
        assertNotNull(result);
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());

        assertThat(result.getContent().getFirst().getProductId()).isInstanceOf(UUID.class);
        assertThat(result.getContent().getFirst().getProductName()).isInstanceOf(String.class);
        assertThat(result.getContent().getFirst().getListPrice()).isInstanceOf(BigDecimal.class);
        assertThat(result.getContent().getFirst().getCurrentPrice()).isInstanceOf(BigDecimal.class);
        assertThat(result.getContent().getFirst().getProductStatus()).isInstanceOf(ProductStatus.class);
        assertThat(result.getContent().getFirst().getImageUrl()).isInstanceOf(String.class);
        assertThat(result.getContent().getFirst().getAddedAt()).isInstanceOf(Instant.class);
        assertThat(result.getContent().getFirst().getUpdatedAt()).isInstanceOf(Instant.class);
        assertThat(result.getContent().getFirst().getTotalAmount()).isInstanceOf(Long.class);
    }

    @Test
    void findProfitByPeriod_shouldReturnBigDecimal_whenValidParameters() {

        Instant cutoff = Instant.now().minus((5*365L), ChronoUnit.DAYS);
        BigDecimal result = productRepository.findProfitByPeriod(OrderStatus.DELIVERED, cutoff);

        assertNotNull(result);
        assertThat(result).isInstanceOf(BigDecimal.class);
    }
}
