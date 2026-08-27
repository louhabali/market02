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
  filteredProducts: Product[] = [];
  isLoading = true;
  error: string | null = null;
  currentUser: ProfileResponse | null = null;
  private userSub!: Subscription;

  // Search & Filter State
  searchKeyword = '';
  selectedCategory = 'ALL';
  maxPrice: number = 1000;

  constructor(
    private productService: ProductService,
    public authService: AuthService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.userSub = this.userService.user$.subscribe((user) => {
      this.currentUser = user;
      this.cdr.detectChanges();
    });

    this.loadProducts();
  }

  get canAddProduct(): boolean {
    const role = this.currentUser?.role || this.authService.getRole();
    return role === 'SELLER';
  }

  loadProducts(): void {
    this.isLoading = true;
    this.productService.getAllProducts().subscribe({
      next: (data: any[]) => {
        if (Array.isArray(data)) {
          this.products = data.map(p => ({
            ...p,
            id: p.id || p._id || (p._id && p._id.$oid ? p._id.$oid : '')
          }));
          this.applyFilters();
        } else {
          this.products = [];
          this.filteredProducts = [];
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Could not fetch products from backend.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
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

  ngOnDestroy(): void {
    if (this.userSub) this.userSub.unsubscribe();
  }

  onDeleteProduct(productId: string): void {
    if (!productId) return;
    this.productService.deleteProduct(productId).subscribe({
      next: () => {
        this.products = this.products.filter(p => p.id !== productId);
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not delete product.';
        this.cdr.detectChanges();
      }
    });
  }
}