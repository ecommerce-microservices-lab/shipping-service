package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.OrderStatus;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;
import com.selimhorri.app.repository.OrderItemRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderItemServiceImpl Tests")
class OrderItemServiceImplTest {

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private OrderItemServiceImpl orderItemService;

	private OrderItem orderItem;
	private OrderItemDto orderItemDto;
	private ProductDto productDto;
	private OrderDto orderDto;

	@BeforeEach
	void setUp() {
		// Setup OrderItem
		orderItem = OrderItem.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.isActive(true)
				.build();

		// Setup ProductDto
		productDto = ProductDto.builder()
				.productId(100)
				.productTitle("Test Product")
				.priceUnit(29.99)
				.quantity(10)
				.build();

		// Setup OrderDto
		orderDto = OrderDto.builder()
				.orderId(1)
				.orderStatus(OrderStatus.ORDERED.name())
				.build();

		// Setup OrderItemDto
		orderItemDto = OrderItemDto.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.productDto(productDto)
				.orderDto(orderDto)
				.build();
	}

	// ========== findAll() Tests ==========

	@Test
	@DisplayName("findAll - Should return empty list when no active order items exist")
	void findAll_ShouldReturnEmptyList_WhenNoActiveOrderItems() {
		// Given
		when(orderItemRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

		// When
		List<OrderItemDto> result = orderItemService.findAll();

		// Then
		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(orderItemRepository).findByIsActiveTrue();
	}

	@Test
	@DisplayName("findAll - Should return filtered list with valid products and orders")
	void findAll_ShouldReturnFilteredList_WhenValidProductsAndOrders() {
		// Given
		OrderItem item1 = OrderItem.builder().orderId(1).productId(100).orderedQuantity(5).isActive(true).build();
		OrderItem item2 = OrderItem.builder().orderId(2).productId(200).orderedQuantity(3).isActive(true).build();

		when(orderItemRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(item1, item2));

		ProductDto product1 = ProductDto.builder().productId(100).build();
		ProductDto product2 = ProductDto.builder().productId(200).build();
		OrderDto order1 = OrderDto.builder().orderId(1).orderStatus(OrderStatus.ORDERED.name()).build();
		OrderDto order2 = OrderDto.builder().orderId(2).orderStatus(OrderStatus.ORDERED.name()).build();

		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenReturn(product1);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/200"),
				eq(ProductDto.class))).thenReturn(product2);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(order1);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/2"),
				eq(OrderDto.class))).thenReturn(order2);

		// When
		List<OrderItemDto> result = orderItemService.findAll();

		// Then
		assertNotNull(result);
		assertEquals(2, result.size());
		verify(orderItemRepository).findByIsActiveTrue();
	}

	@Test
	@DisplayName("findAll - Should filter out items when product is not found")
	void findAll_ShouldFilterOutItems_WhenProductNotFound() {
		// Given
		OrderItem item1 = OrderItem.builder().orderId(1).productId(100).orderedQuantity(5).isActive(true).build();

		when(orderItemRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(item1));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenReturn(null);

		// When
		List<OrderItemDto> result = orderItemService.findAll();

		// Then
		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(orderItemRepository).findByIsActiveTrue();
	}

	@Test
	@DisplayName("findAll - Should filter out items when product fetch throws exception")
	void findAll_ShouldFilterOutItems_WhenProductFetchThrowsException() {
		// Given
		OrderItem item1 = OrderItem.builder().orderId(1).productId(100).orderedQuantity(5).isActive(true).build();

		when(orderItemRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(item1));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenThrow(new RestClientException("Product service unavailable"));

		// When
		List<OrderItemDto> result = orderItemService.findAll();

		// Then
		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(orderItemRepository).findByIsActiveTrue();
	}

	@Test
	@DisplayName("findAll - Should filter out items when order status is not ORDERED")
	void findAll_ShouldFilterOutItems_WhenOrderStatusNotOrdered() {
		// Given
		OrderItem item1 = OrderItem.builder().orderId(1).productId(100).orderedQuantity(5).isActive(true).build();

		ProductDto product1 = ProductDto.builder().productId(100).build();
		OrderDto order1 = OrderDto.builder().orderId(1).orderStatus(OrderStatus.CREATED.name()).build();

		when(orderItemRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(item1));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenReturn(product1);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(order1);

		// When
		List<OrderItemDto> result = orderItemService.findAll();

		// Then
		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(orderItemRepository).findByIsActiveTrue();
	}

	// ========== findById() Tests ==========

	@Test
	@DisplayName("findById - Should return OrderItemDto when order item exists and is active")
	void findById_ShouldReturnOrderItemDto_WhenOrderItemExistsAndActive() {
		// Given
		when(orderItemRepository.findById(1)).thenReturn(Optional.of(orderItem));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenReturn(productDto);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(orderDto);

		// When
		OrderItemDto result = orderItemService.findById(1);

		// Then
		assertNotNull(result);
		assertEquals(1, result.getOrderId());
		assertEquals(100, result.getProductId());
		assertEquals(5, result.getOrderedQuantity());
		verify(orderItemRepository).findById(1);
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
	}

	@Test
	@DisplayName("findById - Should throw OrderItemNotFoundException when order item not found")
	void findById_ShouldThrowException_WhenOrderItemNotFound() {
		// Given
		when(orderItemRepository.findById(999)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.findById(999));
		verify(orderItemRepository).findById(999);
		verify(restTemplate, never()).getForObject(anyString(), any(Class.class));
	}

	@Test
	@DisplayName("findById - Should throw OrderItemNotFoundException when order item is inactive")
	void findById_ShouldThrowException_WhenOrderItemInactive() {
		// Given
		OrderItem inactiveItem = OrderItem.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.isActive(false)
				.build();
		when(orderItemRepository.findById(1)).thenReturn(Optional.of(inactiveItem));

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.findById(1));
		verify(orderItemRepository).findById(1);
		verify(restTemplate, never()).getForObject(anyString(), any(Class.class));
	}

	@Test
	@DisplayName("findById - Should throw OrderItemNotFoundException when product fetch fails")
	void findById_ShouldThrowException_WhenProductFetchFails() {
		// Given
		when(orderItemRepository.findById(1)).thenReturn(Optional.of(orderItem));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenThrow(new RestClientException("Product service unavailable"));

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.findById(1));
		verify(orderItemRepository).findById(1);
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class));
	}

	@Test
	@DisplayName("findById - Should throw OrderItemNotFoundException when order status is not ORDERED")
	void findById_ShouldThrowException_WhenOrderStatusNotOrdered() {
		// Given
		OrderDto createdOrder = OrderDto.builder()
				.orderId(1)
				.orderStatus(OrderStatus.CREATED.name())
				.build();

		when(orderItemRepository.findById(1)).thenReturn(Optional.of(orderItem));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenReturn(productDto);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(createdOrder);

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.findById(1));
		verify(orderItemRepository).findById(1);
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
	}

	@Test
	@DisplayName("findById - Should throw OrderItemNotFoundException when order is null")
	void findById_ShouldThrowException_WhenOrderIsNull() {
		// Given
		when(orderItemRepository.findById(1)).thenReturn(Optional.of(orderItem));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenReturn(productDto);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(null);

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.findById(1));
		verify(orderItemRepository).findById(1);
	}

	@Test
	@DisplayName("findById - Should throw OrderItemNotFoundException when order fetch fails")
	void findById_ShouldThrowException_WhenOrderFetchFails() {
		// Given
		when(orderItemRepository.findById(1)).thenReturn(Optional.of(orderItem));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenReturn(productDto);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenThrow(new RestClientException("Order service unavailable"));

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.findById(1));
		verify(orderItemRepository).findById(1);
	}

	// ========== save() Tests ==========

	@Test
	@DisplayName("save - Should save order item successfully when all validations pass")
	void save_ShouldSaveOrderItem_WhenAllValidationsPass() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.build();

		OrderDto createdOrder = OrderDto.builder()
				.orderId(1)
				.orderStatus(OrderStatus.CREATED.name())
				.build();

		ProductDto product = ProductDto.builder()
				.productId(100)
				.quantity(10)
				.build();

		OrderItem savedItem = OrderItem.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.isActive(true)
				.build();

		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(createdOrder);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenReturn(product);
		when(orderItemRepository.save(any(OrderItem.class))).thenReturn(savedItem);
		when(restTemplate.patchForObject(anyString(), isNull(), eq(Void.class))).thenReturn(null);

		// When
		OrderItemDto result = orderItemService.save(inputDto);

		// Then
		assertNotNull(result);
		assertEquals(1, result.getOrderId());
		assertEquals(100, result.getProductId());
		assertEquals(5, result.getOrderedQuantity());
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class));
		verify(orderItemRepository).save(any(OrderItem.class));
		verify(restTemplate).patchForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1/status"),
				isNull(),
				eq(Void.class));
	}

	@Test
	@DisplayName("save - Should throw IllegalArgumentException when orderId is null")
	void save_ShouldThrowException_WhenOrderIdIsNull() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(null)
				.productId(100)
				.orderedQuantity(5)
				.build();

		// When & Then
		assertThrows(IllegalArgumentException.class, () -> orderItemService.save(inputDto));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("save - Should throw IllegalArgumentException when productId is null")
	void save_ShouldThrowException_WhenProductIdIsNull() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(1)
				.productId(null)
				.orderedQuantity(5)
				.build();

		// When & Then
		assertThrows(IllegalArgumentException.class, () -> orderItemService.save(inputDto));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("save - Should throw IllegalArgumentException when orderedQuantity is null")
	void save_ShouldThrowException_WhenOrderedQuantityIsNull() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(null)
				.build();

		// When & Then
		assertThrows(IllegalArgumentException.class, () -> orderItemService.save(inputDto));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("save - Should throw OrderItemNotFoundException when order not found")
	void save_ShouldThrowException_WhenOrderNotFound() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(999)
				.productId(100)
				.orderedQuantity(5)
				.build();

		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/999"),
				eq(OrderDto.class))).thenReturn(null);

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.save(inputDto));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/999"),
				eq(OrderDto.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("save - Should throw IllegalArgumentException when order status is not CREATED")
	void save_ShouldThrowException_WhenOrderStatusNotCreated() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.build();

		OrderDto orderedOrder = OrderDto.builder()
				.orderId(1)
				.orderStatus(OrderStatus.ORDERED.name())
				.build();

		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(orderedOrder);

		// When & Then
		assertThrows(IllegalArgumentException.class, () -> orderItemService.save(inputDto));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("save - Should throw OrderItemNotFoundException when order fetch fails")
	void save_ShouldThrowException_WhenOrderFetchFails() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.build();

		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenThrow(new RestClientException("Order service unavailable"));

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.save(inputDto));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("save - Should throw OrderItemNotFoundException when product not found")
	void save_ShouldThrowException_WhenProductNotFound() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(1)
				.productId(999)
				.orderedQuantity(5)
				.build();

		OrderDto createdOrder = OrderDto.builder()
				.orderId(1)
				.orderStatus(OrderStatus.CREATED.name())
				.build();

		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(createdOrder);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/999"),
				eq(ProductDto.class))).thenReturn(null);

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.save(inputDto));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/999"),
				eq(ProductDto.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("save - Should throw IllegalArgumentException when ordered quantity exceeds available quantity")
	void save_ShouldThrowException_WhenOrderedQuantityExceedsAvailable() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(15)
				.build();

		OrderDto createdOrder = OrderDto.builder()
				.orderId(1)
				.orderStatus(OrderStatus.CREATED.name())
				.build();

		ProductDto product = ProductDto.builder()
				.productId(100)
				.quantity(10)
				.build();

		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(createdOrder);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenReturn(product);

		// When & Then
		assertThrows(IllegalArgumentException.class, () -> orderItemService.save(inputDto));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("save - Should throw OrderItemNotFoundException when product fetch fails")
	void save_ShouldThrowException_WhenProductFetchFails() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.build();

		OrderDto createdOrder = OrderDto.builder()
				.orderId(1)
				.orderStatus(OrderStatus.CREATED.name())
				.build();

		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(createdOrder);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenThrow(new RestClientException("Product service unavailable"));

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.save(inputDto));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("save - Should handle patch failure gracefully")
	void save_ShouldHandlePatchFailure_WhenStatusUpdateFails() {
		// Given
		OrderItemDto inputDto = OrderItemDto.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.build();

		OrderDto createdOrder = OrderDto.builder()
				.orderId(1)
				.orderStatus(OrderStatus.CREATED.name())
				.build();

		ProductDto product = ProductDto.builder()
				.productId(100)
				.quantity(10)
				.build();

		OrderItem savedItem = OrderItem.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.isActive(true)
				.build();

		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(createdOrder);
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/100"),
				eq(ProductDto.class))).thenReturn(product);
		when(orderItemRepository.save(any(OrderItem.class))).thenReturn(savedItem);
		when(restTemplate.patchForObject(anyString(), isNull(), eq(Void.class)))
				.thenThrow(new RestClientException("Status update failed"));

		// When
		OrderItemDto result = orderItemService.save(inputDto);

		// Then
		assertNotNull(result);
		verify(orderItemRepository).save(any(OrderItem.class));
		verify(restTemplate).patchForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1/status"),
				isNull(),
				eq(Void.class));
	}

	// ========== deleteById() Tests ==========

	@Test
	@DisplayName("deleteById - Should soft delete order item when all validations pass")
	void deleteById_ShouldSoftDeleteOrderItem_WhenAllValidationsPass() {
		// Given
		OrderItem itemToDelete = OrderItem.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.isActive(true)
				.build();

		OrderDto orderedOrder = OrderDto.builder()
				.orderId(1)
				.orderStatus(OrderStatus.ORDERED.name())
				.build();

		when(orderItemRepository.findByOrderIdAndIsActiveTrue(1)).thenReturn(Optional.of(itemToDelete));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(orderedOrder);
		when(orderItemRepository.save(any(OrderItem.class))).thenReturn(itemToDelete);

		// When
		orderItemService.deleteById(1);

		// Then
		verify(orderItemRepository).findByOrderIdAndIsActiveTrue(1);
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
		verify(orderItemRepository).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("deleteById - Should throw OrderItemNotFoundException when order item not found")
	void deleteById_ShouldThrowException_WhenOrderItemNotFound() {
		// Given
		when(orderItemRepository.findByOrderIdAndIsActiveTrue(999)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.deleteById(999));
		verify(orderItemRepository).findByOrderIdAndIsActiveTrue(999);
		verify(restTemplate, never()).getForObject(anyString(), any(Class.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("deleteById - Should throw OrderItemNotFoundException when order not found")
	void deleteById_ShouldThrowException_WhenOrderNotFound() {
		// Given
		OrderItem itemToDelete = OrderItem.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.isActive(true)
				.build();

		when(orderItemRepository.findByOrderIdAndIsActiveTrue(1)).thenReturn(Optional.of(itemToDelete));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(null);

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.deleteById(1));
		verify(orderItemRepository).findByOrderIdAndIsActiveTrue(1);
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("deleteById - Should throw IllegalStateException when order status is not ORDERED")
	void deleteById_ShouldThrowException_WhenOrderStatusNotOrdered() {
		// Given
		OrderItem itemToDelete = OrderItem.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.isActive(true)
				.build();

		OrderDto createdOrder = OrderDto.builder()
				.orderId(1)
				.orderStatus(OrderStatus.CREATED.name())
				.build();

		when(orderItemRepository.findByOrderIdAndIsActiveTrue(1)).thenReturn(Optional.of(itemToDelete));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenReturn(createdOrder);

		// When & Then
		assertThrows(IllegalStateException.class, () -> orderItemService.deleteById(1));
		verify(orderItemRepository).findByOrderIdAndIsActiveTrue(1);
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

	@Test
	@DisplayName("deleteById - Should throw OrderItemNotFoundException when order fetch fails")
	void deleteById_ShouldThrowException_WhenOrderFetchFails() {
		// Given
		OrderItem itemToDelete = OrderItem.builder()
				.orderId(1)
				.productId(100)
				.orderedQuantity(5)
				.isActive(true)
				.build();

		when(orderItemRepository.findByOrderIdAndIsActiveTrue(1)).thenReturn(Optional.of(itemToDelete));
		when(restTemplate.getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class))).thenThrow(new RestClientException("Order service unavailable"));

		// When & Then
		assertThrows(OrderItemNotFoundException.class, () -> orderItemService.deleteById(1));
		verify(orderItemRepository).findByOrderIdAndIsActiveTrue(1);
		verify(restTemplate).getForObject(
				eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
				eq(OrderDto.class));
		verify(orderItemRepository, never()).save(any(OrderItem.class));
	}

}

