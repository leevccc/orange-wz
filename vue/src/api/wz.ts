import { api } from '@/api/interceptor.ts';
import type { IWzNodeValue } from '@/store/wzEditor.ts';

const apiPath = '/admin/wzEditor';

export function deleteCache() {
  return api.delete(`${apiPath}/cache`);
}

export function gc() {
  return api.delete(`${apiPath}/gc`);
}

export function getViews() {
  return api.get(`${apiPath}/views`);
}

export function addView() {
  return api.post(`${apiPath}/views`);
}

export function moveView(node: number, view: number) {
  return api.post(`${apiPath}/views/${node}/move/${view}`);
}

export function removeView(view: number) {
  return api.delete(`${apiPath}/views/${view}`);
}

export function getFolder(id: number) {
  return api.get(`${apiPath}/folder/${id}`);
}

export function reloadFolder() {
  return api.delete(`${apiPath}/folder`);
}

export function load(id: number, view: number, version: string, key: string) {
  return api.get(`${apiPath}/load/${id}?view=${view}&v=${version}&k=${key}`);
}

export function unload(id: number) {
  return api.delete(`${apiPath}/unload/${id}`);
}

export function getNode(id: number) {
  return api.get(`${apiPath}/node/${id}`);
}

export function getValue(id: number) {
  return api.get(`${apiPath}/node/${id}/value`);
}

export function updateValue(data: IWzNodeValue) {
  return api.post(`${apiPath}/node/${data.id}/value`, data);
}

export function copy(id: number[]) {
  const idString = id.join(',');
  return api.get(`${apiPath}/copy?key=${idString}`);
}

export function paste(id: number) {
  return api.get(`${apiPath}/paste/${id}`);
}

export function addNode(data: IWzNodeValue) {
  return api.post(`${apiPath}/node/${data.id}/sub`, data);
}

export function deleteNode(id: number) {
  return api.delete(`${apiPath}/node/${id}`);
}

export function saveNode(id: number) {
  return api.get(`${apiPath}/node/${id}/save`);
}

export function exportWzFileToImg(id: number) {
  return api.get(`${apiPath}/tools/export/img/${id}`);
}

export function exportWzFileToXml(id: number, indent: boolean) {
  return api.get(`${apiPath}/tools/export/xml/${id}?indent=${indent}`);
}

export function fixOutlink(id: number) {
  return api.get(`${apiPath}/tools/outlink/${id}`);
}

export function updateWzKey(id: number, version: string, key: string) {
  return api.get(`${apiPath}/tools/updateKey/${id}?v=${version}&k=${key}`);
}

export function localization(from: number, to: number) {
  return api.get(`${apiPath}/tools/localization?from=${from}&to=${to}`);
}

export function packet(fileVersion: number, id: number) {
  return api.get(`${apiPath}/tools/packet?fileVersion=${fileVersion}&id=${id}`);
}

export function getAllCanvas(id: number) {
  return api.get(`${apiPath}/tools/canvas?id=${id}`);
}
