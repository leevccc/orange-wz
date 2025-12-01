<template>
  <el-dialog v-model="visible" title="打开" width="500">
    <el-button type="success" @click="reloadClick" style="margin: 10px 0">刷新缓存</el-button>
    <el-breadcrumb separator="/" style="margin: 10px 0">
      <el-breadcrumb-item v-for="item in nav" :key="item.id">
        <a @click="navClick(item.id)">{{ item.name }}</a>
      </el-breadcrumb-item>
    </el-breadcrumb>
    <el-table :data="tableData" height="50vh">
      <el-table-column label="文件">
        <template #default="scope">
          <div class="file-item">
            <FcFolder width="20" height="20" v-if="scope.row.type == 'FOLDER'" class="file-icon" />
            <BsFileEarmarkWord width="20" height="20" v-else class="file-icon" />
            {{ scope.row.name }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" :width="160" align="center">
        <template #default="scope">
          <el-button
            v-show="scope.row.type == 'FOLDER'"
            type="success"
            @click="getFolderClick(scope.row)"
            size="small"
          >
            查看
          </el-button>
          <el-button type="primary" @click="loadClick(scope.row)" size="small">载入</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
  <WzKey ref="WzKeyRef" @confirmWzKey="confirmWzKey" />
</template>

<script setup lang="ts">
  import { getFolder, reloadFolder } from '@/api/wz.ts';
  import type { IWzNode } from '@/store/wzEditor.ts';
  import WzKey from '@/views/wz/WzKey.vue';
  import { ref, useTemplateRef } from 'vue';
  import { BsFileEarmarkWord } from 'vue-icons-plus/bs';
  import { FcFolder } from 'vue-icons-plus/fc';

  const pFunc = defineEmits(['loadConfirm']);

  const visible = ref(false);
  const nav = ref<IWzNode[]>([{ name: 'root', id: -1, type: 'FOLDER', children: [], leaf: true }]);
  const tableData = ref<IWzNode[]>([]);
  const openReady = ref<IWzNode>();
  const wzKeyRef = useTemplateRef('WzKeyRef');

  const initial = () => {
    navClick(-1);
    visible.value = true;
  };

  const loadTableData = async (id: number) => {
    const { data } = await getFolder(id);
    tableData.value = data;
  };

  const navClick = (id: number) => {
    loadTableData(id);
    removeNav(nav.value, id);
  };

  const removeNav = (arr: IWzNode[], targetValue: number) => {
    const index = arr.findIndex((item) => item.id === targetValue);
    if (index !== -1) arr.splice(index + 1); // 从 index+1 开始删除后面的所有元素

    return arr;
  };

  const getFolderClick = (row: IWzNode) => {
    loadTableData(row.id);
    nav.value.push(row);
  };

  const loadClick = async (row: IWzNode) => {
    openReady.value = row;
    wzKeyRef.value.initial();
  };

  const reloadClick = async () => {
    await reloadFolder();
    navClick(-1);
    ElMessage.success({ message: '数据已更新' });
  };

  const confirmWzKey = async (version: number, key: string) => {
    if (openReady.value) {
      pFunc('loadConfirm', openReady.value.id, version, key);
      visible.value = false;
    }
  };

  defineExpose({ initial });
</script>

<style scoped></style>
