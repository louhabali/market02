import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ShoppingCart, AddToCartRequest } from '../models/cart.model';
import { tap } from 'rxjs';

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

  loadCart() {
    return this.http.get<ShoppingCart>(this.apiUrl).pipe(
      tap(cart => this.cartSignal.set(cart))
    ).subscribe();
  }

  addToCart(item: AddToCartRequest) {
    return this.http.post<ShoppingCart>(`${this.apiUrl}/items`, item).pipe(
      tap(updatedCart => this.cartSignal.set(updatedCart))
    );
  }

  removeFromCart(productId: string) {
    return this.http.delete<ShoppingCart>(`${this.apiUrl}/items/${productId}`).pipe(
      tap(updatedCart => this.cartSignal.set(updatedCart))
    );
  }

  clearCart() {
    return this.http.delete<void>(this.apiUrl).pipe(
      tap(() => this.cartSignal.set({ userId: '', items: [], totalAmount: 0 }))
    );
  }
}