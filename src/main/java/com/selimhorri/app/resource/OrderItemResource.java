package com.selimhorri.app.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.OrderItemService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/shippings")
@Slf4j
@RequiredArgsConstructor
public class OrderItemResource {
	
	private final OrderItemService orderItemService;
	
	@GetMapping
	public ResponseEntity<DtoCollectionResponse<OrderItemDto>> findAll() {
		log.info("*** OrderItemDto List, controller; fetch all orderItems *");
		return ResponseEntity.ok(new DtoCollectionResponse<>(this.orderItemService.findAll()));
	}
	
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderItemDto> findById(
			@PathVariable("orderId") final String orderId) {
		log.info("*** OrderItemDto, resource; fetch orderItem by id *");
		return ResponseEntity.ok(this.orderItemService.findById(Integer.parseInt(orderId)));
	}
	
	@PostMapping
	public ResponseEntity<OrderItemDto> save(
			@RequestBody 
			@NotNull(message = "Input must not be NULL") 
			@Valid final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, resource; save orderItem *");
		return ResponseEntity.ok(this.orderItemService.save(orderItemDto));
	}
	
	@DeleteMapping("/{orderId}")
	public ResponseEntity<Boolean> deleteById(
			@PathVariable("orderId") final String orderId) {
		log.info("*** Boolean, resource; delete orderItem by id *");
		this.orderItemService.deleteById(Integer.parseInt(orderId));
		return ResponseEntity.ok(true);
	}
	
}










