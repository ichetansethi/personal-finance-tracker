# Frontend OTP Integration

## Overview
The login page has been updated to support OTP (One-Time Password) verification. The frontend automatically adapts based on whether OTP is enabled on the backend.

## Changes Made

### 1. Updated Login Page (`login.html`)

#### New Features:
- **Two-Step Login Flow**:
  - Step 1: Username and password
  - Step 2: OTP verification (if enabled)

- **Dynamic Form Switching**: Automatically shows OTP form when backend requires it

- **Enhanced UX**:
  - Email icon and message when OTP is sent
  - Large, centered OTP input field
  - Enter key support on both login and OTP forms
  - Back to login button for easy navigation

### 2. Updated Styles (`styles.css`)

#### New Styles:
- `.otp-input`: Special styling for OTP input field (centered, large text, monospace font)
- `.otp-message`: Animated message container for OTP instructions

## How It Works

### When OTP is Disabled (`otp.enabled=false`)

1. User enters username and password
2. Clicks "Log in" button
3. Backend returns: `{"token": "...", "otpRequired": false}`
4. User is immediately redirected to dashboard

**Flow Diagram:**
```
Login Form → Backend Validation → JWT Token → Dashboard
```

### When OTP is Enabled (`otp.enabled=true`)

1. User enters username and password
2. Clicks "Log in" button
3. Backend returns: `{"otpRequired": true, "message": "OTP has been sent to your email"}`
4. Frontend switches to OTP form
5. User checks email and enters 6-digit code
6. Clicks "Verify Code" button
7. Backend validates OTP and returns JWT token
8. User is redirected to dashboard

**Flow Diagram:**
```
Login Form → Backend Validation → OTP Sent → OTP Form → OTP Verification → JWT Token → Dashboard
```

## API Integration

### Login Endpoint
```javascript
POST /api/auth/login
Request: { "username": "user", "password": "pass" }

Response (OTP Disabled):
{ "token": "jwt-token", "otpRequired": false }

Response (OTP Enabled):
{ "otpRequired": true, "message": "OTP has been sent to your email" }
```

### OTP Verification Endpoint
```javascript
POST /api/auth/verify-otp
Request: { "username": "user", "otp": "123456" }

Response:
{ "token": "jwt-token" }
```

## User Experience Features

### 1. Visual Feedback
- Email icon appears when OTP is requested
- Clear message: "We've sent a verification code to your email"
- Error messages for invalid or expired OTPs

### 2. Form Validation
- OTP must be exactly 6 digits
- Only numeric input allowed
- Real-time validation feedback

### 3. Keyboard Support
- Press Enter on username/password fields to submit login
- Press Enter on OTP field to submit verification

### 4. Navigation
- "Back to Login" button to return from OTP form
- Form state is reset when navigating back

### 5. Auto-Focus
- OTP input field automatically receives focus when OTP form is shown

## Error Handling

The frontend handles various error scenarios:

1. **Invalid Credentials**: "❌ Invalid username or password"
2. **Invalid OTP**: "❌ Invalid or expired verification code"
3. **Missing OTP**: "❌ Please enter a valid 6-digit code"
4. **Network Error**: "⚠️ Could not reach server"
5. **Server Error**: "⚠️ Unexpected response from server"

## Testing

### Test Case 1: OTP Disabled
1. Set `otp.enabled=false` in backend
2. Login with valid credentials
3. Should immediately redirect to dashboard

### Test Case 2: OTP Enabled
1. Set `otp.enabled=true` in backend
2. Login with valid credentials
3. Should see OTP form
4. Enter OTP from email
5. Should redirect to dashboard

### Test Case 3: Invalid OTP
1. Login with valid credentials
2. Enter wrong OTP
3. Should see error message
4. Can try again with correct OTP

### Test Case 4: Back Navigation
1. Login with valid credentials
2. Click "Back to Login" on OTP form
3. Should return to login form
4. Form should be reset

## Browser Compatibility

The implementation uses standard HTML5 and ES6+ JavaScript features:
- ✅ Chrome/Edge (latest)
- ✅ Firefox (latest)
- ✅ Safari (latest)
- ✅ Mobile browsers (iOS Safari, Chrome Mobile)

## Responsive Design

The OTP form maintains the same responsive design as the login form:
- Mobile-friendly input
- Proper scaling on all screen sizes
- Touch-friendly buttons

## Security Considerations

1. **OTP is never stored in frontend**: Only kept in memory during verification
2. **Username is stored temporarily**: Cleared when returning to login or after successful verification
3. **Token is stored in localStorage**: Standard JWT storage for session management
4. **HTTPS recommended**: All sensitive data should be transmitted over HTTPS in production

## Future Enhancements (Optional)

Possible improvements for future versions:
- Resend OTP button
- Countdown timer showing OTP expiry
- Auto-submit when 6 digits are entered
- Biometric authentication option
- Remember device feature
