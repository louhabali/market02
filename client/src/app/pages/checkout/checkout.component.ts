import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './checkout.component.html'
})
export class CheckoutComponent {
  cartService = inject(CartService);
  private orderService = inject(OrderService);
  private fb = inject(FormBuilder);
  router = inject(Router);

  isSubmitting = false;

  checkoutForm = this.fb.group({
    fullName: ['', Validators.required],
    phone: ['', Validators.required],
    streetAddress: ['', Validators.required],
    city: ['', Validators.required],
    postalCode: ['', Validators.required]
  });

  placeOrder(): void {
    if (this.checkoutForm.invalid) return;

    this.isSubmitting = true;

    const request = {
      items: this.cartService.cart().items.map(item => ({
        productId: item.productId,
        sellerId: item.sellerId,
        productName: item.productName,
        price: item.price,
        quantity: item.quantity
      })),
      shippingAddress: this.checkoutForm.value as any
    };

    this.orderService.createOrder(request).subscribe({
      next: (order) => {
        this.isSubmitting = false;
        this.cartService.clearCart().subscribe();
        this.router.navigate(['/profile']);
      },
      error: () => {
        this.isSubmitting = false;
        alert('Failed to place order. Please try again.');
      }
    });
  }
}