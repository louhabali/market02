import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, BehaviorSubject } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../environments/environment';
import { UserService } from './user.service';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  role: string;
  avatarUrl?: string;
}

export interface ProfileResponse {
  id: string;
  name: string;
  email: string;
  role: string;
  avatarUrl: string;
  createdAt: string;
}

export interface JwtPayload {
  sub: string;
  userId: string;
  role: string;
  exp: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private userService = inject(UserService);

  private readonly TOKEN_KEY = 'token'; // Kept only for session persistence across refreshes
  private loggedInSubject = new BehaviorSubject<boolean>(this.hasToken());
  loggedIn$ = this.loggedInSubject.asObservable();

  login(data: LoginRequest): Observable<any> {
    return this.http.post<any>(`${environment.apiUrl}/api/auth/login`, data).pipe(
      tap(res => {
        this.saveToken(res.token);
        
      })
    );
  }

  getProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(`${environment.apiUrl}/api/auth/profile`).pipe(
      tap(profile => {
        // Hydrate in-memory state directly from secure API response
        this.userService.setUser(profile);
      })
    );
  }

  updateProfile(profile: { username: string; email: string; avatarUrl: string; role: string }): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(`${environment.apiUrl}/api/auth/profile`, profile).pipe(
      tap(updatedProfile => {
        // Broadcast profile/role update across the app memory
        this.userService.setUser(updatedProfile);
      })
    );
  }

  register(data: RegisterRequest): Observable<any> {
    return this.http.post(`${environment.apiUrl}/api/auth/register`, data);
  }

  logout(): void {
    this.removeToken();
    this.userService.clearUser();
  }

  saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    this.loggedInSubject.next(true);

    // Initialize in-memory state from the signed JWT payload
    const decoded = this.getDecodedToken();
    if (decoded) {
      this.userService.setUser({
        id: decoded.userId,
        role: decoded.role,
        email: decoded.sub
      });
    }
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  removeToken(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.loggedInSubject.next(false);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const decoded = jwtDecode<JwtPayload>(token);
      return decoded.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  getDecodedToken(): JwtPayload | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      return jwtDecode<JwtPayload>(token);
    } catch {
      return null;
    }
  }

  getUserId(): string | null {
    return this.userService.currentUser?.id || this.getDecodedToken()?.userId || null;
  }

  getEmail(): string | null {
    return this.userService.currentUser?.email || this.getDecodedToken()?.sub || null;
  }

  getRole(): string | null {
    return this.userService.currentUser?.role || this.getDecodedToken()?.role || null;
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('token');
  }
}