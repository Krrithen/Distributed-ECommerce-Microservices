<template>
  <div class="products-page">
    <!-- Header Section -->
    <div class="products-header">
      <h1>Products</h1>
      <p>Results come from GET /api/search (search-service, Elasticsearch)</p>
    </div>

    <!-- Search and Sort Section -->
    <div class="search-filter-section">
      <div class="search-container">
        <form class="search-input-wrapper" @submit.prevent="performSearch">
          <input
            v-model="searchinput"
            type="text"
            placeholder="Search products..."
            class="search-input"
          />
          <button type="submit" class="search-button">Search</button>
        </form>
      </div>

      <div class="filter-section">
        <div class="filter-group">
          <label>Sort by:</label>
          <select v-model="sortBy" class="filter-select">
            <option value="name">Name</option>
            <option value="price-low">Price: Low to High</option>
            <option value="price-high">Price: High to Low</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Products Grid -->
    <div class="products-container">
      <div v-if="isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>Loading products...</p>
      </div>

      <div v-else-if="getError" class="error-state">
        <div class="error-icon">!</div>
        <h3>Search request failed</h3>
        <p>{{ getError }}</p>
        <button @click="performSearch" class="retry-button">Try Again</button>
      </div>

      <div v-else-if="sortedProducts.length === 0" class="no-products">
        <div class="no-products-icon">?</div>
        <h3>No products found</h3>
        <p>Try a different search term</p>
        <button @click="clearSearch" class="clear-filters-button">
          Clear Search
        </button>
      </div>

      <div v-else class="products-grid">
        <div
          v-for="product in sortedProducts"
          :key="product.id"
          class="product-card"
          @click="productSelected(product)"
        >
          <div class="product-info">
            <h3 class="product-name">{{ product.name }}</h3>
            <p class="product-description">{{ product.description }}</p>

            <div class="product-footer">
              <span class="product-price">${{ product.price }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { mapActions, mapGetters } from "vuex";

export default {
  name: "DemoProducts",
  data() {
    return {
      searchinput: "",
      sortBy: "name",
    };
  },
  computed: {
    ...mapGetters(["getProductsList", "isLoading", "getError"]),
    // Filtering happens server-side in the search call; only sort here.
    sortedProducts() {
      const products = [...this.getProductsList];

      switch (this.sortBy) {
        case "name":
          products.sort((a, b) => (a.name || "").localeCompare(b.name || ""));
          break;
        case "price-low":
          products.sort((a, b) => a.price - b.price);
          break;
        case "price-high":
          products.sort((a, b) => b.price - a.price);
          break;
      }

      return products;
    },
  },
  methods: {
    ...mapActions(["searchProductsApi"]),
    productSelected(product) {
      this.$router.push(`/product/${product.id}`);
    },
    performSearch() {
      this.searchProductsApi(this.searchinput.trim()).catch(() => {
        // Error message is stored in Vuex and rendered by the template
      });
    },
    clearSearch() {
      this.searchinput = "";
      this.performSearch();
    },
  },
  created() {
    this.performSearch();
  },
};
</script>
<style scoped>
/* Main Container */
.products-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

/* Header Section */
.products-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.products-header h1 {
  font-size: 2.5rem;
  margin-bottom: 10px;
  font-weight: 700;
}

.products-header p {
  font-size: 1.1rem;
  opacity: 0.9;
}

/* Search and Filter Section */
.search-filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  gap: 20px;
  flex-wrap: wrap;
}

.search-container {
  flex: 1;
  min-width: 300px;
}

.search-input-wrapper {
  display: flex;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  border: none;
  outline: none;
  font-size: 16px;
}

.search-input::placeholder {
  color: #999;
}

.search-button {
  padding: 12px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  cursor: pointer;
  font-weight: 600;
  transition: opacity 0.3s;
}

.search-button:hover {
  opacity: 0.9;
}

.filter-section {
  display: flex;
  gap: 20px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-group label {
  font-weight: 600;
  color: #2c3e50;
}

.filter-select {
  padding: 8px 12px;
  border: 2px solid #e1e8ed;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  outline: none;
  transition: border-color 0.3s;
}

.filter-select:focus {
  border-color: #667eea;
}

/* Products Container */
.products-container {
  margin-bottom: 40px;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-state p {
  color: #666;
  font-size: 18px;
}

/* Error State */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  background: #fdf2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.error-state h3 {
  color: #dc2626;
  margin-bottom: 10px;
}

.error-state p {
  color: #666;
  margin-bottom: 20px;
}

.retry-button {
  padding: 12px 24px;
  background: #dc2626;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: background-color 0.3s;
}

.retry-button:hover {
  background: #b91c1c;
}

/* No Products State */
.no-products {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.no-products-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.no-products h3 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.no-products p {
  color: #666;
  margin-bottom: 20px;
}

.clear-filters-button {
  padding: 12px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: background-color 0.3s;
}

.clear-filters-button:hover {
  background: #5a67d8;
}

/* Products Grid */
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

/* Product Card */
.product-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

/* Product Info */
.product-info {
  padding: 20px;
}

.product-name {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
  line-height: 1.3;
}

.product-description {
  color: #666;
  font-size: 0.9rem;
  line-height: 1.4;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  font-size: 1.3rem;
  font-weight: 700;
  color: #2c3e50;
}

/* Responsive Design */
@media (max-width: 768px) {
  .products-page {
    padding: 10px;
  }

  .products-header h1 {
    font-size: 2rem;
  }

  .search-filter-section {
    flex-direction: column;
    align-items: stretch;
  }

  .search-container {
    min-width: auto;
  }

  .filter-section {
    justify-content: center;
  }

  .products-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 480px) {
  .products-grid {
    grid-template-columns: 1fr;
  }
}
</style>
