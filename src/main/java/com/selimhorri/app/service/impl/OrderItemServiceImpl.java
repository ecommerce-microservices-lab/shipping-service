package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.OrderStatus;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;
import com.selimhorri.app.helper.OrderItemMappingHelper;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

	private final OrderItemRepository orderItemRepository;
	private final RestTemplate restTemplate;

	@Override
	public List<OrderItemDto> findAll() {
		log.info("*** OrderItemDto List, service; fetch all active orderItems *");
		return this.orderItemRepository.findByIsActiveTrue()
				.stream()
				.map(OrderItemMappingHelper::map)
				.filter(o -> {
					// Verificar producto
					if (o.getProductDto() != null && o.getProductDto().getProductId() != null) {
						try {
							ProductDto product = this.restTemplate.getForObject(
									AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" +
											o.getProductDto().getProductId(),
									ProductDto.class);
							if (product == null) {
								log.warn("Product {} not found", o.getProductDto().getProductId());
								return false;
							}
							o.setProductDto(product);
						} catch (RestClientException e) {
							log.warn("Failed to fetch product with id: {}", o.getProductDto().getProductId(), e);
							return false;
						}
					} else {
						return false;
					}

					// Verificar orden
					if (o.getOrderDto() != null && o.getOrderDto().getOrderId() != null) {
						try {
							OrderDto order = this.restTemplate.getForObject(
									AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" +
											o.getOrderDto().getOrderId(),
									OrderDto.class);
							if (order == null) {
								log.warn("Order {} not found", o.getOrderDto().getOrderId());
								return false;
							}
							System.out.println("====================");
							System.out.println(OrderStatus.ORDERED.name());
							System.out.println(order.getOrderStatus());
							System.out.println("====================");

							if (!OrderStatus.ORDERED.name().equals(order.getOrderStatus())) {
								return false;
							}
							o.setOrderDto(order);
						} catch (RestClientException e) {
							log.warn("Failed to fetch order with id: {}", o.getOrderDto().getOrderId(), e);
							return false;
						}
					} else {
						return false;
					}

					return true;
				})
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public OrderItemDto findById(final int orderItemId) {
		log.info("*** OrderItemDto, service; fetch orderItem by id *");

		OrderItem orderItem = this.orderItemRepository.findById(orderItemId)
				.filter(OrderItem::isActive)
				.orElseThrow(() -> new OrderItemNotFoundException(
						String.format("Active OrderItem with id: %s not found", orderItemId)));

		OrderItemDto dto = OrderItemMappingHelper.map(orderItem);

		// Verificar y cargar producto
		if (dto.getProductDto() != null && dto.getProductDto().getProductId() != null) {
			try {
				ProductDto product = this.restTemplate.getForObject(
						AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" +
								dto.getProductDto().getProductId(),
						ProductDto.class);
				dto.setProductDto(product);
			} catch (RestClientException e) {
				log.error("Failed to fetch product details for order item: {}", orderItemId, e);
				throw new OrderItemNotFoundException("Product information not available for this order item");
			}
		}

		// Verificar y cargar orden, y comprobar su estado
		if (dto.getOrderDto() != null && dto.getOrderDto().getOrderId() != null) {
			try {
				OrderDto order = this.restTemplate.getForObject(
						AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" +
								dto.getOrderDto().getOrderId(),
						OrderDto.class);

				if (order == null) {
					throw new OrderItemNotFoundException("Associated order not found");
				}

				// Verificar que el estado sea ORDERED
				if (!OrderStatus.ORDERED.name().equals(order.getOrderStatus())) {
					throw new OrderItemNotFoundException("Shipping not found");
				}

				dto.setOrderDto(order);
			} catch (RestClientException e) {
				log.error("Failed to fetch order details for order item: {}", orderItemId, e);
				throw new OrderItemNotFoundException("Order information not available for this order item");
			}
		} else {
			throw new OrderItemNotFoundException("No associated order found for this order item");
		}

		return dto;
	}

	@Override
	public OrderItemDto save(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; save orderItem *");
		if (orderItemDto.getOrderId() == null || orderItemDto.getProductId() == null
				|| orderItemDto.getOrderedQuantity() == null) {
			throw new IllegalArgumentException(
					"To create a shipping you have to provide a valid orderId, productId and orderedQuantity");
		}

		// Verify the order exists first
		OrderDto order;
		try {
			order = this.restTemplate.getForObject(
					AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/"
							+ orderItemDto.getOrderId(),
					OrderDto.class);

			if (order == null) {
				throw new OrderItemNotFoundException(
						"Order with ID " + orderItemDto.getOrderId() + " not found");
			}

			if (!order.getOrderStatus().equals(OrderStatus.CREATED.name())) {
				throw new IllegalArgumentException(
						"Cannot create a shipping for an order that is in any state other than CREATED");
			}
		} catch (RestClientException e) {
			throw new OrderItemNotFoundException("Error verifying order existence: " + e.getMessage());
		}

		// Verify the product exists
		try {
			ProductDto product = this.restTemplate.getForObject(
					AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/"
							+ orderItemDto.getProductId(),
					ProductDto.class);

			if (product == null) {
				throw new OrderItemNotFoundException(
						"Product with ID " + orderItemDto.getProductId() + " not found");
			}

			if (product.getQuantity() < orderItemDto.getOrderedQuantity()) {
				throw new IllegalArgumentException(
						"You cannot order more units than there is available, available units: "
								+ product.getQuantity());
			}
		} catch (RestClientException e) {
			throw new OrderItemNotFoundException("Error verifying product existence: " + e.getMessage());
		}

		// Save the order item
		OrderItemDto savedItem = OrderItemMappingHelper.map(
				this.orderItemRepository.save(OrderItemMappingHelper.mapForCreation(orderItemDto)));

		// Update order status after successful save
		try {
			String patchUrl = AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/"
					+ orderItemDto.getOrderId() + "/status";

			this.restTemplate.patchForObject(
					patchUrl,
					null, // No request body
					Void.class);

		} catch (RestClientException e) {
			log.error("Failed to update order status after saving item: " + e.getMessage());

		}

		return savedItem;
	}

	@Override
	@Transactional
	public void deleteById(final int orderItemId) {
		log.info("*** Void, service; soft delete orderItem by id *");

		this.orderItemRepository.findByOrderIdAndIsActiveTrue(orderItemId)
				.ifPresentOrElse(orderItem -> {
					// Verificar si la orden asociada tiene estado ORDERED
					try {
						OrderDto order = this.restTemplate.getForObject(
								AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" +
										orderItem.getOrderId(),
								OrderDto.class);

						if (order == null) {
							throw new OrderItemNotFoundException("Associated order not found");
						}

						if (!OrderStatus.ORDERED.name().equals(order.getOrderStatus())) {
							throw new IllegalStateException(
									"Cannot delete order item - associated order is not in ORDERED status");
						}

						orderItem.setActive(false);
						this.orderItemRepository.save(orderItem);
						log.info("OrderItem with id {} has been deactivated", orderItemId);

					} catch (RestClientException e) {
						log.error("Failed to fetch order details for order item: {}", orderItemId, e);
						throw new OrderItemNotFoundException("Order information not available for this order item");
					}

				}, () -> {
					throw new OrderItemNotFoundException(
							String.format("OrderItem with id: %s not found", orderItemId));
				});
	}
}
