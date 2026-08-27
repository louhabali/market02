import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterRequest } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';

export type UserRole = 'SELLER' | 'CLIENT';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    FormsModule
  ],
  templateUrl: './register.component.html'
})
export class RegisterComponent implements OnInit {

  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private mediaService = inject(MediaService);
  private router = inject(Router);

  avatarPreview: string | null = null;
  selectedAvatar: File | null = null;
  loading = false;
  error = '';

  // Regex patterns aligned with backend specifications
  private usernameRegex = /^[a-zA-Z0-9]{3,20}$/;
  private emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  private passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{4,}$/;

  form = this.fb.group({
    username: ['', [Validators.required, Validators.pattern(this.usernameRegex)]],
    email: ['', [Validators.required, Validators.pattern(this.emailRegex)]],
    password: ['', [Validators.required, Validators.pattern(this.passwordRegex)]],
    role: ['SELLER' as UserRole, [Validators.required, Validators.pattern(/^(SELLER|CLIENT)$/)]]
  });

  ngOnInit(): void {
    // Clear global error message when form inputs change
    this.form.valueChanges.subscribe(() => {
      if (this.error) {
        this.error = '';
      }
    });
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const file = input.files[0];
    this.selectedAvatar = file;

    const reader = new FileReader();
    reader.onload = () => {
      this.avatarPreview = reader.result as string;
    };
    reader.readAsDataURL(file);
  }

  register(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    if (this.selectedAvatar) {
      this.mediaService.uploadPublicAvatar(this.selectedAvatar).subscribe({
        next: (res) => {
          this.registerUser(res.avatarUrl);
        },
        error: (er) => {
          console.error('Avatar upload failed:', er);
          this.loading = false;
          this.error = er?.error?.error ?? er?.error?.message ?? 'Failed to upload avatar.';
        }
      });
    } else {
      this.registerUser('');
    }
  }

  private registerUser(avatarUrl: string): void {
    const rawValues = this.form.getRawValue();

    // Sanitize and trim values prior to sending request
    const data: RegisterRequest = {
      username: rawValues.username.trim(),
      email: rawValues.email.trim().toLowerCase(),
      password: rawValues.password.trim(),
      role: rawValues.role.trim() as UserRole,
      avatarUrl: avatarUrl ? avatarUrl.trim() : ''
    };

    this.authService.register(data).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Backend registration error:', err);
        this.loading = false;
        this.error = err?.error?.errorMessage ?? err?.error?.message ?? 'Registration failed. Please try again later.';
      }
    });
  }
}