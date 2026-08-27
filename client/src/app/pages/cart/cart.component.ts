import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cart.component.html'
})
export class CartComponent implements OnInit {
  cartService = inject(CartService);
  private router = inject(Router);

  ngOnInit(): void {
    this.cartService.loadCart();
  }

  updateQuantity(productId: string, currentQty: number, delta: number): void {
    const newQty = currentQty + delta;
    if (newQty <= 0) {
      this.removeItem(productId);
      return;
    }
    this.cartService.updateQuantity(productId, newQty).subscribe();
  }

  removeItem(productId: string): void {
    this.cartService.removeFromCart(productId).subscribe();
  }

  clearCart(): void {
    this.cartService.clearCart().subscribe();
  }

  proceedToCheckout(): void {
    this.router.navigate(['/checkout']);
  }
}