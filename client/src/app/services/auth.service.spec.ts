import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService (Jasmine / Karma)', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should authenticate user and store token', () => {
    const credentials = { email: 'louhab@gmail.com', password: 'password123' };
    const mockAuthResponse = { token: 'mocked.valid.jwt.token', role: 'SELLER' };

    spyOn(localStorage, 'setItem');

    service.login(credentials).subscribe((res) => {
      expect(res.token).toEqual('mocked.valid.jwt.token');
      expect(res.role).toEqual('SELLER');
    });

    // Matches the relative endpoint that AuthService actually calls
    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(credentials);

    req.flush(mockAuthResponse);
  });

  it('should return 401 Error when credentials are bad', () => {
    const badCredentials = { email: 'wrong@gmail.com', password: 'bad321' };

    service.login(badCredentials).subscribe({
      next: () => fail('Expected login to fail with 401'),
      error: (error) => {
        expect(error.status).toBe(401);
      }
    });

    // Matches the relative endpoint that AuthService actually calls
    const req = httpMock.expectOne('/api/auth/login');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });
});