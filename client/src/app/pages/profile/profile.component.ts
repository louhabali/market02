import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService, ProfileResponse } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { UserService } from '../../services/user.service';
import { CustomerInsightsResponse, OrderService } from '../../services/order.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit, OnDestroy {
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private mediaService = inject(MediaService);
  private orderService = inject(OrderService);
  private us = inject(UserService);
  private router = inject(Router);

  private userSub!: Subscription;

  form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    avatarUrl: [''],
    role: ['', [Validators.required]]
  });

  user!: ProfileResponse;
  loading = true;
  error = '';
  editing = false;
  selectedAvatar: File | null = null;
  avatarPreview = '';
  shoppingInsights: CustomerInsightsResponse = {
    totalSpent: 0,
    totalOrders: 0,
    topCategory: 'N/A'
  };

  ngOnInit(): void {
    this.userSub = this.us.user$.subscribe((data) => {
      if (data) {
        this.user = data;

        if (!this.editing) {
          this.form.patchValue({
            username: data.name || (data as any).username || '',
            email: data.email || '',
            avatarUrl: data.avatarUrl || '',
            role: data.role || 'CLIENT'
          });
          this.avatarPreview = data.avatarUrl || '';
        }
      }
    });

    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.authService.getProfile().subscribe({
      next: (data: ProfileResponse) => {
        const normalizedUser: ProfileResponse = {
          ...data,
          name: data.name || (data as any).username || '',
          avatarUrl: data.avatarUrl || (data as any).avatar || '',
          role: data.role ? data.role.replace('ROLE_', '') : 'CLIENT'
        };

        this.user = normalizedUser;
        this.us.setUser(normalizedUser);
        this.loading = false;
        this.loadShoppingInsights();
      },
      error: (err) => {
        console.error('Cannot load profile:', err);
        this.error = 'Cannot load profile';
        this.loading = false;
      }
    });
  }

  private loadShoppingInsights(): void {
    if (!this.user || this.user.role !== 'CLIENT') {
      this.shoppingInsights = { totalSpent: 0, totalOrders: 0, topCategory: 'N/A' };
      return;
    }

    this.orderService.getMyInsights().subscribe({
      next: (insights) => {
        this.shoppingInsights = {
          totalSpent: Number(insights?.totalSpent ?? 0),
          totalOrders: Number(insights?.totalOrders ?? 0),
          topCategory: insights?.topCategory || 'N/A'
        };
      },
      error: () => {
        this.shoppingInsights = { totalSpent: 0, totalOrders: 0, topCategory: 'N/A' };
      }
    });
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    this.selectedAvatar = input.files[0];
    const reader = new FileReader();
    reader.onload = () => {
      this.avatarPreview = reader.result as string;
    };
    reader.readAsDataURL(this.selectedAvatar);
  }

  editProfile(): void {
    this.editing = true;
  }

  cancelEdit(): void {
    this.editing = false;
    this.selectedAvatar = null;
    this.error = '';

    if (this.user) {
      this.form.patchValue({
        username: this.user.name || (this.user as any).username || '',
        email: this.user.email || '',
        avatarUrl: this.user.avatarUrl || '',
        role: this.user.role || 'CLIENT'
      });
      this.avatarPreview = this.user.avatarUrl || '';
    }
  }

  saveProfile(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';

    if (this.selectedAvatar) {
      this.mediaService.uploadImages([this.selectedAvatar]).subscribe({
        next: (urls) => this.updateBackend(urls[0]),
        error: () => {
          this.loading = false;
          this.error = 'Failed to upload avatar.';
        }
      });
    } else {
      const currentAvatar = this.user?.avatarUrl || '';
      this.updateBackend(currentAvatar);
    }
  }

  private updateBackend(avatarUrl: string): void {
    const values = this.form.getRawValue();
    this.authService.updateProfile({
      username: values.username,
      email: values.email,
      avatarUrl: avatarUrl,
      role: values.role
    }).subscribe({
      next: (updatedUser: ProfileResponse) => {
        const normalizedUser: ProfileResponse = {
          ...updatedUser,
          name: updatedUser.name || (updatedUser as any).username || values.username,
          avatarUrl: updatedUser.avatarUrl || (updatedUser as any).avatar || avatarUrl,
          role: updatedUser.role ? updatedUser.role.replace('ROLE_', '') : values.role
        };

        this.us.setUser(normalizedUser);
        this.editing = false;
        this.loading = false;
        this.selectedAvatar = null;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.errorMessage ?? 'Failed to update profile.';
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  ngOnDestroy(): void {
    if (this.userSub) {
      this.userSub.unsubscribe();
    }
  }
}