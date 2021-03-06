package io.brothersoo.ecommerce.validator;

import io.brothersoo.ecommerce.domain.product.CartProduct;
import io.brothersoo.ecommerce.domain.user.User;
import io.brothersoo.ecommerce.dto.CartProductDto.CartProductInfo;
import io.brothersoo.ecommerce.exception.order.ProductOutOfStockException;
import org.springframework.stereotype.Component;

/**
 * 장바구니 상품 서비스 내 검증
 */
@Component
public class CartProductValidator {

  /**
   * 장바구니 상품이 요청한 사용자의 것인지 검증
   *
   * @param cartProduct
   * @param user
   */
  public void validateOwner(CartProduct cartProduct, User user) {
    if (!cartProduct.getUser().getEmail().equals(user.getEmail())) {
      throw new IllegalArgumentException("not the owner of the cart product");
    }
  }

  /**
   * 장바구니 상품의 수량이 해당 상품의 재고 범위 내인지 검증
   */
  public void validateIsInStock(CartProduct cartProduct) {
    if (cartProduct.isOrderableQuantity()) {
      throw new ProductOutOfStockException("product out of stock");
    }
  }

  public void validateIsInStock(CartProductInfo cartProduct) {
    if (cartProduct.isOrderableQuantity()) {
      throw new ProductOutOfStockException("product out of stock");
    }
  }
}
