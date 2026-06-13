import axios from 'axios';

const API_URL = 'http://localhost:8080/api/query';
export const queryBackend = async (question) => {
  try {
    const res = await axios.post(API_URL, { question });
    return res.data;
  } catch (err) {
    throw err;
  }
};