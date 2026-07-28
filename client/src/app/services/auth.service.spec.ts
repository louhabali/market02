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


  ///////////
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


  it('should call localStorage.setItem with token upon successful login', () => {
    const credentials = { email: 'louhab@gmail.com', password: 'password123' };
    const mockAuthResponse = { token: 'mocked.valid.jwt.token' };

    spyOn(localStorage, 'setItem');

    service.login(credentials).subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    req.flush(mockAuthResponse);

    expect(localStorage.setItem).toHaveBeenCalledWith('token', 'mocked.valid.jwt.token');
  });


  it('should retrieve token from localStorage when getToken is called', () => {
    spyOn(localStorage, 'getItem').and.returnValue('stored.jwt.token');

    const token = service.getToken();

    expect(localStorage.getItem).toHaveBeenCalledWith('token');
    expect(token).toBe('stored.jwt.token');
  });


  it('should clear token from localStorage on logout', () => {
    spyOn(localStorage, 'removeItem');

    service.logout();

    expect(localStorage.removeItem).toHaveBeenCalledWith('token');
  });


  it('should send a POST request to register a new user', () => {
    const newUser = { username: 'newwww', email: 'new@gmail.com', password: 'password123', role: 'CLIENT' };
    const mockRegisterResponse = { message: 'User registered successfully' };

    service.register(newUser).subscribe((res) => {
      expect(res.message).toBe('User registered successfully');
    });

    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newUser);

    req.flush(mockRegisterResponse);
  });

  it('should return true for isLoggedIn when a valid unexpired token exists', () => {
    // A mock JWT token whose 'exp' claim is set far in the future
    const unexpiredJwt = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjI1MzQwMjMwMDAwMH0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c';

    // Return the mock JWT when getToken() is called by isLoggedIn()
    spyOn(service, 'getToken').and.returnValue(unexpiredJwt);

    const loggedIn = service.isLoggedIn();

    expect(loggedIn).toBeTrue();
  });
});