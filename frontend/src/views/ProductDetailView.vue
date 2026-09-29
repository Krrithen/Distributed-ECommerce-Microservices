<template>
  <div class="product-detail-view">
    <router-link to="/products" class="back-link">&larr; Back to products</router-link>

    <p v-if="isProductLoading" class="status">Loading product...</p>

    <div v-else-if="getProductError" class="error-state">
      <h2>Product not available</h2>
      <p>{{ getProductError }}</p>
    </div>

    <div v-else-if="getProduct" class="product-card">
      <h1>{{ getProduct.name }}</h1>
      <p class="price">${{ getProduct.price }}</p>
      <p class="quantity">{{ getProduct.quantity }} in stock</p>
      <p class="description">{{ getProduct.description }}</p>

      <table v-if="hasAttributes" class="attributes">
        <tr v-for="(value, key) in getProduct.attributes" :key="key">
          <th>{{ key }}</th>
          <td>{{ value }}</td>
        </tr>
      </table>

      <p class="source">Loaded from GET /api/products/{{ getProduct.id }} (catalog-service, MongoDB)</p>
    </div>
  </div>
</template>
<script>
import { mapActions, mapGetters } from "vuex";

export default {
  name: "ProductDetailView",
  computed: {
    ...mapGetters(["getProduct", "isProductLoading", "getProductError"]),
    hasAttributes() {
      const attrs = this.getProduct && this.getProduct.attributes;
      return attrs && Object.keys(attrs).length > 0;
    },
  },
  methods: {
    ...mapActions(["fetchProductApi"]),
    load() {
      this.fetchProductApi(this.$route.params.productId).catch(() => {
        // Error message is stored in Vuex and rendered by the template
      });
    },
  },
  watch: {
    "$route.params.productId": "load",
  },
  created() {
    this.load();
  },
};
</script>
<style scoped>
.product-detail-view {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.back-link {
  display: inline-block;
  margin-bottom: 20px;
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
}

.status {
  color: #666;
}

.product-card,
.error-state {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  padding: 30px;
}

.error-state h2 {
  color: #dc2626;
  margin-bottom: 10px;
}

h1 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.price {
  font-size: 1.5rem;
  font-weight: 700;
  color: #2c3e50;
}

.quantity {
  color: #666;
  margin-bottom: 20px;
}

.description {
  color: #444;
  line-height: 1.5;
  margin-bottom: 20px;
}

.attributes {
  border-collapse: collapse;
  margin-bottom: 20px;
}

.attributes th,
.attributes td {
  text-align: left;
  padding: 6px 12px;
  border-bottom: 1px solid #eee;
}

.attributes th {
  color: #2c3e50;
}

.source {
  color: #999;
  font-size: 0.85rem;
}
</style>
