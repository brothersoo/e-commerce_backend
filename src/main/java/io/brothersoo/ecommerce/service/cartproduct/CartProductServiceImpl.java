package io.brothersoo.ecommerce.service.cartproduct;

import io.brothersoo.ecommerce.domain.product.CartProduct;
import io.brothersoo.ecommerce.domain.product.Product;
import io.brothersoo.ecommerce.domain.user.User;
import io.brothersoo.ecommerce.dto.CartProductDto.CartProductInfo;
import io.brothersoo.ecommerce.repository.cartproduct.CartProductRepository;
import io.brothersoo.ecommerce.service.product.ProductService;
import io.brothersoo.ecommerce.validator.CartProductValidator;
import io.brothersoo.ecommerce.validator.ProductValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장바구니와 관련된 서비스 로직을 포함하는 서비스 클래스입니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CartProductServiceImpl implements CartProductService {

  private final CartProductRepository cartProductRepository;
  private final ProductService productService;
  private final ProductValidator productValidator;
  private final CartProductValidator cartProductValidator;
  private final ModelMapper modelMapper;

  /**
   * id를 사용하여 장바구니 상품을 검색합니다.<br/>
   * id에 해당하는 장바구니 상품이 없을 시 에러를 발생합니다.
   */
  @Override
  public CartProduct findByIdAndValidate(Long cartProductId) {
    CartProduct cartProduct = cartProductRepository.findByIdFetchProduct(cartProductId);
    if (cartProduct == null) {
      throw new IllegalArgumentException("no such cart product found with the given id");
    }
    return cartProduct;
  }

  /**
   * 선택한 상품을 조건 수량만큼 장바구니에 담습니다.<br/>
   * 이미 장바구니에 담겨있는 상품일 시 수량을 추가합니다.<br/>
   * 추가할 수량과 이미 장바구니에 담겨있는 수량의 합이 재고 이하인지 검증합니다.
   * <p>
   * 장바구니에 없는 상품일 경우 새로 추가합니다.<br/>
   * 수량이 재고 이하인지 검증합니다.
   */
  @Override
  @Transactional
  public CartProductInfo addProductToCart(User user, Long productId, int quantity) {
    CartProduct cartProduct = cartProductRepository.findByUserAndProduct(user.getId(), productId);
    if (cartProduct != null) {
      int newQuantity = cartProduct.getQuantity() + quantity;
      productValidator.validateOrderableQuantity(cartProduct.getProduct(), newQuantity);
      cartProduct.modifyQuantity(newQuantity);
    } else {
      Product product = productService.findByIdAndValidate(productId);
      productValidator.validateOrderableQuantity(product, quantity);
      cartProduct = CartProduct.builder()
          .user(user).product(product).quantity(quantity)
          .build();
      user.getCartProducts().add(cartProduct);
    }
    return modelMapper.map(cartProductRepository.save(cartProduct), CartProductInfo.class);
  }

  /**
   * 사용자의 장바구니에 담겨있는 모든 상품을 반환합니다.<br/>
   * 상품 그룹별로 묶어 반환합니다.
   */
  @Override
  public Map<String, List<CartProductInfo>> findAllCartProductsForUser(User user) {
    List<CartProductInfo> cartProductInfos
        = cartProductRepository.findAllByUserEmail(user.getEmail());
    Map<String, List<CartProductInfo>> groupedCartProductInfo = new HashMap<>();
    for (CartProductInfo cartProductInfo : cartProductInfos) {
      cartProductValidator.validateIsInStock(cartProductInfo);

      groupedCartProductInfo.putIfAbsent(
          cartProductInfo.getProduct().getProductGroup().getName(),
          new ArrayList<>()
      );
      groupedCartProductInfo.computeIfPresent(
          cartProductInfo.getProduct().getProductGroup().getName(),
          (String k, List<CartProductInfo> v) -> {
            v.add(cartProductInfo);
            return v;
          }
      );
    }
    return groupedCartProductInfo;
  }

  /**
   * 장바구니의 상품 수량을 수정하는 로직입니다.<br/>
   * 장바구니 상품이 요청한 사용자의 장바구니에 담긴 상품인지 검증합니다.<br/>
   * 수정 할 수량이 상품의 재고 내의 범위인지 검증합니다.
   */
  @Override
  @Transactional
  public CartProductInfo modifyCartProductQuantity(
      Long cartProductId, User user, int quantity
  ) {
    CartProduct cartProduct = findByIdAndValidate(cartProductId);
    cartProductValidator.validateOwner(cartProduct, user);
    productValidator.validateOrderableQuantity(cartProduct.getProduct(), quantity);
    cartProduct.modifyQuantity(quantity);
    return modelMapper.map(cartProduct, CartProductInfo.class);
  }

  /**
   * 장바구니에 담긴 상품을 제거합니다.<br/>
   * 해당 장바구니 상품이 요청한 사용자에게 해당하는 상품인지 검증합니다.
   */
  @Override
  @Transactional
  public Long deleteCartProductById(Long cartProductId, User user) {
    CartProduct cartProduct = findByIdAndValidate(cartProductId);
    cartProductValidator.validateOwner(cartProduct, user);
    cartProductRepository.delete(cartProduct);
    return cartProductId;
  }

  /**
   * 유저의 장바구니 내 상품이 해당 상품의 재고 범위에 맞지 않는 경우 처리하는 서비스 로직입니다.
   * "reset" 과 "remove"에 따라 다른 기능을 수행합니다.
   */
  @Override
  @Transactional
  public List<Long> handleCartProductQuantityExceededStock(String type, User user) {
    List<Long> cartProductQuantityExceededStockIds = new ArrayList<>();
    if (type.equals("reset")) {
      resetCartProductQuantityExceededStock(user, cartProductQuantityExceededStockIds);
    } else if (type.equals("remove")) {
      removeCartProductQuantityExceededStock(user, cartProductQuantityExceededStockIds);
    }
    return cartProductQuantityExceededStockIds;
  }

  /**
   * reset인 경우 장바구니 상품의 수량을 해당 상품의 재고 수로 변경합니다.
   * 해당 상품의 재고가 0인 경우 장바구니 상품을 장바구니에서 제거합니다.
   */
  @Override
  @Transactional
  public void resetCartProductQuantityExceededStock(User user, List<Long> cartProductIds) {
    List<CartProduct> cartProducts
        = cartProductRepository.findQuantityExceededStockFetchProduct(user.getId());
    for (CartProduct cartProduct : cartProducts) {
      cartProductValidator.validateOwner(cartProduct, user);
      cartProductIds.add(cartProduct.getId());
      if (cartProduct.isInStock()) {
        cartProduct.modifyQuantity(cartProduct.getProduct().getStock());
      } else {
        cartProductRepository.delete(cartProduct);
      }
    }
  }

  /**
   * remove인 경우 장바구니 상품을 장바구니에서 제거합니다.
   */
  @Override
  public void removeCartProductQuantityExceededStock(User user, List<Long> cartProductIds) {
    List<CartProduct> cartProducts
        = cartProductRepository.findQuantityExceededStockFetchProduct(user.getId());
    for (CartProduct cartProduct : cartProducts) {
      cartProductValidator.validateOwner(cartProduct, user);
      cartProductIds.add(cartProduct.getId());
    }
    cartProductRepository.deleteAll(cartProducts);
  }
}
