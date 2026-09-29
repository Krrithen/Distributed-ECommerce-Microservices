// API Configuration: only endpoints the API gateway actually exposes
const API_CONFIG = {
  BASE_URL: process.env.VUE_APP_API_BASE_URL || "http://localhost:8081",
  ENDPOINTS: {
    SEARCH: "/api/search",
    PRODUCT: (id) => `/api/products/${encodeURIComponent(id)}`,
  },
};

export default API_CONFIG;
