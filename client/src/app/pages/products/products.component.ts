import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
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
  
  initialLoading = true;
  isFiltering = false;
  userLoading = true;
  error: string | null = null;
  currentUser: ProfileResponse | null = null;
  private userSub!: Subscription;

  // Search & Filter state
  searchKeyword = '';
  selectedCategory = 'ALL';
  readonly categories = ['Streetwear', 'Outerwear', 'Accessories'];
  minPrice: number | null = null;
  maxPrice: number | null = null;

  // Pagination state
  currentPage = 0;
  pageSize = 12;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private productService: ProductService,
    public authService: AuthService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.userSub = this.userService.user$.subscribe({
      next: (user) => {
        this.currentUser = user;
        this.userLoading = false;
      },
      error: () => {
        this.currentUser = null;
        this.userLoading = false;
      }
    });

    this.fetchProducts(true);
  }

  get canAddProduct(): boolean {
    const rawRole = this.currentUser?.role || this.authService.getRole() || '';
    const normalizedRole = rawRole.replace('ROLE_', '').toUpperCase();
    return normalizedRole === 'SELLER';
  }

  // Fetch paginated & filtered products from backend
  fetchProducts(isInitial: boolean = false): void {
    if (isInitial) {
      this.initialLoading = true;
    } else {
      this.isFiltering = true;
    }
    this.error = null;

    this.productService.searchProducts(
      this.searchKeyword,
      this.selectedCategory,
      this.minPrice,
      this.maxPrice,
      this.currentPage,
      this.pageSize
    ).subscribe({
      next: (res) => {
        this.products = (res.content || []).map((p: any) => ({
          ...p,
          id: p.id || p._id || (p._id && p._id.$oid ? p._id.$oid : '')
        }));
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
        this.initialLoading = false;
        this.isFiltering = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load products', err);
        this.error = 'Could not fetch products from backend.';
        this.initialLoading = false;
        this.isFiltering = false;
        this.cdr.detectChanges();
      }
    });
  }

  // Reset to first page on filter input
  onFilterChange(): void {
    this.currentPage = 0;
    this.fetchProducts(false);
  }

  // Pagination page navigation
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.fetchProducts(false);
    }
  }

  onDeleteProduct(productId: string): void {
    if (!productId) return;

    this.productService.deleteProduct(productId).subscribe({
      next: () => {
        this.fetchProducts(false);
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