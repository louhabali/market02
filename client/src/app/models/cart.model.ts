export interface CartItem {
  productId: string;
  sellerId: string;
  productName: string;
  imageUrl?: string;
  price: number;
  quantity: number;
}

export interface ShoppingCart {
  userId: string;
  items: CartItem[];
  totalAmount: number;
}

export interface AddToCartRequest {
  productId: string;
  sellerId: string;
  productName: string;
  imageUrl?: string;
  price: number;
  quantity: number;
}