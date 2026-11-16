<template>
  <router-view />
</template>

<script setup>
// 根组件:仅承载路由出口,布局由各 Layout 组件负责
</script>

<style>
:root {
  --el-color-primary: #245466;
  --el-color-primary-light-3: #668894;
  --el-color-primary-light-5: #92aab2;
  --el-color-primary-light-7: #becdd1;
  --el-color-primary-light-8: #d3dfe1;
  --el-color-primary-light-9: #e8eff0;
  --el-color-primary-dark-2: #183c4a;
  --el-border-radius-base: 4px;
  --el-border-color: #e2e6e5;
  --el-text-color-primary: #21313a;
}
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
  background: #f5f5f2;
  color: #21313a;
  -webkit-font-smoothing: antialiased;
}
.page-card {
  background: #fff;
  border: 1px solid #e5e9e8;
  border-radius: 4px;
  padding: 28px;
  box-shadow: 0 12px 30px rgba(21, 47, 57, 0.035);
}
.page-title {
  font-family: Georgia, 'Songti SC', SimSun, serif;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.02em;
  margin-bottom: 22px;
  color: #193544;
}
.el-card {
  --el-card-border-color: #e3e8e6;
  border-radius: 4px;
}
.el-button { font-weight: 600; }
.el-tag { border-radius: 2px; }
.el-table { --el-table-border-color: #e8ecea; }
.el-table th.el-table__cell { background: #f5f7f6; color: #455a64; font-weight: 700; }
.el-input__wrapper, .el-textarea__inner { box-shadow: 0 0 0 1px #dfe5e3 inset; }
.el-input__wrapper:hover, .el-textarea__inner:hover { box-shadow: 0 0 0 1px #9eb5bb inset; }
.el-input__wrapper.is-focus, .el-textarea__inner:focus { box-shadow: 0 0 0 1px #245466 inset; }
.el-dialog { border-radius: 6px; }
.el-drawer__header { border-bottom: 1px solid #e8ecea; padding-bottom: 18px; }
.el-drawer__title { font-weight: 700; color: #193544; }
.el-empty__description p { color: #819097; }
::selection { background: #cfdee0; color: #183c4a; }
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { scroll-behavior: auto !important; transition-duration: 0.01ms !important; }
}
@media (max-width: 600px) {
  .page-card { padding: 20px 18px; }
  .page-title { font-size: 23px; }
  .el-dialog, .el-message-box { max-width: calc(100vw - 24px); }
  .el-drawer { max-width: 100vw; }
}
</style>
