import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ShoppingCart, AddToCartRequest, CartItem } from '../models/cart.model';
import { tap, Observable } from 'rxjs';

export interface CartPageResponse {
  items: CartItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/cart';

  // State via Angular Signals
  private cartSignal = signal<ShoppingCart>({ userId: '', items: [], totalAmount: 0 });

  // Computed Selectors
  readonly cart = this.cartSignal.asReadonly();
  readonly itemCount = computed(() => 
    this.cartSignal().items.reduce((sum, item) => sum + item.quantity, 0)
  );
  readonly totalAmount = computed(() => 
    this.cartSignal().items.reduce((sum, item) => sum + (item.price * item.quantity), 0)
  );

  getCartPage(page = 0, size = 20): Observable<CartPageResponse> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<CartPageResponse>(this.apiUrl, { params }).pipe(
      tap(cartPage => {
        const cart: ShoppingCart = {
          userId: '',
          items: cartPage.items,
          totalAmount: cartPage.items.reduce((sum, item) => sum + (item.price * item.quantity), 0)
        };
        this.cartSignal.set(cart);
      })
    );
  }

  loadCart(page = 0, size = 20) {
    return this.getCartPage(page, size).subscribe();
  }

  addToCart(item: AddToCartRequest): Observable<ShoppingCart> {
    return this.http.post<ShoppingCart>(`${this.apiUrl}/items`, item).pipe(
      tap(updatedCart => this.cartSignal.set(updatedCart))
    );
  }

  updateQuantity(productId: string, quantity: number): Observable<ShoppingCart> {
    return this.http.put<ShoppingCart>(`${this.apiUrl}/items`, { productId, quantity }).pipe(
      tap(updatedCart => this.cartSignal.set(updatedCart))
    );
  }

  removeFromCart(productId: string): Observable<ShoppingCart> {
    return this.http.delete<ShoppingCart>(`${this.apiUrl}/items/${productId}`).pipe(
      tap(updatedCart => this.cartSignal.set(updatedCart))
    );
  }

  clearCart(): Observable<void> {
    return this.http.delete<void>(this.apiUrl).pipe(
      tap(() => this.cartSignal.set({ userId: '', items: [], totalAmount: 0 }))
    );
  }
}