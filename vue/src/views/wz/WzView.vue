<template>
  <el-splitter>
    <el-splitter-panel :size="430" style="height: calc(100vh - 292px)" collapsible>
      <el-card shadow="hover">
        <div style="display: flex; align-items: center; width: 100%">
          视图 {{ viewId }}
          <el-button style="margin-left: auto" type="danger" size="small" @click="closeClick">
            关闭
          </el-button>
        </div>

        <el-auto-resizer style="height: calc(100vh - 370px); margin-top: 12px">
          <template #default="{ height }">
            <el-input
              v-model="filterQuery"
              placeholder="过滤"
              size="small"
              @input="filterClick"
              clearable
            />
            <el-tree-v2
              ref="TreeRef"
              node-key="id"
              class="hide-expand-icon"
              :data="treeData"
              :height="height - 24"
              :default-expanded-keys="expandedKeys"
              :expand-on-click-node="false"
              onselectstart="return false;"
              @node-click="handleNodeClick"
              @node-expand="handleNodeExpand"
              @node-collapse="handleNodeCollapse"
              @node-contextmenu="contextMenu"
            >
              <template #default="{ data }">
                <div
                  :class="
                    checkedKey.includes(data.id) ? 'custom-tree-node-selected' : 'custom-tree-node'
                  "
                >
                  <span v-if="!data.leaf" class="file-item">
                    <AiOutlineMinusSquare
                      class="file-icon"
                      width="16px"
                      height="16px"
                      v-if="expandedKeys.includes(data.id)"
                    />
                    <AiOutlinePlusSquare class="file-icon" width="16px" height="16px" v-else />
                    {{ data.name }}
                  </span>
                  <span v-else style="margin-left: 19px">
                    {{ data.name }}
                  </span>
                  <el-tag type="info">{{ getMiniType(data.type) }}</el-tag>
                </div>
              </template>
            </el-tree-v2>
          </template>
        </el-auto-resizer>
      </el-card>
    </el-splitter-panel>
    <el-splitter-panel style="height: calc(100vh - 292px)" collapsible>
      <el-card shadow="hover" style="height: calc(100vh - 294px); background: rgb(229, 233, 242)">
        <el-form :label-width="40">
          <el-form-item label="名称">
            <el-input v-model="editFormData.name" />
          </el-form-item>
          <el-form-item label="类型">
            <el-input v-model="editFormData.type" disabled />
          </el-form-item>
          <el-form-item label="值" v-show="isNormalValue(editFormData.type)">
            <el-input v-model="editFormData.value" />
          </el-form-item>
          <template v-if="editFormData.type == 'IMAGE_VECTOR'">
            <el-form-item label="X">
              <el-input v-model="editFormData.x" />
            </el-form-item>
            <el-form-item label="Y">
              <el-input v-model="editFormData.y" />
            </el-form-item>
          </template>

          <template v-if="editFormData.type == 'IMAGE_CANVAS' || editFormData.type == 'IMAGE_UOL'">
            <el-form-item label="图片">
              <el-image
                fit="scale-down"
                :preview-src-list="['data:image/png;base64,' + editFormData.png]"
                :src="'data:image/png;base64,' + editFormData.png"
                hide-on-click-modal
              />
            </el-form-item>
            <el-form-item label="宽高">
              <el-tag type="success" disable-transitions>
                {{ editFormData.x }} x {{ editFormData.y }}
              </el-tag>
            </el-form-item>
            <el-form-item label="压缩">
              <el-select
                v-model="editFormData.pngFormat"
                :disabled="editFormData.type == 'IMAGE_UOL'"
              >
                <el-option
                  v-for="item in formatOptions"
                  :key="item.id"
                  :label="item.value"
                  :value="item.value"
                  :disabled="item.disabled"
                >
                  <span style="float: left">{{ item.value }}</span>
                  <span
                    style="float: right; color: var(--el-text-color-secondary); font-size: 13px"
                  >
                    {{ item.label }}
                  </span>
                </el-option>
              </el-select>
            </el-form-item>
          </template>

          <el-form-item label="音频" v-show="editFormData.type == 'IMAGE_SOUND'">
            <audio controls :key="editFormData.mp3" loop>
              <source :src="'data:audio/wav;base64,' + editFormData.mp3" />
            </audio>
          </el-form-item>
          <el-form-item class="saveButtons">
            <el-button type="success" v-show="editFormData.id" @click="saveChangeClick">
              保存修改
            </el-button>
            <el-upload
              v-show="editFormData.type == 'IMAGE_CANVAS'"
              :on-change="pngSelected"
              accept=".png"
              :auto-upload="false"
              :show-file-list="false"
            >
              <el-button type="primary">选择图片</el-button>
            </el-upload>
            <el-button
              v-show="editFormData.type == 'IMAGE_CANVAS'"
              type="primary"
              @click="savePngClick"
            >
              下载图片
            </el-button>

            <el-upload
              v-show="editFormData.type == 'IMAGE_SOUND'"
              :on-change="mp3Selected"
              accept=".mp3"
              :auto-upload="false"
              :show-file-list="false"
            >
              <el-button type="primary">选择音频</el-button>
            </el-upload>
            <el-button
              v-show="editFormData.type == 'IMAGE_SOUND'"
              type="primary"
              @click="saveMp3Click"
            >
              下载音频
            </el-button>
            <el-button
              type="primary"
              v-show="editFormData.type == 'IMAGE' || editFormData.type == 'IMAGE_LIST'"
              @click="getAllCanvasClick"
            >
              图片嗅探
            </el-button>
          </el-form-item>
          <el-form-item v-if="allCanvas != undefined">
            <el-auto-resizer style="height: calc(100vh - 520px); margin-top: 12px">
              <template #default="{ height }">
                <el-scrollbar :height="height">
                  <div class="canvas-list">
                    <template v-for="(item, index) in allCanvas" :key="index">
                      <el-card style="width: 200px">
                        <el-image
                          fit="scale-down"
                          :preview-src-list="['data:image/png;base64,' + item.png]"
                          :src="'data:image/png;base64,' + item.png"
                          hide-on-click-modal
                        />
                        <template #footer>
                          <el-button
                            type="primary"
                            size="small"
                            plain
                            @click="copyCanvasPathClick(item.value)"
                          >
                            {{ item.value }}
                          </el-button>
                        </template>
                      </el-card>
                    </template>
                  </div>
                </el-scrollbar>
              </template>
            </el-auto-resizer>
          </el-form-item>
        </el-form>
      </el-card>
    </el-splitter-panel>
  </el-splitter>
  <WzContextMenu ref="WzContextMenuRef" @handleContextMenu="handleContextMenu" />
  <NewNodeForm ref="NewNodeFormRef" @insertNode="insertNode" />
  <WzKey ref="WzKeyRef" @confirmWzKey="confirmWzKey" />
</template>

<script setup lang="ts">
  import {
    copy,
    deleteNode,
    exportWzFileToImg,
    exportWzFileToXml,
    fixOutlink,
    getAllCanvas,
    getNode,
    getValue,
    moveView,
    packet,
    paste,
    removeView,
    saveNode,
    unload,
    updateValue,
    updateWzKey,
  } from '@/api/wz.ts';
  import {
    getDefaultWzNodeValue,
    getFormatOptions,
    type IWzNode,
    type IWzNodeValue,
  } from '@/store/wzEditor.ts';
  import {
    filterTreeAndCollectIds,
    getBrotherByTwoId,
    getNodeById,
    removeNodeById,
    renameNodeById,
  } from '@/utils/nodeUtils.ts';
  import { getMiniType, isNormalValue } from '@/utils/wzUtils.ts';
  import NewNodeForm from '@/views/wz/NewNodeForm.vue';
  import WzContextMenu from '@/views/wz/WzContextMenu.vue';
  import WzKey from '@/views/wz/WzKey.vue';
  import { nextTick, ref, useTemplateRef } from 'vue';
  import { AiOutlineMinusSquare, AiOutlinePlusSquare } from 'vue-icons-plus/ai';

  const pFunc = defineEmits([
    'getViewsId',
    'reInitial',
    'syncExpand',
    'syncCollapse',
    'syncLoadEditForm',
    'removeView',
    'setCmsId',
    'chineseClick',
  ]);

  const viewId = ref<number>(-1);
  const treeData = ref<IWzNode[]>([]);
  const tree = useTemplateRef('TreeRef');

  /* 初始化和关闭 -----------------------------------------------------------------------------------*/
  const initial = async (id: number) => {
    const { data } = await getNode(id);
    viewId.value = id;
    treeData.value = data;
  };

  const closeClick = async () => {
    let count = 0;
    pFunc('getViewsId', viewId.value, (response) => {
      count = response.length;
    });

    if (count == 0) {
      ElMessage.error({ message: '不要关闭唯一的视图啊喂' });
      return;
    }
    await removeView(viewId.value);
    pFunc('removeView', viewId.value);
  };

  /* 节点 搜索 -------------------------------------------------------------------------------------*/
  const filterQuery = ref('');
  const allTreeData = ref<IWzNode[] | null>(null);
  const filterClick = () => {
    if (filterQuery.value) {
      if (allTreeData.value == null) {
        allTreeData.value = treeData.value;
      }
      const { filteredTree, matchedIds } = filterTreeAndCollectIds(
        allTreeData.value,
        filterQuery.value,
      );
      treeData.value = filteredTree;
      expandedKeys.value = matchedIds;
    } else {
      if (allTreeData.value != null) {
        treeData.value = allTreeData.value;
        expandedKeys.value = [];
      }
    }
  };

  /* 节点 单击/双击事件 ------------------------------------------------------------------------------*/
  const clickTimer = ref(null);
  const lastClickedNode = ref(null);
  const handleNodeClick = async (row, node, event) => {
    // 清除之前的计时器
    if (clickTimer.value) {
      clearTimeout(clickTimer);
    }

    // 如果点击的是同一个节点，则视为双击
    if (lastClickedNode.value && lastClickedNode.value.id === node.id) {
      // 双击逻辑
      // 切换节点的展开/折叠状态
      if (node.expanded) {
        handleNodeCollapse(row);
        treeData.value = [...treeData.value];
        // 同步视图
        const path = findNodePath(treeData.value, row.id);
        pFunc('syncCollapse', viewId.value, path);
      } else {
        if (row.children.length == 0 && !row.leaf) {
          const { data } = await getNode(row.id);
          data.forEach((item: IWzNode) => {
            row.children.push(item);
          });
        }
        if (row.children.length == 0) row.leaf = true;
        if (row.children.length > 0) {
          handleNodeExpand(row);
          treeData.value = [...treeData.value];
        }
        // 同步视图
        const path = findNodePath(treeData.value, row.id);
        pFunc('syncExpand', viewId.value, path);
      }
      await nextTick(() => {
        if (tree.value) {
          tree.value.setExpandedKeys(expandedKeys.value);
        }
      });
      lastClickedNode.value = null;
      return;
    } else {
      // 记录当前点击的节点
      lastClickedNode.value = node;

      handleCheck(row, event);
      // 单击时加载当前节点
      await loadEditForm(row);
      // 同步视图
      const path = findNodePath(treeData.value, row.id);
      pFunc('syncLoadEditForm', viewId.value, path);

      // 设置计时器，300ms后清除记录
      clickTimer.value = setTimeout(() => {
        lastClickedNode.value = null;
      }, 300);
    }
  };

  /* 节点展开/收缩 ---------------------------------------------------------------------------------*/
  const expandedKeys = ref<number[]>([]);

  const handleNodeExpand = (data: IWzNode) => {
    if (!expandedKeys.value.includes(data.id)) {
      expandedKeys.value.push(data.id);
    }

    expandedKeys.value = [...new Set(expandedKeys.value)];
  };

  const handleNodeCollapse = (data: IWzNode) => {
    data.children.forEach((item: IWzNode) => {
      handleNodeCollapse(item);
    });

    expandedKeys.value = expandedKeys.value.filter((value) => value !== data.id);
  };

  /* 复选框 ---------------------------------------------------------------------------------------*/
  const lastChecked = ref<IWzNode>({ id: -520 });
  const checkedKey = ref<number[]>([]);
  const handleCheck = (data: IWzNode, event: KeyboardEvent) => {
    const ctrlKeyDowned = event.ctrlKey;
    const shiftKeyDowned = event.shiftKey;

    if (!ctrlKeyDowned && !shiftKeyDowned) {
      if (checkedKey.value.length == 1 && checkedKey.value[0] == data.id) {
        checkedKey.value = [];
      } else {
        checkedKey.value = [data.id];
      }
    } else if (ctrlKeyDowned) {
      if (checkedKey.value.includes(data.id)) {
        checkedKey.value = checkedKey.value.filter((item) => item !== data.id);
      } else {
        const id1 = data.id;
        const id2 = lastChecked.value.id;
        const brother = getBrotherByTwoId(treeData.value, id1, id2);
        if (brother) {
          checkedKey.value.push(id1);
        } else {
          checkedKey.value = [id1];
        }
      }
    } else {
      const id1 = data.id;
      const id2 = lastChecked.value.id;
      const brother = getBrotherByTwoId(treeData.value, id1, id2);
      if (brother) {
        let endId = 0;
        let startPush = false;
        for (let i = 0; i < brother.length; i++) {
          if (startPush) {
            if (checkedKey.value.includes(brother[i].id))
              checkedKey.value = checkedKey.value.filter((item) => item !== brother[i].id);
            else checkedKey.value.push(brother[i].id);
          } else if (brother[i].id == id1) {
            endId = id2;
            startPush = true;
            checkedKey.value.push(id1);
          } else if (brother[i].id == id2) {
            endId = id1;
            startPush = true;
            checkedKey.value.push(id2);
          }
          if (brother[i].id == endId) break;
        }
      } else {
        if (checkedKey.value.length == 1 && checkedKey.value[0] == data.id) {
          checkedKey.value = [];
        } else {
          checkedKey.value = [data.id];
        }
      }
    }

    lastChecked.value = data;
    checkedKey.value = [...new Set(checkedKey.value)];
  };

  /* 编辑框 ---------------------------------------------------------------------------------------*/
  const editFormData = ref<IWzNodeValue>(getDefaultWzNodeValue());
  const formatOptions = ref(getFormatOptions());
  const allCanvas = ref<IWzNodeValue[]>(undefined);

  const loadEditForm = async (row: IWzNode) => {
    if (editFormData.value.id != row.id) {
      if (row.file) return;

      editFormData.value = getDefaultWzNodeValue();
      allCanvas.value = undefined;

      if (row.type == 'WZ_DIRECTORY' || row.type == 'IMAGE' || row.type == 'IMAGE_LIST') {
        editFormData.value.name = row.name;
        editFormData.value.id = row.id;
        editFormData.value.type = row.type;
      } else {
        const { data } = await getValue(row.id);
        editFormData.value.name = row.name;
        editFormData.value.id = row.id;
        editFormData.value.type = row.type;
        if (row.type == 'IMAGE_VECTOR') {
          editFormData.value.x = data.x;
          editFormData.value.y = data.y;
        } else if (row.type == 'IMAGE_CANVAS') {
          editFormData.value.png = data.png;
          editFormData.value.pngFormat = data.pngFormat;
          editFormData.value.x = data.x;
          editFormData.value.y = data.y;
        } else if (row.type == 'IMAGE_UOL') {
          editFormData.value.value = data.value;
          editFormData.value.png = data.png;
          editFormData.value.pngFormat = data.pngFormat;
          editFormData.value.x = data.x;
          editFormData.value.y = data.y;
        } else if (row.type == 'IMAGE_SOUND') {
          editFormData.value.mp3 = data.mp3;
        } else {
          editFormData.value.value = data.value;
        }
      }
    }
  };

  const pngSelected = async (uploadFile: UploadFile) => {
    try {
      const base64 = await convertToBase64(uploadFile.raw);
      const pureBase64 = base64.split(',')[1];
      editFormData.value.png = pureBase64;

      return pureBase64;
    } catch (error) {
      ElMessage.error({
        message: `图片识别失败:${error}`,
        duration: 3000,
      });
    }
  };

  const mp3Selected = async (uploadFile: UploadFile) => {
    try {
      const base64 = await convertToBase64(uploadFile.raw);
      const pureBase64 = base64.split(',')[1];
      editFormData.value.mp3 = pureBase64;

      return pureBase64;
    } catch (error) {
      ElMessage.error({
        message: `音频识别失败:${error}`,
        duration: 3000,
      });
    }
  };

  const savePngClick = async () => {
    const base64String = editFormData.value.png;

    // 去掉 data:image/png;base64,
    const pureBase64 = base64String.replace(/^data:image\/png;base64,/, '');
    const byteArray = Uint8Array.from(atob(pureBase64), (c) => c.charCodeAt(0));

    const blob = new Blob([byteArray], { type: 'image/png' });
    const filename = `${editFormData.value.name}.png`;

    try {
      const fileHandle = await window.showSaveFilePicker({
        suggestedName: filename,
        types: [{ description: 'PNG图片', accept: { 'image/png': ['.png'] } }],
      });

      const writable = await fileHandle.createWritable();
      await writable.write(blob);
      await writable.close();

      ElMessage.success('已保存');
    } catch (error) {
      ElMessage.error(`保存失败${error}`);
    }
  };

  const saveMp3Click = async () => {
    const base64String = editFormData.value.mp3;
    const filename = `${editFormData.value.name}.mp3`;

    // 去掉前缀 data:audio/mp3;base64,
    const pureBase64 = base64String.replace(/^data:audio\/(mp3|mpeg|wav);base64,/, '');

    // 转回原始二进制（无损）
    const byteArray = Uint8Array.from(atob(pureBase64), (c) => c.charCodeAt(0));

    // 创建 Blob（二进制保持完全一致）
    const blob = new Blob([byteArray], { type: 'audio/mp3' });

    // 用于降级下载
    const downloadWithLink = () => {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.click();
      URL.revokeObjectURL(url);
    };

    try {
      if ('showSaveFilePicker' in window) {
        const fileHandle = await window.showSaveFilePicker({
          suggestedName: filename,
          types: [
            {
              description: 'MP3 音频文件',
              accept: { 'audio/mp3': ['.mp3'], 'audio/mpeg': ['.mp3'] },
            },
          ],
        });

        const writable = await fileHandle.createWritable();
        await writable.write(blob);
        await writable.close();

        ElMessage.success(`MP3 已保存: ${fileHandle.name}`);
      } else {
        downloadWithLink();
      }
    } catch (error) {
      ElMessage.error(`保存失败，使用下载方式 ${error}`);
      downloadWithLink();
    }
  };

  const convertToBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();

      reader.onload = () => {
        const arrayBuffer = reader.result as ArrayBuffer;
        const bytes = new Uint8Array(arrayBuffer);
        let binary = '';

        for (let i = 0; i < bytes.byteLength; i++) {
          binary += String.fromCharCode(bytes[i]);
        }

        const base64 = btoa(binary);
        resolve(`data:image/png;base64,${base64}`);
      };

      reader.onerror = reject;
      reader.readAsArrayBuffer(file);
    });
  };

  const saveChangeClick = async () => {
    await updateValue(editFormData.value);
    renameNodeById(treeData.value, editFormData.value.id, editFormData.value.name);
    ElMessage.success({
      message: '修改成功',
      duration: 1000,
    });
  };

  const getAllCanvasClick = async () => {
    const { data } = await getAllCanvas(editFormData.value.id);
    allCanvas.value = data;
    ElMessage.success({ message: `加载完毕, 共发现 ${data.length} 张图片` });
  };

  const copyCanvasPathClick = async (path: string) => {
    const text = `${findNodePath(treeData.value, editFormData.value.id)}/${path}`;
    if (text) {
      await navigator.clipboard.writeText(text);
      ElMessage.success({ message: `已复制 ${text}` });
    } else {
      ElMessage.error({ message: '复制失败' });
    }
  };

  /* 右键菜单 --------------------------------------------------------------------------------------*/
  const contextMenuRow = ref<IWzNode>();
  const menuRef = useTemplateRef('WzContextMenuRef');
  const updateKeyId = ref<number>(-1);
  const wzKeyRef = useTemplateRef('WzKeyRef');
  const newNodeFormRef = useTemplateRef('NewNodeFormRef');

  const contextMenu = (event: Event, data: IWzNode) => {
    if (!checkedKey.value.includes(data.id)) {
      checkedKey.value = [data.id];
      lastChecked.value = data;
    }
    event.preventDefault(); // 阻止默认右键菜单
    contextMenuRow.value = data;
    const menuName = data.name;
    const menuItem = genMenuItems(data);
    menuRef.value.openMenu(menuName, menuItem, event.clientX, event.clientY);
  };

  const genMenuItems = (data: IWzNode) => {
    switch (data.type) {
      case 'FOLDER':
        return genFolderMenuItems();
      case 'WZ':
        return genWzMenuItems();
      case 'IMAGE':
        return data.file ? genImgFileMenuItems : genImgMenuItems();
      case 'WZ_DIRECTORY':
        return genWzDirMenuItems();
      case 'IMAGE_LIST':
      case 'IMAGE_CANVAS':
      case 'IMAGE_CONVEX':
        return genListMenuItems();
      default:
        return genDefaultMenuItems();
    }
  };

  const genFolderMenuItems = () => {
    return [
      { name: '复制路径', command: 'copyPath' },
      { name: '打包', command: 'packet' },
      { name: '保存', command: 'save' },
      { name: '修改密钥', command: 'updateKey' },
      { name: '关闭', command: 'unload' },
      { name: '全部关闭', command: 'unloadAll' },
      genMoveViewsItem(),
      {
        name: '导出',
        command: undefined,
        children: [
          { name: '导出 Img', command: 'exportImg' },
          { name: '导出 XML', command: 'exportXml' },
          { name: '导出 XML (紧凑)', command: 'exportXmlMini' },
        ],
        divided: true,
      },
    ].filter(Boolean); // 会把 null 和 undefined 自动过滤掉
  };

  const genWzMenuItems = () => {
    return [
      { name: '复制路径', command: 'copyPath' },
      { name: '子节点', command: 'insert' },
      { name: '粘贴', command: 'paste' },
      { name: '保存', command: 'save' },
      { name: '修改密钥', command: 'updateKey' },
      { name: '关闭', command: 'unload' },
      { name: '全部关闭', command: 'unloadAll' },
      genMoveViewsItem(),
      {
        name: '导出',
        command: undefined,
        children: [
          { name: '导出 Img', command: 'exportImg' },
          { name: '导出 XML', command: 'exportXml' },
          { name: '导出 XML (紧凑)', command: 'exportXmlMini' },
        ],
        divided: true,
      },
      { name: 'outlink', command: 'outlink', divided: true },
      { name: '设为汉化用', command: 'setCMS' },
      { name: '汉化 WZ', command: 'chinese' },
    ].filter(Boolean); // 会把 null 和 undefined 自动过滤掉
  };

  const genImgMenuItems = () => {
    return [
      { name: '复制路径', command: 'copyPath' },
      { name: '子节点', command: 'insert' },
      { name: '粘贴', command: 'paste' },
      genMoveViewsItem(),
      {
        name: '导出',
        command: undefined,
        children: [
          { name: '导出 XML', command: 'exportXml' },
          { name: '导出 XML (紧凑)', command: 'exportXmlMini' },
        ],
        divided: true,
      },
      { name: 'outlink', command: 'outlink', divided: true },
    ].filter(Boolean); // 会把 null 和 undefined 自动过滤掉
  };

  const genImgFileMenuItems = () => {
    return [
      { name: '复制路径', command: 'copyPath' },
      { name: '子节点', command: 'insert' },
      { name: '粘贴', command: 'paste' },
      { name: '保存', command: 'save' },
      { name: '修改密钥', command: 'updateKey' },
      { name: '关闭', command: 'unload' },
      { name: '全部关闭', command: 'unloadAll' },
      genMoveViewsItem(),
      {
        name: '导出',
        command: undefined,
        children: [
          { name: '导出 XML', command: 'exportXml' },
          { name: '导出 XML (紧凑)', command: 'exportXmlMini' },
        ],
        divided: true,
      },
    ].filter(Boolean); // 会把 null 和 undefined 自动过滤掉
  };

  const genWzDirMenuItems = () => {
    return [
      { name: '复制路径', command: 'copyPath' },
      { name: '子节点', command: 'insert' },
      { name: '复制', command: 'copy' },
      { name: '粘贴', command: 'paste' },
      { name: '删除', command: 'delete' },
    ];
  };

  const genListMenuItems = () => {
    return [
      { name: '复制路径', command: 'copyPath' },
      { name: '子节点', command: 'insert' },
      { name: '复制', command: 'copy' },
      { name: '粘贴', command: 'paste' },
      { name: '删除', command: 'delete' },
      { name: 'outlink', command: 'outlink', divided: true },
    ];
  };

  const genDefaultMenuItems = () => {
    return [
      { name: '复制路径', command: 'copyPath' },
      { name: '复制', command: 'copy' },
      { name: '删除', command: 'delete' },
    ];
  };

  const genMoveViewsItem = () => {
    const moveChildren = [];
    let views;
    pFunc('getViewsId', viewId.value, (response) => {
      views = response;
    });
    views.forEach((view) => {
      moveChildren.push({ name: `移动到视图 ${view}`, command: `moveView${view}` });
    });
    if (moveChildren.length > 0) {
      return {
        name: '视图',
        command: undefined,
        children: moveChildren,
        divided: true,
      };
    }

    return null;
  };

  const handleContextMenu = (action: string) => {
    if (action.startsWith('moveView')) {
      const view = action.replace(/moveView(\d+)/, '$1');
      moveViewClick(parseInt(view));
      return;
    }
    switch (action) {
      case 'copyPath':
        copyPathClick();
        break;
      case 'save':
        saveClick();
        break;
      case 'updateKey':
        updateKeyClick();
        break;
      case 'unload':
        unloadClick();
        break;
      case 'unloadAll':
        unloadAllClick();
        break;
      case 'exportImg':
        exportWzFileToImgClick();
        break;
      case 'exportXml':
        exportWzFileToXmlClick(true);
        break;
      case 'exportXmlMini':
        exportWzFileToXmlClick(false);
        break;
      case 'outlink':
        fixOutlink([...checkedKey.value]);
        break;
      case 'setCMS':
        pFunc('setCmsId', contextMenuRow.value.id);
        break;
      case 'chinese':
        pFunc('chineseClick', contextMenuRow.value.id);
        break;
      case 'copy':
        copyClick();
        break;
      case 'paste':
        pasteClick();
        break;
      case 'insert':
        newNodeClick(contextMenuRow.value);
        break;
      case 'delete':
        deleteClick();
        break;
      case 'packet':
        packetClick();
        break;
    }
  };

  const copyPathClick = async () => {
    const text = findNodePath(treeData.value, contextMenuRow.value.id);
    if (text) {
      await navigator.clipboard.writeText(text);
      ElMessage.success({ message: `已复制 ${text}` });
    } else {
      ElMessage.error({ message: '复制失败' });
    }
  };

  const saveClick = async () => {
    await saveNode(contextMenuRow.value.id);
    ElMessage.success({
      message: '保存成功',
    });
  };

  const unloadClick = async () => {
    const id = contextMenuRow.value.id;
    await unload(id);
    removeTreeNode(id);
    treeData.value = [...treeData.value];
    ElMessage.success({
      message: '文件已卸载',
    });
  };

  const unloadAllClick = async () => {
    const id = viewId.value;
    await unload(id);
    removeTreeNode(id);
    treeData.value = [...treeData.value];
    ElMessage.success({
      message: '文件已卸载',
    });
  };

  const updateKeyClick = () => {
    updateKeyId.value = contextMenuRow.value.id;
    wzKeyRef.value.initial();
  };

  const confirmWzKey = async (version: number, key: string) => {
    await updateWzKey(updateKeyId.value, version, key);
    ElMessage.success({ message: '修改成功，请手动保存文件' });
  };

  const exportWzFileToImgClick = async () => {
    const id = contextMenuRow.value.id;
    await exportWzFileToImg(id);
    ElMessage.success({
      message: '导出成功, 文件位于 export 目录下',
    });
  };

  const exportWzFileToXmlClick = async (indent: boolean) => {
    const id = contextMenuRow.value.id;
    await exportWzFileToXml(id, indent);
    ElMessage.success({
      message: '导出成功, 文件位于 export 目录下',
    });
  };

  const copyClick = async () => {
    await copy([...checkedKey.value]);
    ElMessage.success({ message: '复制成功' });
  };

  const pasteClick = async () => {
    const id = contextMenuRow.value.id;
    await paste(id);
    const { data } = await getNode(id);
    const node = getNodeById(treeData.value, id);
    if (node) {
      node.children = data;
      await nextTick(); // 确保数据加载后再刷新树图
      treeData.value = [...treeData.value];
    }
  };

  const deleteClick = async () => {
    const data = contextMenuRow.value;
    confirmDeleteNode(data.name, data.id);
  };

  const confirmDeleteNode = (nodeName: string, id: number) => {
    ElMessageBox.confirm(`你确定要删除 ${nodeName} 节点？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(async () => {
        await deleteNode(id);
        removeNodeById(treeData.value, id);
        treeData.value = [...treeData.value];
        ElMessage({
          type: 'success',
          message: '节点已删除',
        });
      })
      .catch(() => {
        ElMessage({
          type: 'info',
          message: '操作已取消',
        });
      });
  };

  const newNodeClick = (data: IWzNode) => {
    if (!data.type || data.type === 'FOLDER') {
      ElMessage.error({
        message: '该分类不能添加子类',
      });
      return;
    }

    newNodeFormRef.value.initial(data);
  };

  const insertNode = async (pId: number, newData: IWzNode) => {
    const node = getNodeById(treeData.value, pId);
    if (node) {
      if (node.children && node.children.length == 0) {
        const { data } = await getNode(pId);
        node.children = data;
      } else {
        node.children.unshift(newData);
      }
      if (node.children.length > 0) node.leaf = false;

      await nextTick();
      treeData.value = [...treeData.value];
    }
  };

  const moveViewClick = async (view: number) => {
    await moveView(contextMenuRow.value.id, view);
    await initial(viewId.value);
    pFunc('reInitial', view);
  };

  const packetClick = async () => {
    ElMessageBox.prompt('数字版本号如79、83、95等', '文件版本', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '至少一个字符',
    })
      .then(async ({ value }) => {
        await packet(value, contextMenuRow.value.id);
        ElMessage.success({ message: '打包完成请在export/打包wz目录中查看' });
      })
      .catch(() => {
        ElMessage({
          type: 'info',
          message: '操作已取消',
        });
      });
  };

  /* 视图同步 --------------------------------------------------------------------------------------*/
  const expandTreeByPath = async (path, separator = '/') => {
    const treeRef = tree.value;
    if (!treeRef || !path) {
      return false;
    }

    const pathNames = path.split(separator).filter((name) => name.trim());

    if (pathNames.length === 0) {
      return false;
    }

    let dataList = treeData.value;
    for (const p of pathNames) {
      dataList = dataList.find((i) => i.name === p);
      if (!dataList) {
        break;
      } else if (!dataList.leaf) {
        if (dataList.children.length == 0) {
          const { data } = await getNode(dataList.id);
          dataList.children.push(...data);
        }
        if (dataList.children.length == 0) dataList.leaf = true;
        if (dataList.children.length > 0) {
          handleNodeExpand(dataList);
        }

        lastChecked.value = dataList;
        checkedKey.value = [dataList.id];
      } else {
        lastChecked.value = dataList;
        checkedKey.value = [dataList.id];
        break;
      }
      dataList = dataList.children;
    }
    treeData.value = [...treeData.value];
    treeRef.setExpandedKeys(expandedKeys.value);
    await nextTick(() => {
      treeRef.scrollToNode(checkedKey.value[0], 'start');
    });
  };

  const collapseTreeByPath = async (path, separator = '/') => {
    const treeRef = tree.value;
    if (!treeRef || !path) {
      return false;
    }

    const pathNames = path.split(separator).filter((name) => name.trim());

    if (pathNames.length === 0) {
      return false;
    }

    let dataList = treeData.value;
    for (let i = 0; i < pathNames.length; i++) {
      dataList = dataList.find((item) => item.name === pathNames[i]);
      if (!dataList) break;
      if (dataList.leaf) break;
      if (i + 1 == pathNames.length) break;
      dataList = dataList.children;
    }

    if (dataList) {
      handleNodeCollapse(dataList);
      treeData.value = [...treeData.value];
    }
  };

  const loadEditFormByPath = async (path, separator = '/') => {
    const treeRef = tree.value;
    if (!treeRef || !path) {
      return false;
    }

    const pathNames = path.split(separator).filter((name) => name.trim());

    if (pathNames.length === 0) {
      return false;
    }

    let dataList = treeData.value;
    for (let i = 0; i < pathNames.length; i++) {
      dataList = dataList.find((item) => item.name === pathNames[i]);
      if (!dataList) break;
      if (dataList.leaf) break;
      if (i + 1 == pathNames.length) break;
      dataList = dataList.children;
    }
    if (dataList) await loadEditForm(dataList);
  };

  /* 通用方法 --------------------------------------------------------------------------------------*/
  const removeTreeNode = (id: number) => {
    if (id == viewId.value) {
      treeData.value = [];
    } else {
      removeNodeById(treeData.value, id);
    }
  };

  const findNodePath = (treeData: IWzNode[], targetId: number): string | null => {
    // 深度优先搜索函数
    const dfs = (nodes: IWzNode[], currentPath: string[] = []): string | null => {
      for (const node of nodes) {
        // 创建当前节点的路径
        const newPath = [...currentPath, node.name];

        // 如果找到目标节点，返回路径
        if (node.id === targetId) {
          return newPath.join('/');
        }

        // 如果有子节点，递归搜索
        if (node.children && node.children.length > 0) {
          const result = dfs(node.children, newPath);
          if (result) {
            return result;
          }
        }
      }
      return null;
    };

    return dfs(treeData);
  };

  defineExpose({ initial, expandTreeByPath, collapseTreeByPath, loadEditFormByPath });
</script>

<style scoped>
  .file-item {
    display: flex;
    align-items: center;
    gap: 3px;
  }

  .file-icon {
    flex-shrink: 0;
  }

  .custom-tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    padding-right: 8px;
  }

  .custom-tree-node-selected {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    padding-right: 8px;
    background-color: #fff3e0;
    color: #ef6c00;
  }

  :deep(.el-tree-node__expand-icon) {
    position: absolute;
    opacity: 0;
  }

  :deep(.el-tree-node__content) {
    position: relative;
  }

  .saveButtons button {
    margin-left: 0;
    margin-right: 12px;
  }
</style>
<style>
  .hide-expand-icon .el-tree-node__expand-icon {
    display: none;
  }

  .canvas-list {
    display: flex;
    flex-wrap: wrap; /* 允许换行 */
    gap: 12px 16px; /* 行间距12px，列间距16px */
    align-items: center; /* 同一行的所有 canvas-item 上下对齐 */

    .el-card {
      text-align: center;
    }

    .el-card__footer {
      padding: 0;
    }
  }
</style>
