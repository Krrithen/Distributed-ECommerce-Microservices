import DemoProducts from "@/components/DemoProducts.vue";
import Vue from "vue";
import VueRouter from "vue-router";
import HomeView from "../views/HomeView.vue";
import ProductDetailView from "../views/ProductDetailView.vue";

Vue.use(VueRouter);

// Demo UI: only routes backed by real gateway endpoints.
const routes = [
  {
    path: "/",
    name: "home",
    component: HomeView,
  },
  {
    path: "/products",
    name: "products",
    component: DemoProducts,
  },
  {
    path: "/product/:productId",
    name: "productDetail",
    component: ProductDetailView,
  },
];

const router = new VueRouter({
  mode: "history",
  base: process.env.BASE_URL,
  routes,
});

export default router;
