import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
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
    totalSpent: 0,
    totalOrders: 0,
    topCategory: 'N/A',
    mostBoughtProducts: []
  };

  ngOnInit(): void {
    this.fetchOrders();
  }

  fetchOrders(): void {
    this.loading = true;
    this.orderService.getMyOrders().subscribe({
      next: (data) => {
        this.orders = data || [];
        this.updateAnalytics();
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

  private updateAnalytics(): void {
    const productTotals = new Map<string, { quantity: number; amount: number }>();

    this.analytics = {
      totalSpent: this.orders.reduce((sum, order) => sum + Number(order.totalAmount || 0), 0),
      totalOrders: this.orders.length,
      topCategory: 'N/A',
      mostBoughtProducts: []
    };

    this.orders.forEach(order => order.items?.forEach(item => {
      const current = productTotals.get(item.productId) || { quantity: 0, amount: 0 };
      current.quantity += item.quantity || 0;
      current.amount += Number(item.priceAtPurchase || 0) * (item.quantity || 0);
      productTotals.set(item.productId, current);
    }));

    this.analytics.mostBoughtProducts = Array.from(productTotals.entries())
      .map(([productId, totals]) => ({
        name: this.orders.flatMap(order => order.items || [])
          .find(item => item.productId === productId)?.productName || 'Unknown product',
        ...totals
      }))
      .sort((first, second) => second.quantity - first.quantity)
      .slice(0, 3);
  }

  filterOrders(): void {
    const q = this.searchQuery.toLowerCase().trim();
    this.filteredOrders = this.orders.filter(order => {
      const matchesSearch = !q || order.id?.toLowerCase().includes(q) || 
        order.items?.some(i => i.productName.toLowerCase().includes(q));
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
    order.items?.forEach(item => {
      this.cartService.addToCart({
        productId: item.productId,
        sellerId: item.sellerId,
        productName: item.productName,
        price: item.priceAtPurchase,
        quantity: item.quantity
      }).subscribe();
    });
    alert('Items re-added to your cart!');
  }
}