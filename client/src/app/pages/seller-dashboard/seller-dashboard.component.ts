import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';

export interface SellerAnalytics {
  totalRevenue: number;
  totalUnitsSold: number;
  totalOrders: number;
  topProducts: { name: string; unitsSold: number; revenue: number }[];
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
    totalRevenue: 3420.50,
    totalUnitsSold: 142,
    totalOrders: 38,
    topProducts: [
      { name: 'Oversized Streetwear Hoodie', unitsSold: 54, revenue: 1620.00 },
      { name: 'Vintage Denim Jacket', unitsSold: 32, revenue: 960.00 },
      { name: 'Cargo Pants (Black)', unitsSold: 28, revenue: 560.00 }
    ]
  };

  ngOnInit(): void {
    this.fetchAnalytics();
  }

  fetchAnalytics(): void {
    this.loading = true;
    this.http.get<SellerAnalytics>('/api/v1/sellers/analytics').subscribe({
      next: (data) => {
        if (data) this.analytics = data;
        this.loading = false;
      },
      error: () => {
        // Fallback to initial display state if analytics endpoint is offline
        this.loading = false;
      }
    });
  }
}