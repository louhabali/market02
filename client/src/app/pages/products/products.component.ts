import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  imports: [CommonModule, RouterLink, ProductCardComponent],
  templateUrl: './products.component.html'
})
export class ProductsComponent implements OnInit, OnDestroy {
  products: Product[] = [];
  isLoading = true;
  error: string | null = null;
  currentUser: ProfileResponse | null = null;
  private userSub!: Subscription;

  constructor(
    private productService: ProductService,
    public authService: AuthService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // In-memory subscription
    this.userSub = this.userService.user$.subscribe((user) => {
      this.currentUser = user;
      this.cdr.detectChanges();
    });

    this.loadProducts();
  }

  get canAddProduct(): boolean {
    const role = this.currentUser?.role || this.authService.getRole() ;
    return role === 'SELLER';
  }

  loadProducts(): void {
    this.isLoading = true;
    this.productService.getAllProducts().subscribe({
      next: (data: any[]) => {
     
        console.log(data)
        if (Array.isArray(data)) {
          this.products = data.map(p => ({
            ...p,
            id: p.id || p._id || (p._id && p._id.$oid ? p._id.$oid : '')
            
          }));
        } else {
          this.products = [];
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load products', err);
        this.error = 'Could not fetch products from backend.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.userSub) {
      this.userSub.unsubscribe();
    }
  }
  onDeleteProduct(productId: string): void {
  if (!productId) {
    console.error('Cannot delete: Product ID is empty');
    return;
  }

  this.productService.deleteProduct(productId).subscribe({
    next: () => {
      // Instantly remove the deleted product from local array to update UI
      this.products = this.products.filter(
        (p) => p.id !== productId && (p as any)._id !== productId
      );
      this.cdr.detectChanges();
    },
    error: (err) => {
      console.error('Failed to delete product', err);
      this.error = err?.error?.errorMessage || err?.error?.message || 'Could not delete product from backend.';
      this.cdr.detectChanges();
    }
  });
}
}