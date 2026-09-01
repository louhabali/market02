import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { CreateOrderRequest, Order } from '../models/order.model';
import { Observable } from 'rxjs';

export interface OrderPageResponse {
  content: Order[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface CustomerInsightsResponse {
  totalSpent: number;
  totalOrders: number;
  topCategory: string;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private http = inject(HttpClient);
  private apiUrl = '/api/v1/orders';

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, request);
  }

  getMyOrders(page = 0, size = 20): Observable<OrderPageResponse> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<OrderPageResponse>(`${this.apiUrl}/my-orders`, { params });
  }

  getMyInsights(): Observable<CustomerInsightsResponse> {
    return this.http.get<CustomerInsightsResponse>(`${this.apiUrl}/my-insights`);
  }

  cancelOrder(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/cancel`, {});
  }

  getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  deleteOrder(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}