import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';

export interface SellerAnalytics {
  totalRevenue: number;
  totalUnitsSold: number;
  totalOrders: number;
  topProducts: { productId: string; name: string; unitsSold: number; revenue: number }[];
}

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './seller-dashboard.component.html'
})
export class SellerDashboardComponent implements OnInit {
  private http = inject(HttpClient);

  loading = true;
  analytics: SellerAnalytics = {
    totalRevenue: 0,
    totalUnitsSold: 0,
    totalOrders: 0,
    topProducts: []
  };

  ngOnInit(): void {
    this.fetchAnalytics();
  }

  fetchAnalytics(): void {
    this.loading = true;
    this.http.get<SellerAnalytics>('/api/v1/orders/seller/analytics').subscribe({
      next: (data) => {
        if (data) this.analytics = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}