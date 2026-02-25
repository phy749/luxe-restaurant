import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';

// Custom hook for API calls with caching and error handling
export const useApi = (url, options = {}) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { currentUser } = useAuth();

  const {
    method = 'GET',
    body = null,
    headers = {},
    cache = true,
    cacheTime = 5 * 60 * 1000, // 5 minutes default
    dependencies = []
  } = options;

  // Create cache key
  const cacheKey = `api_${url}_${method}_${JSON.stringify(body)}`;

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Check cache first
      if (cache && method === 'GET') {
        const cached = localStorage.getItem(cacheKey);
        if (cached) {
          const { data: cachedData, timestamp } = JSON.parse(cached);
          if (Date.now() - timestamp < cacheTime) {
            setData(cachedData);
            setLoading(false);
            return;
          }
        }
      }

      const config = {
        method,
        headers: {
          'Content-Type': 'application/json',
          ...headers
        }
      };

      // Add auth token if user is logged in
      if (currentUser?.token) {
        config.headers.Authorization = `Bearer ${currentUser.token}`;
      }

      // Add body for non-GET requests
      if (body && method !== 'GET') {
        config.body = JSON.stringify(body);
      }

      const response = await fetch(`${import.meta.env.VITE_API_URL}${url}`, config);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      setData(result);

      // Cache GET requests
      if (cache && method === 'GET') {
        localStorage.setItem(cacheKey, JSON.stringify({
          data: result,
          timestamp: Date.now()
        }));
      }

    } catch (err) {
      setError(err.message);
      console.error('API Error:', err);
    } finally {
      setLoading(false);
    }
  }, [url, method, body, currentUser?.token, cacheKey, cache, cacheTime, ...dependencies]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const refetch = useCallback(() => {
    // Clear cache before refetching
    if (cache) {
      localStorage.removeItem(cacheKey);
    }
    fetchData();
  }, [fetchData, cache, cacheKey]);

  const mutate = useCallback(async (newData) => {
    setData(newData);
    // Update cache
    if (cache) {
      localStorage.setItem(cacheKey, JSON.stringify({
        data: newData,
        timestamp: Date.now()
      }));
    }
  }, [cache, cacheKey]);

  return { data, loading, error, refetch, mutate };
};

// Specialized hooks for common operations
export const useDishes = () => {
  return useApi('/api/dish/getall', {
    cache: true,
    cacheTime: 10 * 60 * 1000 // 10 minutes for menu items
  });
};

export const useCategories = () => {
  return useApi('/api/category/getall', {
    cache: true,
    cacheTime: 30 * 60 * 1000 // 30 minutes for categories
  });
};

export const useOrders = (userId) => {
  return useApi(`/api/orders/findOrder/${userId}`, {
    cache: true,
    cacheTime: 2 * 60 * 1000, // 2 minutes for orders
    dependencies: [userId]
  });
};

export const usePromotions = () => {
  return useApi('/api/promotion/getall', {
    cache: true,
    cacheTime: 15 * 60 * 1000 // 15 minutes for promotions
  });
};

// Hook for mutations (POST, PUT, DELETE)
export const useMutation = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const { currentUser } = useAuth();

  const mutate = useCallback(async (url, options = {}) => {
    try {
      setLoading(true);
      setError(null);

      const config = {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...options.headers
        },
        ...options
      };

      if (currentUser?.token) {
        config.headers.Authorization = `Bearer ${currentUser.token}`;
      }

      if (options.body) {
        config.body = JSON.stringify(options.body);
      }

      const response = await fetch(`${import.meta.env.VITE_API_URL}${url}`, config);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      
      // Clear related caches after mutation
      if (options.invalidateCache) {
        options.invalidateCache.forEach(cachePattern => {
          Object.keys(localStorage).forEach(key => {
            if (key.includes(cachePattern)) {
              localStorage.removeItem(key);
            }
          });
        });
      }

      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [currentUser?.token]);

  return { mutate, loading, error };
};

// Cache management utilities
export const clearApiCache = (pattern = '') => {
  Object.keys(localStorage).forEach(key => {
    if (key.startsWith('api_') && key.includes(pattern)) {
      localStorage.removeItem(key);
    }
  });
};

export const clearAllApiCache = () => {
  Object.keys(localStorage).forEach(key => {
    if (key.startsWith('api_')) {
      localStorage.removeItem(key);
    }
  });
};