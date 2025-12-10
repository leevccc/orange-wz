import type { IWzNode } from '@/store/wzEditor.ts';

export function removeNodeById(data: [], id: number) {
  // 遍历根数组
  for (let i = 0; i < data.length; i++) {
    if (data[i].id === id) {
      // 如果找到匹配的节点，从数组中移除
      data.splice(i, 1);
      return true;
    }

    // 如果有子节点，递归查找
    if (data[i].children && data[i].children.length > 0) {
      if (removeNodeById(data[i].children, id)) {
        return true;
      }
    }
  }
  return false;
}

export function renameNodeById(data: [], id: number, name: string) {
  // 遍历根数组
  for (let i = 0; i < data.length; i++) {
    if (data[i].id === id) {
      data[i].name = name;
      return true;
    }

    // 如果有子节点，递归查找
    if (data[i].children && data[i].children.length > 0) {
      if (renameNodeById(data[i].children, id, name)) {
        return true;
      }
    }
  }
  return false;
}

export function getNodeById(data: [], id: number) {
  // 遍历根数组
  for (let i = 0; i < data.length; i++) {
    if (data[i].id === id) {
      return data[i];
    }

    // 如果有子节点，递归查找
    if (data[i].children && data[i].children.length > 0) {
      const result = getNodeById(data[i].children, id);
      if (result) return result;
    }
  }
  return null;
}

export function getBrotherByTwoId(data: [], id1: number, id2: number) {
  // 遍历根数组
  let count = 0;
  for (let i = 0; i < data.length; i++) {
    if (data[i].id == id1) count++;
    else if (data[i].id == id2) count++;

    if (count == 2) return data;

    // 如果有子节点，递归查找
    if (count == 0 && data[i].children && data[i].children.length > 0) {
      const result = getBrotherByTwoId(data[i].children, id1, id2);
      if (result === undefined) return undefined;
      else if (result) return result;
    }
  }

  if (count == 1) return undefined; // 不在同一个
  return null;
}

interface FilterResult {
  filteredTree: IWzNode[];
  matchedIds: number[];
}

export function filterTreeAndCollectIds(nodes: IWzNode[], keyword: string): FilterResult {
  const idSet = new Set<number>();

  // 转成小写用于不区分大小写匹配
  const lower = keyword.toLowerCase();

  function dfs(node: IWzNode, path: number[]): IWzNode | null {
    // 当前 node 的父级路径（不含当前 id）
    const currentPath = [...path];

    let matchedChildren: IWzNode[] = [];
    if (node.children) {
      matchedChildren = node.children
        .map((child) => dfs(child, [...currentPath, node.id]))
        .filter((n): n is IWzNode => n !== null);
    }

    // 🔍 名称匹配（不区分大小写）
    const nameMatched = node.name.toLowerCase().includes(lower);

    if (nameMatched) {
      // 加入父级 ID，不包含当前节点 id
      currentPath.forEach((id) => idSet.add(id));
    }

    if (nameMatched || matchedChildren.length > 0) {
      return {
        ...node,
        children: matchedChildren,
      };
    }

    return null;
  }

  const filteredTree = nodes.map((node) => dfs(node, [])).filter((n): n is IWzNode => n !== null);

  return {
    filteredTree,
    matchedIds: Array.from(idSet),
  };
}
