import { toast } from 'react-toastify';

// Error types
export const ERROR_TYPES = {
  NETWORK: 'NETWORK_ERROR',
  VALIDATION: 'VALIDATION_ERROR',
  AUTHENTICATION: 'AUTH_ERROR',
  AUTHORIZATION: 'AUTHORIZATION_ERROR',
  SERVER: 'SERVER_ERROR',
  UNKNOWN: 'UNKNOWN_ERROR'
};

// Error messages in Vietnamese
const ERROR_MESSAGES = {
  [ERROR_TYPES.NETWORK]: 'Lỗi kết nối mạng. Vui lòng kiểm tra kết nối internet.',
  [ERROR_TYPES.VALIDATION]: 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.',
  [ERROR_TYPES.AUTHENTICATION]: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  [ERROR_TYPES.AUTHORIZATION]: 'Bạn không có quyền thực hiện thao tác này.',
  [ERROR_TYPES.SERVER]: 'Lỗi máy chủ. Vui lòng thử lại sau.',
  [ERROR_TYPES.UNKNOWN]: 'Đã xảy ra lỗi không xác định. Vui lòng thử lại.'
};

// Determine error type based on status code or error message
export const getErrorType = (error) => {
  if (!error) return ERROR_TYPES.UNKNOWN;

  // Network errors
  if (error.name === 'TypeError' && error.message.includes('fetch')) {
    return ERROR_TYPES.NETWORK;
  }

  // HTTP status codes
  if (error.status) {
    switch (error.status) {
      case 400:
        return ERROR_TYPES.VALIDATION;
      case 401:
        return ERROR_TYPES.AUTHENTICATION;
      case 403:
        return ERROR_TYPES.AUTHORIZATION;
      case 500:
      case 502:
      case 503:
      case 504:
        return ERROR_TYPES.SERVER;
      default:
        return ERROR_TYPES.UNKNOWN;
    }
  }

  // Parse error message
  const message = error.message?.toLowerCase() || '';
  if (message.includes('network') || message.includes('fetch')) {
    return ERROR_TYPES.NETWORK;
  }
  if (message.includes('validation') || message.includes('invalid')) {
    return ERROR_TYPES.VALIDATION;
  }
  if (message.includes('unauthorized') || message.includes('token')) {
    return ERROR_TYPES.AUTHENTICATION;
  }
  if (message.includes('forbidden') || message.includes('permission')) {
    return ERROR_TYPES.AUTHORIZATION;
  }

  return ERROR_TYPES.UNKNOWN;
};

// Get user-friendly error message
export const getErrorMessage = (error) => {
  const errorType = getErrorType(error);
  return ERROR_MESSAGES[errorType] || ERROR_MESSAGES[ERROR_TYPES.UNKNOWN];
};

// Handle API errors with toast notifications
export const handleApiError = (error, customMessage = null) => {
  console.error('API Error:', error);

  const errorType = getErrorType(error);
  const message = customMessage || getErrorMessage(error);

  // Show toast notification
  switch (errorType) {
    case ERROR_TYPES.NETWORK:
      toast.error(message, {
        toastId: 'network-error', // Prevent duplicate toasts
        autoClose: 5000
      });
      break;
    case ERROR_TYPES.AUTHENTICATION:
      toast.error(message, {
        toastId: 'auth-error',
        autoClose: 3000
      });
      // Redirect to login page after a delay
      setTimeout(() => {
        window.location.href = '/login';
      }, 3000);
      break;
    case ERROR_TYPES.AUTHORIZATION:
      toast.warn(message, {
        toastId: 'auth-error',
        autoClose: 4000
      });
      break;
    case ERROR_TYPES.VALIDATION:
      toast.warn(message, {
        toastId: 'validation-error',
        autoClose: 4000
      });
      break;
    case ERROR_TYPES.SERVER:
      toast.error(message, {
        toastId: 'server-error',
        autoClose: 5000
      });
      break;
    default:
      toast.error(message, {
        toastId: 'unknown-error',
        autoClose: 4000
      });
  }

  return { errorType, message };
};

// Retry mechanism for failed requests
export const retryRequest = async (requestFn, maxRetries = 3, delay = 1000) => {
  let lastError;

  for (let i = 0; i < maxRetries; i++) {
    try {
      return await requestFn();
    } catch (error) {
      lastError = error;
      
      // Don't retry for certain error types
      const errorType = getErrorType(error);
      if ([ERROR_TYPES.AUTHENTICATION, ERROR_TYPES.AUTHORIZATION, ERROR_TYPES.VALIDATION].includes(errorType)) {
        throw error;
      }

      // Wait before retrying
      if (i < maxRetries - 1) {
        await new Promise(resolve => setTimeout(resolve, delay * Math.pow(2, i))); // Exponential backoff
      }
    }
  }

  throw lastError;
};

// Global error boundary handler
export const globalErrorHandler = (error, errorInfo) => {
  console.error('Global Error:', error, errorInfo);
  
  // Log to external service in production
  if (process.env.NODE_ENV === 'production') {
    // Send to logging service (e.g., Sentry, LogRocket)
    // logErrorToService(error, errorInfo);
  }

  // Show user-friendly error message
  toast.error('Đã xảy ra lỗi không mong muốn. Trang web sẽ được tải lại.', {
    toastId: 'global-error',
    autoClose: 5000,
    onClose: () => {
      // Reload page after error
      window.location.reload();
    }
  });
};

// Validation helpers
export const validateRequired = (value, fieldName) => {
  if (!value || (typeof value === 'string' && value.trim() === '')) {
    throw new Error(`${fieldName} là bắt buộc`);
  }
};

export const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    throw new Error('Email không hợp lệ');
  }
};

export const validatePhone = (phone) => {
  const phoneRegex = /^[0-9]{10,11}$/;
  if (!phoneRegex.test(phone)) {
    throw new Error('Số điện thoại không hợp lệ');
  }
};

export const validatePrice = (price) => {
  if (isNaN(price) || price <= 0) {
    throw new Error('Giá phải là số dương');
  }
};

// Performance monitoring
export const measurePerformance = (name, fn) => {
  return async (...args) => {
    const start = performance.now();
    try {
      const result = await fn(...args);
      const end = performance.now();
      console.log(`${name} took ${end - start} milliseconds`);
      return result;
    } catch (error) {
      const end = performance.now();
      console.log(`${name} failed after ${end - start} milliseconds`);
      throw error;
    }
  };
};