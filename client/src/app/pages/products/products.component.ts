import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription, forkJoin, timer, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ProductService } from '../../services/product.service';
import { AuthService, ProfileResponse } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { Product } from '../../models/product';
import { ProductCardComponent } from '../../../shared/product-card/product-card.component';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ProductCardComponent],
  templateUrl: './products.component.html'
})
export class ProductsComponent implements OnInit, OnDestroy {
  products: Product[] = [];
  filteredProducts: Product[] = [];
  
  isLoading = true;
  userLoading = true;
  error: string | null = null;
  currentUser: ProfileResponse | null = null;
  private userSub!: Subscription;

  // Search & Filter State
  searchKeyword = '';
  selectedCategory = 'ALL';
  maxPrice: number = 2000;

  constructor(
    private productService: ProductService,
    public authService: AuthService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // 1. Listen for user profile state
    this.userSub = this.userService.user$.subscribe({
      next: (user) => {
        this.currentUser = user;
      },
      error: () => {
        this.currentUser = null;
      }
    });

    // 2. Load products with parallel delay execution
    this.loadDataWithDelay();
  }

  get canAddProduct(): boolean {
    const rawRole = this.currentUser?.role || this.authService.getRole() || '';
    const normalizedRole = rawRole.replace('ROLE_', '').toUpperCase();
    return normalizedRole === 'SELLER';
  }

  loadDataWithDelay(): void {
    this.isLoading = true;
    this.userLoading = true;
    this.error = null;

    // Run a 1000ms timer and the API request in parallel
    forkJoin({
      timerDelay: timer(1000),
      productsData: this.productService.getAllProducts().pipe(
        catchError((err) => {
          console.error('Failed to load products', err);
          this.error = 'Could not fetch products from backend.';
          return of([]);
        })
      )
    }).subscribe(({ productsData }) => {
      if (Array.isArray(productsData)) {
        this.products = productsData.map((p: any) => ({
          ...p,
          id: p.id || p._id || (p._id && p._id.$oid ? p._id.$oid : '')
        }));
        this.applyFilters();
      } else {
        this.products = [];
        this.filteredProducts = [];
      }

      // Hide both loaders at the exact same moment
      this.isLoading = false;
      this.userLoading = false;
      this.cdr.detectChanges();
    });
  }

  applyFilters(): void {
    const kw = this.searchKeyword.toLowerCase().trim();
    this.filteredProducts = this.products.filter(p => {
      const matchesKw = !kw || p.name.toLowerCase().includes(kw) || p.description?.toLowerCase().includes(kw);
      const matchesCategory = this.selectedCategory === 'ALL' || p.category === this.selectedCategory;
      const matchesPrice = !p.price || p.price <= this.maxPrice;
      return matchesKw && matchesCategory && matchesPrice;
    });
  }

  onDeleteProduct(productId: string): void {
    if (!productId) {
      console.error('Cannot delete: Product ID is empty');
      return;
    }

    this.productService.deleteProduct(productId).subscribe({
      next: () => {
        this.products = this.products.filter(
          (p) => p.id !== productId && (p as any)._id !== productId
        );
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to delete product', err);
        this.error = err?.error?.errorMessage || err?.error?.message || 'Could not delete product from backend.';
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.userSub) {
      this.userSub.unsubscribe();
    }
  }
}