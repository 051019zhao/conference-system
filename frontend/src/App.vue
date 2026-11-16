<template>
  <router-view />
</template>

<script setup>
// 根组件:仅承载路由出口,布局由各 Layout 组件负责
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
  background: #f5f7fa;
  color: #303133;
}
.page-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 16px;
}
</style>
