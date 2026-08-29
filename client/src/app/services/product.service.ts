


import { Injectable } from '@angular/core';

import { HttpClient, HttpHeaders ,HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { Product } from '../models/product';
import { environment } from '../../environments/environment';


export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
@Injectable({
  providedIn: 'root'
})
export class ProductService {
 

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'X-User-Id': this.authService.getUserId() || '',
      'X-Role': this.authService.getRole() || 'CLIENT' 
    });
  }

  getAllProducts(): Observable<Product[]> {
    return this.http.get<Product[]>( `${environment.apiUrl}/api/products`);
  }
    getProduct(id: string): Observable<Product> {
     return this.http.get<Product>(
       `${environment.apiUrl}/api/products/${id}`
     ); 
   }
   getProductById(id: string): Observable<Product> {
    return this.getProduct(id);
  }

  // Search products with backend pagination
  searchProducts(
    keyword: string, 
    category: string, 
    minPrice: number | null, 
    maxPrice: number | null, 
    page: number = 0, 
    size: number = 12
  ): Observable<PageResponse<Product>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (keyword && keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    if (category && category !== 'ALL') {
      params = params.set('category', category);
    }
    if (minPrice !== null && minPrice !== undefined) {
      params = params.set('minPrice', minPrice.toString());
    }
    if (maxPrice !== null && maxPrice !== undefined) {
      params = params.set('maxPrice', maxPrice.toString());
    }

    return this.http.get<PageResponse<Product>>(`${environment.apiUrl}/api/products/search`, { params });
  }
  createProduct(formData: FormData): Observable<Product> {
    // Angular handles Content-Type boundaries automatically when passing FormData
    return this.http.post<Product>(`${environment.apiUrl}/api/products`, formData, {
      headers: this.getHeaders()
    });
  }

  updateProduct(id: string, formData: FormData): Observable<Product> {
    return this.http.put<Product>(`${environment.apiUrl}/api/products/${id}`, formData, {
      headers: this.getHeaders()
    });
  }

  deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/products/${id}`, {
      headers: this.getHeaders()
    });
  }
  
}
