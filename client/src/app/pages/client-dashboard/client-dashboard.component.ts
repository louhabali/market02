import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { OrderService, Order } from '../../services/order.service';
import { CartService } from '../../services/cart.service';

export interface ClientAnalytics {
  totalSpent: number;
  totalOrders: number;
  topCategory: string;
  mostBoughtProducts: { name: string; quantity: number; amount: number }[];
}

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './client-dashboard.component.html'
})
export class ClientDashboardComponent implements OnInit {
  private orderService = inject(OrderService);
  private cartService = inject(CartService);
  private cdr = inject(ChangeDetectorRef);

  loading = true;
  orders: Order[] = [];
  filteredOrders: Order[] = [];
  searchQuery = '';
  statusFilter = 'ALL';

  analytics: ClientAnalytics = {
    totalSpent: 450.00,
    totalOrders: 8,
    topCategory: 'Outerwear',
    mostBoughtProducts: [
      { name: 'Oversized Streetwear Hoodie', quantity: 4, amount: 240.00 },
      { name: 'Vintage Denim Jacket', quantity: 2, amount: 150.00 },
      { name: 'Cargo Pants (Black)', quantity: 2, amount: 60.00 }
    ]
  };

  ngOnInit(): void {
    this.fetchOrders();
  }

  fetchOrders(): void {
    this.loading = true;
    this.orderService.getUserOrders().subscribe({
      next: (data) => {
        this.orders = data || [];
        this.filterOrders();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  filterOrders(): void {
    const q = this.searchQuery.toLowerCase().trim();
    this.filteredOrders = this.orders.filter(order => {
      const matchesSearch = !q || order.id?.toLowerCase().includes(q) || 
        order.items.some(i => i.productName.toLowerCase().includes(q));
      const matchesStatus = this.statusFilter === 'ALL' || order.status === this.statusFilter;
      return matchesSearch && matchesStatus;
    });
  }

  cancelOrder(orderId: string): void {
    if (!confirm('Are you sure you want to cancel this order?')) return;
    this.orderService.cancelOrder(orderId).subscribe({
      next: () => this.fetchOrders(),
      error: (err) => alert(err?.error?.message || 'Could not cancel order')
    });
  }

  redoOrder(order: Order): void {
    order.items.forEach(item => {
      this.cartService.addToCart(item.productId, item.quantity, item.price, item.productName).subscribe();
    });
    alert('Items re-added to your cart!');
  }
}