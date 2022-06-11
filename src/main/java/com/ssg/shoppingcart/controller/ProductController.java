package com.ssg.shoppingcart.controller;

import com.ssg.shoppingcart.dto.ProductDto.PriceRangeInGroups;
import com.ssg.shoppingcart.dto.ProductDto.ProductInfo;
import com.ssg.shoppingcart.dto.ProductDto.ProductListFilter;
import com.ssg.shoppingcart.service.product.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @GetMapping
  public ResponseEntity<Page<ProductInfo>> filteredProductList(
      ProductListFilter filter, @PageableDefault Pageable pageable
  ) {
    return new ResponseEntity<>(
        productService.findFilteredProducts(filter, pageable), HttpStatus.OK
    );
  }

  @GetMapping("/price_range")
  public ResponseEntity<PriceRangeInGroups> priceRangeInGroupsRetrieve(
      @RequestParam("groupIds") List<Long> groupIds
  ) {
    return new ResponseEntity<>(
        productService.getMinMaxPriceInProductGroups(groupIds),
        HttpStatus.OK
    );
  }
}
