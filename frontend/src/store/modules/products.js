import API_CONFIG from "@/config/api";
import axios from "axios";

// Create axios instance with base configuration
const apiClient = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

const toErrorMessage = (err) =>
  err.response?.data?.message || err.message || "An error occurred";

export default {
  state: {
    productsList: [],
    loading: false,
    error: null,
    product: null,
    productLoading: false,
    productError: null,
  },
  getters: {
    getProductsList: (state) => state.productsList,
    isLoading: (state) => state.loading,
    getError: (state) => state.error,
    getProduct: (state) => state.product,
    isProductLoading: (state) => state.productLoading,
    getProductError: (state) => state.productError,
  },
  mutations: {
    setProductsList: (state, value) => (state.productsList = value),
    setLoading: (state, value) => (state.loading = value),
    setError: (state, value) => (state.error = value),
    setProduct: (state, value) => (state.product = value),
    setProductLoading: (state, value) => (state.productLoading = value),
    setProductError: (state, value) => (state.productError = value),
  },
  actions: {
    // GET /api/search?q= (search-service, backed by Elasticsearch)
    searchProductsApi: ({ commit }, query = "") => {
      commit("setLoading", true);
      commit("setError", null);

      return apiClient
        .get(API_CONFIG.ENDPOINTS.SEARCH, { params: { q: query } })
        .then((response) => {
          commit("setProductsList", response.data);
          return response.data;
        })
        .catch((err) => {
          commit("setProductsList", []);
          commit("setError", toErrorMessage(err));
          throw err;
        })
        .finally(() => commit("setLoading", false));
    },
    // GET /api/products/{id} (catalog-service, backed by MongoDB)
    fetchProductApi: ({ commit }, productId) => {
      commit("setProductLoading", true);
      commit("setProductError", null);
      commit("setProduct", null);

      return apiClient
        .get(API_CONFIG.ENDPOINTS.PRODUCT(productId))
        .then((response) => {
          commit("setProduct", response.data);
          return response.data;
        })
        .catch((err) => {
          commit("setProductError", toErrorMessage(err));
          throw err;
        })
        .finally(() => commit("setProductLoading", false));
    },
  },
};
