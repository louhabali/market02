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

  cartMessage: string | null = null;
  cartMessageType: 'error' | 'success' = 'error';
  cartPage = 0;
  cartPageSize = 20;
  cartTotalPages = 1;
  cartTotalItems = 0;

  ngOnInit(): void {
    this.loadCartPage();
  }

  loadCartPage(): void {
    this.cartService.getCartPage(this.cartPage, this.cartPageSize).subscribe({
      next: (page) => {
        this.cartTotalPages = page.totalPages > 0 ? page.totalPages : 1;
        this.cartTotalItems = page.totalElements;
      },
      error: () => {
        this.cartTotalPages = 1;
        this.cartTotalItems = 0;
      }
    });
  }

  updateQuantity(productId: string, currentQty: number, delta: number): void {
    const newQty = currentQty + delta;
    if (newQty <= 0) {
      this.removeItem(productId);
      return;
    }
    this.cartService.updateQuantity(productId, newQty).subscribe({
      next: () => {
        this.cartMessage = null;
      },
      error: (err) => {
        this.cartMessageType = 'error';
        this.cartMessage = err?.error?.message || 'Unable to update quantity due to stock availability.';
      }
    });
  }

  removeItem(productId: string): void {
    this.cartService.removeFromCart(productId).subscribe({
      next: () => this.loadCartPage()
    });
  }

  clearCart(): void {
    this.cartService.clearCart().subscribe({
      next: () => {
        this.cartPage = 0;
        this.loadCartPage();
      }
    });
  }

  previousCartPage(): void {
    if (this.cartPage > 0) {
      this.cartPage -= 1;
      this.loadCartPage();
    }
  }

  nextCartPage(): void {
    if (this.cartPage < this.cartTotalPages - 1) {
      this.cartPage += 1;
      this.loadCartPage();
    }
  }

  proceedToCheckout(): void {
    this.router.navigate(['/checkout']);
  }

  dismissCartMessage(): void {
    this.cartMessage = null;
  }
}