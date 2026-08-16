export interface ShippingAddress {
  fullName: string;
  phone: string;
  streetAddress: string;
  city: string;
  postalCode: string;
}

export interface OrderItem {
  productId: string;
  sellerId: string;
  productName: string;
  priceAtPurchase: number;
  quantity: number;
}

export interface CreateOrderRequest {
  items: {
    productId: string;
    sellerId: string;
    productName: string;
    price: number;
    quantity: number;
  }[];
  shippingAddress: ShippingAddress;
}

export interface Order {
  id: string;
  customerId: string;
  items: OrderItem[];
  totalAmount: number;
  status: 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  paymentMethod: string;
  shippingAddress: ShippingAddress;
  createdAt: string;
}