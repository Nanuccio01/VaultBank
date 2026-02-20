import axios from "axios";
import { getToken, clearToken } from "../auth/token";

export const http = axios.create({
    baseURL: "/", // stesso origin (vite proxy in dev, nginx in docker)
    timeout: 15000,
});

http.interceptors.request.use((config) => {
    const token = getToken();
    if (token) {
        config.headers = config.headers ?? {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

http.interceptors.response.use(
    (res) => res,
    (err) => {
        if (err?.response?.status === 401) {
            // token scaduto/non valido
            clearToken();
        }
        return Promise.reject(err);
    }
);