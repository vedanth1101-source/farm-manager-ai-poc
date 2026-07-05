import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api';

export const queryBackend = async (question) => {
  const res = await axios.post(`${BASE_URL}/query`, { question });
  return res.data;
};

export const getTelemetry = async () => {
  const res = await axios.get(`${BASE_URL}/telemetry`);
  return res.data;
};

export const resetTelemetry = async () => {
  const res = await axios.post(`${BASE_URL}/telemetry/reset`);
  return res.data;
};

export const getGoals = async () => {
  const res = await axios.get(`${BASE_URL}/goals`);
  return res.data;
};

export const addGoal = async (goal) => {
  const res = await axios.post(`${BASE_URL}/goals`, goal);
  return res.data;
};

export const getBriefing = async () => {
  const res = await axios.get(`${BASE_URL}/briefing`);
  return res.data;
};

export const regenerateBriefing = async () => {
  const res = await axios.post(`${BASE_URL}/briefing/regenerate`);
  return res.data;
};

// --- CODE GRAPH EXPLORER API ---

export const getGraphNodes = async () => {
  const res = await axios.get(`${BASE_URL}/graph/nodes`);
  return res.data;
};

export const queryGraph = async (q) => {
  const res = await axios.get(`${BASE_URL}/graph/query`, { params: { q } });
  return res.data;
};

export const getGraphAffected = async (node) => {
  const res = await axios.get(`${BASE_URL}/graph/affected`, { params: { node } });
  return res.data;
};

// --- KNOWLEDGE BASE API ---

export const getApprovedNotes = async () => {
  const res = await axios.get(`${BASE_URL}/kb/notes`);
  return res.data;
};

export const getApprovedNoteContent = async (filename) => {
  const res = await axios.get(`${BASE_URL}/kb/notes/${filename}`);
  return res.data;
};

export const deleteApprovedNote = async (filename) => {
  const res = await axios.delete(`${BASE_URL}/kb/notes/${filename}`);
  return res.data;
};

export const getPendingNotes = async () => {
  const res = await axios.get(`${BASE_URL}/kb/pending`);
  return res.data;
};

export const deletePendingNote = async (filename) => {
  const res = await axios.delete(`${BASE_URL}/kb/pending/${filename}`);
  return res.data;
};

export const lintPendingNote = async (filename) => {
  const res = await axios.post(`${BASE_URL}/kb/lint`, { filename });
  return res.data;
};

export const approvePendingNote = async (filename, title, content) => {
  const res = await axios.post(`${BASE_URL}/kb/approve`, { filename, title, content });
  return res.data;
};

export const queryKnowledgeBase = async (query) => {
  const res = await axios.post(`${BASE_URL}/kb/query`, { query });
  return res.data;
};

// --- AGENTIC OS API ---

export const getFarmSoul = async () => {
  const res = await axios.get(`${BASE_URL}/kb/soul`);
  return res.data;
};

export const saveFarmSoul = async (content) => {
  const res = await axios.post(`${BASE_URL}/kb/soul`, { content });
  return res.data;
};

export const getAgentTasks = async () => {
  const res = await axios.get(`${BASE_URL}/agent/tasks`);
  return res.data;
};

export const runAgentTask = async (task, params = {}) => {
  const res = await axios.post(`${BASE_URL}/agent/run`, { task, ...params });
  return res.data;
};

// --- MODEL INTEL API ---

export const getAvailableModels = async () => {
  const res = await axios.get(`${BASE_URL}/models`);
  return res.data;
};

export const getModelRouting = async () => {
  const res = await axios.get(`${BASE_URL}/models/routing`);
  return res.data;
};

export const updateModelRouting = async (task, model) => {
  const res = await axios.post(`${BASE_URL}/models/routing`, { task, model });
  return res.data;
};

export const getModelCosts = async () => {
  const res = await axios.get(`${BASE_URL}/models/costs`);
  return res.data;
};